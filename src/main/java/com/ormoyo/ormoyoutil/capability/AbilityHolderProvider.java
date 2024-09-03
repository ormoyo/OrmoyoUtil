package com.ormoyo.ormoyoutil.capability;

import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityHolderImpl;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

public class AbilityHolderProvider implements ICapabilitySerializable<INBT>
{
    private final AbilityHolderImpl instance;
    private final LazyOptional<AbilityHolder> instanceOptional;

    public AbilityHolderProvider(PlayerEntity player)
    {
        this.instance = new AbilityHolderImpl(player);
        this.instanceOptional = LazyOptional.of(() -> this.instance);
    }

    @Nonnull
    @Override
    public <U> LazyOptional<U> getCapability(Capability<U> capability, Direction facing)
    {
        if (capability == Ability.getAbilityCapability())
            return instanceOptional.cast();

        return LazyOptional.empty();
    }

    @Override
    public INBT serializeNBT()
    {
        return Ability.getAbilityCapability().getStorage().writeNBT(Ability.getAbilityCapability(), this.instance, null);
    }

    @Override
    public void deserializeNBT(INBT nbt)
    {
        Ability.getAbilityCapability().getStorage().readNBT(Ability.getAbilityCapability(), this.instance, null, nbt);
    }

    public void invalidate()
    {
        this.instanceOptional.invalidate();
    }
}
