package com.ormoyo.ormoyoutil.capability;

import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.proxy.CommonProxy;
import net.minecraft.util.ResourceLocation;

import java.util.Set;

public interface IAbilityCap
{
    /**
     * This should only be used by {@link CommonProxy} use it instead
     */
    boolean unlockAbility(Ability ability);

    boolean isAbilityUnlocked(Ability ability);

    boolean isAbilityUnlocked(ResourceLocation name);

    boolean isAbilityUnlocked(Class<? extends Ability> clazz);

    Ability getAbility(ResourceLocation name);

    <T extends Ability> T getAbility(Class<T> clazz);

    Set<Ability> getAbilities();
}
