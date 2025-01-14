package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.ability.event.AbilityEventEntry;
import com.ormoyo.ormoyoutil.ability.util.AbilityMessage;
import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import com.ormoyo.ormoyoutil.network.datasync.AbilityDataParameter;
import com.ormoyo.ormoyoutil.network.datasync.AbilitySyncManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class Ability
{
    @SuppressWarnings("unused")
    private static final Class<?> ABILITY_EVENT_HANDLER_CLASS = AbilityEventHandler.class;

    public Ability(AbilityHolder owner)
    {
        this.owner = owner != null ? owner.asPlayer() : null;
        this.entry = Ability.getAbilityClassEntry(this.getClass());

        this.syncManager = new AbilitySyncManager(this);
        this.abilityInit();
    }

    private static final AbilityDataParameter<Boolean> IS_ENABLED = AbilitySyncManager.createKey(Ability.class, DataSerializers.BOOLEAN);

    private final AbilityEntry<?> entry;
    protected final AbilitySyncManager syncManager;

    protected final PlayerEntity owner;
    private int messagesCheckingTicks;

    /*
     * Called every tick
     */
    public void tick()
    {
        if (this.messagesCheckingTicks > 0)
        {
            this.messagesCheckingTicks--;
            return;
        }

        this.messagesCheckingTicks = this.getMessagesCheckingTime();
        this.getHolder().handleMessagesFor(this.getEntry().getAbilityClass());
    }

    /**
     * Called for registering data parameters
     */
    public void abilityInit()
    {
        this.syncManager.register(IS_ENABLED, true);
    }

    /**
     * Called when a player unlocks this ability
     */
    public void onUnlock()
    {
    }

    public void writeToNBT(CompoundNBT compound)
    {
    }

    public void readFromNBT(CompoundNBT compound)
    {
    }

    public void setIsEnabled(boolean isEnabled)
    {
        this.syncManager.set(IS_ENABLED, isEnabled);
    }

    /**
     * If the ability is disabled all it's methods wouldn't get called
     */
    public boolean isEnabled()
    {
        return this.syncManager.get(IS_ENABLED);
    }

    public void onAbilityEnabled()
    {
    }

    public void onAbilityDisabled()
    {
    }

    public void notifySyncManagerChange(AbilityDataParameter<?> parameter)
    {
        if (parameter == Ability.IS_ENABLED)
        {
            if (this.isEnabled())
                this.onAbilityEnabled();
            else
                this.onAbilityDisabled();
        }
    }

    public boolean isServerAbility()
    {
        return AbilityEventHandler.SERVER_ABILITIES.contains(this.getEntry().getAbilityClass());
    }

    public boolean isClientAbility()
    {
        return AbilityEventHandler.CLIENT_ABILITIES.contains(this.getEntry().getAbilityClass());
    }

    public PlayerEntity getOwner()
    {
        return this.owner;
    }

    public ITextComponent getName()
    {
        return this.getEntry().getName();
    }

    public final ResourceLocation getRegistryName()
    {
        return this.getEntry().getRegistryName();
    }

    public final AbilityEntry<?> getEntry()
    {
        return this.entry;
    }

    public AbilitySyncManager getSyncManager()
    {
        return this.syncManager;
    }

    public int getMessagesCheckingTime()
    {
        return 20;
    }

    /**
     * Sends a message to an ability the owner has (If they don't the message will be queued until the ability is unlocked). </p>
     * This is the recommended way for inter ability communications (and for handling abilities that may not have been unlocked yet). </br></br>
     * If you need something to do with another ability right now then you can access it from the {@link AbilityHolder} and check if it's not null
     * @param ability The class of the ability to send the message to
     * @param message The message
     */
    protected final void sendMessageToAbility(Class<? extends Ability> ability, AbilityMessage message)
    {
        message = new AbilityMessage(this, message.getKey(), message.getValue());
        this.getHolder().queueMessageFor(ability, message);
    }

    public void onMessageFromAbility(AbilityEntry<?> sender, AbilityMessage message)
    {
    }

    @Override
    public String toString()
    {
        return this.getRegistryName().toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj instanceof Ability)
        {
            Ability ability = (Ability) obj;
            return this.entry.equals(ability.entry);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(entry, owner);
    }

    @SuppressWarnings("unchecked")
    public static<T extends Ability> AbilityEntry<T> getAbilityClassEntry(Class<T> clazz)
    {
        for (AbilityEntry<?> entry : Ability.getAbilityRegistry().getValues())
            if (entry.getAbilityClass() == clazz)
                return (AbilityEntry<T>) entry;

        return null;
    }

    public static IForgeRegistry<AbilityEntry<?>> getAbilityRegistry()
    {
        return AbilityEventHandler.ABILITY_REGISTRY;
    }

    public static IForgeRegistry<AbilityEventEntry> getAbilityEventRegistry()
    {
        return AbilityEventHandler.ABILITY_EVENT_REGISTRY;
    }

    public static<T extends Ability> ITextComponent getAbilityDisplayName(Class<T> clazz)
    {
        AbilityEntry<T> entry = Ability.getAbilityClassEntry(clazz);
        return entry != null ? entry.getName() : null;
    }

    public static Capability<AbilityHolder> getAbilityCapability()
    {
        return AbilityEventHandler.ABILITY_HOLDER_CAPABILITY;
    }

    @Nullable
    public static AbilityHolder getAbilityHolder(PlayerEntity player)
    {
        if (player == null)
            return null;

        return player.getCapability(Ability.getAbilityCapability()).resolve().orElse(null);
    }
}