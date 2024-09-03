package com.ormoyo.ormoyoutil.ability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;

public interface AbilityHolder
{
    Collection<Ability> getAbilities();

    <T extends Ability> T getAbility(ResourceLocation resourceLocation);

    <T extends Ability> T getAbility(Class<T> clazz);

    boolean unlockAbility(AbilityEntry entry);

    void setAbilities(Collection<Ability> abilities);

    PlayerEntity getPlayer();
}
