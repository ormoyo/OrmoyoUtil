package com.ormoyo.ormoyoutil.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

/**
 * A simple implementation of {@link ICapabilitySerializable} that supports a single {@link Capability} handler instance.
 * <p>
 * Uses the {@link Capability}'s {@link IStorage} to serialise/deserialise NBT.
 *
 * @author Choonster
 */
public class AbilityCapProvider<HANDLER> implements ICapabilitySerializable<NBTBase>
{
    /**
     * The {@link Capability} instance to provide the handler for.
     */
    private final Capability<HANDLER> capability;

    /**
     * The {@link EnumFacing} to provide the handler for.
     */
    private final EnumFacing facing;

    /**
     * The handler instance to provide.
     */
    private final HANDLER instance;

    /**
     * Create a provider for the default handler instance.
     *
     * @param capability The Capability instance to provide the handler for
     * @param facing     The EnumFacing to provide the handler for
     */
    @SuppressWarnings("unchecked")
    public AbilityCapProvider(Capability<HANDLER> capability, @Nullable EnumFacing facing, EntityPlayer player)
    {
        this(capability, facing, (HANDLER) new AbilityCap(player), player);
    }

    /**
     * Create a provider for the specified handler instance.
     *
     * @param capability The Capability instance to provide the handler for
     * @param facing     The EnumFacing to provide the handler for
     * @param instance   The handler instance to provide
     */
    public AbilityCapProvider(Capability<HANDLER> capability, @Nullable EnumFacing facing, HANDLER instance, EntityPlayer player)
    {
        this.capability = capability;
        this.instance = instance;
        this.facing = facing;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        return capability == CapabilityHandler.CAPABILITY_ABILITY_DATA;
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        Capability<IAbilityCap> cap = CapabilityHandler.CAPABILITY_ABILITY_DATA;
        return capability == cap ? cap.cast((IAbilityCap) this.instance) : null;
    }


    @Override
    public NBTBase serializeNBT()
    {
        return this.getCapability().getStorage().writeNBT(this.capability, getInstance(), getFacing());
    }

    @Override
    public void deserializeNBT(NBTBase nbt)
    {
        this.getCapability().getStorage().readNBT(this.capability, getInstance(), getFacing(), nbt);
    }

    /**
     * Get the {@link Capability} instance to provide the handler for.
     *
     * @return The Capability instance
     */
    public final Capability<HANDLER> getCapability()
    {
        return this.capability;
    }

    /**
     * Get the {@link EnumFacing} to provide the handler for.
     *
     * @return The EnumFacing to provide the handler for
     */
    @Nullable
    public EnumFacing getFacing()
    {
        return this.facing;
    }

    /**
     * Get the handler instance.
     *
     * @return The handler instance
     */
    public final HANDLER getInstance()
    {
        return this.instance;
    }
}
