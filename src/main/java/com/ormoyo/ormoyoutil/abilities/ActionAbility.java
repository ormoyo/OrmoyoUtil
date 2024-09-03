package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import com.ormoyo.ormoyoutil.ability.AbilityCooldown;

public abstract class ActionAbility extends AbilityCooldown
{
    public ActionAbility(AbilityHolder owner)
    {
        super(owner);
    }

    /**
     * Called when the ability is activated (Usually by a press).
     * @param keybind The specific keybinding being pressed
     * @return If the ability has succeeded:<br><strong>success</strong> - the cooldown starts.<br><strong>failure</strong> - the cooldown stays off.
     */
    public abstract boolean action(String keybind);

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);
        this.setIsOnCooldown(keybind, this.action(keybind));
    }
}
