package com.ormoyo.ormoyoutil.capability;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.util.AbilityMessage;
import com.ormoyo.ormoyoutil.event.AbilityEvents;
import com.ormoyo.ormoyoutil.network.MessageSetAbilities;
import com.ormoyo.ormoyoutil.network.MessageUnlockAbility;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.network.PacketDistributor;

import java.lang.reflect.Field;
import java.util.*;

public class AbilityHolderImpl implements AbilityHolder
{
    private static final Class<?> ABILITY_EVENT_HANDLER_CLASS;

    protected final Map<Class<? extends Ability>, Ability> abilities = Maps.newHashMap();
    protected final Map<Class<? extends Ability>, Collection<AbilityMessage>> queuedMessages = Maps.newHashMap();

    protected final PlayerEntity player;

    public AbilityHolderImpl()
    {
        this(null);
    }

    public AbilityHolderImpl(PlayerEntity player)
    {
        this.player = player;
    }

    @Override
    public Collection<Ability> getAbilities()
    {
        return this.abilities.values();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Ability> T getAbility(ResourceLocation resourceLocation)
    {
        for (Ability ability : this.abilities.values())
            if (Objects.equals(ability.getEntry().getRegistryName(), resourceLocation))
                return (T) ability;

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Ability> T getAbility(Class<T> clazz)
    {
        return (T) this.abilities.get(clazz);
    }

    @Override
    public boolean unlockAbility(AbilityEntry<?> entry)
    {
        Ability ability = entry.newInstance(this);

        boolean isUnlocked = this.unlockAbilityInternal(ability);
        if (isUnlocked)
        {
            if (MinecraftForge.EVENT_BUS.post(new AbilityEvents.AbilityUnlockedEvent(ability)))
            {
                this.abilities.remove(ability.getClass());
                return false;
            }

            if (EffectiveSide.get().isServer())
                OrmoyoUtil.NETWORK_CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(this::asPlayer), new MessageUnlockAbility(this, ability.getEntry()));

            ability.onUnlock();
        }

        return isUnlocked;
    }

    private boolean unlockAbilityInternal(Ability ability)
    {
        return this.getAbility(ability.getClass()) == null && this.abilities.put(ability.getClass(), ability) == null;
    }

    @Override
    public void setAbilities(Collection<Ability> abilities)
    {
        String caller = ASMUtils.getCallerClassName();

        if (caller != null &&
                !ABILITY_EVENT_HANDLER_CLASS.getName().equals(caller) &&
                !AbilityHolderStorage.class.getName().equals(caller) &&
                !MessageSetAbilities.class.getName().equals(caller))
            throw new IllegalStateException("This method can only be called by specific classes");


        this.abilities.clear();
        abilities.forEach(this::unlockAbilityInternal);
    }

    @Override
    public void queueMessageFor(Class<? extends Ability> ability, AbilityMessage message)
    {
        AbilityEvents.MessageQueuedEvent event = new AbilityEvents.MessageQueuedEvent(ability, message);
        if (MinecraftForge.EVENT_BUS.post(event))
            return;

        ability = event.getAbilityClass();
        message = event.getSenderEntry();

        Ability a = this.abilities.get(ability);
        if (a == null)
        {
            Collection<AbilityMessage> messages = this.queuedMessages.computeIfAbsent(ability, k -> new ArrayList<>(3));
            messages.add(message);

            return;
        }

        a.onMessageFromAbility(message.getSender(), message);
    }

    @Override
    public void handleMessagesFor(Class<? extends Ability> ability)
    {
        Collection<AbilityMessage> messages = this.queuedMessages.get(ability);
        if (messages == null)
            return;

        Ability a = this.abilities.get(ability);
        if (a == null)
            return;

        for (AbilityMessage message : messages)
            a.onMessageFromAbility(message.getSender(), message);
    }

    @Override
    public PlayerEntity asPlayer()
    {
        return this.player;
    }

    static
    {
        Class<?> clazz;

        try
        {
            Field field = Ability.class.getDeclaredField("ABILITY_EVENT_HANDLER_CLASS");
            field.setAccessible(true);
            clazz = (Class<?>) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }

        ABILITY_EVENT_HANDLER_CLASS = clazz;
    }
}
