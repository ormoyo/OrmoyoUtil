package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.ability.event.AbilityEventEntry;
import com.ormoyo.ormoyoutil.ability.util.AbilityMessage;
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
    public Ability(AbilityHolder owner)
    {
        this.owner = owner != null ? owner.asPlayer() : null;
        this.entry = Ability.getAbilityClassEntry(this.getClass());

        this.syncManager = new AbilitySyncManager(this);
        AbilityEventHandler.ABILITY_DISPLAY_NAMES.put(this.getEntry(), this.getTranslatedName());

        this.abilityInit();
    }

    private static final AbilityDataParameter<Boolean> IS_ENABLED = AbilitySyncManager.createKey(Ability.class, DataSerializers.BOOLEAN);

    private final AbilityEntry entry;
    protected final AbilitySyncManager syncManager;

    protected final PlayerEntity owner;

    /**
     * Called every tick
     */
    public void tick()
    {
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

    public String getName()
    {
        String registryName = String.valueOf(this.entry);
        return StringUtils.capitalize(registryName.substring(registryName.lastIndexOf(':')));
    }

    public ITextComponent getTranslatedName()
    {
        return new TranslationTextComponent("ability." + this.getRegistryName().getNamespace() + "." + this.getRegistryName().getPath() + ".name");
    }

    public final ResourceLocation getRegistryName()
    {
        return this.entry.getRegistryName();
    }

    public final AbilityEntry getEntry()
    {
        return this.entry;
    }

    public AbilitySyncManager getSyncManager()
    {
        return this.syncManager;
    }

    protected final void sendMessageToAbility(Ability ability, AbilityMessage message)
    {
        ability.getMessageFromAbility(this, message);
    }

    protected void getMessageFromAbility(Ability sender, AbilityMessage message)
    {
    }

    @Override
    public String toString()
    {
        return this.getName();
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

    public static AbilityEntry getAbilityClassEntry(Class<? extends Ability> clazz)
    {
        for (AbilityEntry entry : Ability.getAbilityRegistry().getValues())
            if (entry.getAbilityClass() == clazz)
                return entry;

        return null;
    }

    public static IForgeRegistry<AbilityEntry> getAbilityRegistry()
    {
        return AbilityEventHandler.ABILITY_REGISTRY;
    }

    public static IForgeRegistry<AbilityEventEntry> getAbilityEventRegistry()
    {
        return AbilityEventHandler.ABILITY_EVENT_REGISTRY;
    }

    public static ITextComponent getAbilityDisplayName(Class<? extends Ability> clazz)
    {
        return AbilityEventHandler.ABILITY_DISPLAY_NAMES.get(clazz);
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