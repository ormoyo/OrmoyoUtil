package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;

public abstract class ActionAbility extends AbilityCooldown
{
    public ActionAbility(IAbilityHolder owner)
    {
        super(owner);
    }

    public abstract boolean action(String keybind);

    public abstract int getCooldown();

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);
        this.setIsOnCooldown(keybind, this.action(keybind));
    }
}
