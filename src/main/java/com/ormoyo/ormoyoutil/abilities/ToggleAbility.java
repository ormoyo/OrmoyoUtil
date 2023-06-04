package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;

public abstract class ToggleAbility extends AbilityCooldown
{
    protected boolean isToggled;

    public ToggleAbility(IAbilityHolder owner)
    {
        super(owner);
    }

    public abstract boolean toggle();

    public abstract boolean untoggle();

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);

        if (keybind != null)
            return;

        if (!this.isToggled)
        {
            if (this.toggle())
                this.isToggled = true;

            return;
        }

        if (this.untoggle())
        {
            this.isToggled = false;
            this.setIsOnCooldown(true);
        }
    }
}
