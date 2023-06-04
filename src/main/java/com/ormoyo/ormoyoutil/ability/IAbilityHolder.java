package com.ormoyo.ormoyoutil.ability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;

public interface IAbilityHolder
{
    Collection<Ability> getAbilities();

    Ability getAbility(ResourceLocation resourceLocation);

    Ability getAbility(Class<? extends Ability> clazz);

    boolean unlockAbility(AbilityEntry entry);

    default PlayerEntity getPlayer()
    {
        return (PlayerEntity) this;
    }
}
