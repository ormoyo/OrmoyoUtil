package com.ormoyo.ormoyoutil.ability;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.capability.AbilityHolderStorage;
import com.ormoyo.ormoyoutil.event.AbilityEvents;
import com.ormoyo.ormoyoutil.network.MessageSetAbilities;
import com.ormoyo.ormoyoutil.network.MessageUnlockAbility;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class AbilityHolderImpl implements AbilityHolder
{
    protected final Map<Class<? extends Ability>, Ability> abilities = Maps.newHashMap();
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
        {
            if (Objects.equals(ability.getEntry().getRegistryName(), resourceLocation))
            {
                return (T) ability;
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Ability> T getAbility(Class<T> clazz)
    {
        return (T) this.abilities.get(clazz);
    }

    @Override
    public boolean unlockAbility(AbilityEntry entry)
    {
        Ability ability = entry.newInstance(this);

        boolean isUnlocked = this.getAbility(ability.getClass()) == null && this.abilities.put(entry.getAbilityClass(), ability) == null;
        if (isUnlocked)
        {
            if (MinecraftForge.EVENT_BUS.post(new AbilityEvents.OnAbilityUnlockedEvent(ability)))
                return false;

            if (EffectiveSide.get().isServer())
                OrmoyoUtil.NETWORK_CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(this::getPlayer), new MessageUnlockAbility(this, ability.getEntry()));

            ability.onUnlock();
        }

        return isUnlocked;
    }

    @Override
    public void setAbilities(Collection<Ability> abilities)
    {
        String caller = ASMUtils.getCallerClassName();

        if (!AbilityHolderStorage.class.getName().equals(caller) &&
                !AbilityEventHandler.class.getName().equals(caller) &&
                !MessageSetAbilities.class.getName().equals(caller))
            throw new IllegalStateException("This method can only be called by specific classes");


        this.abilities.clear();
        abilities.forEach(ability -> this.abilities.put(ability.getEntry().getAbilityClass(), ability));
    }

    @Override
    public PlayerEntity getPlayer()
    {
        return this.player;
    }
}
