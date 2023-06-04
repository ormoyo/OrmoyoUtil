package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;

public abstract class ActionAbility extends AbilityCooldown
{
    public ActionAbility(IAbilityHolder owner)
    {
        super(owner);
    }

    public abstract boolean action();

    public abstract int getCooldown();

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);

        if (keybind != null)
            return;

        if (!this.action())
            return;

        this.setIsOnCooldown(true);
    }
}
