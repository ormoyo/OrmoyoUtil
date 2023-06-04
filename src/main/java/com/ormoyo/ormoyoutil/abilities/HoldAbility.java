package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;

public abstract class HoldAbility extends AbilityCooldown
{
    protected boolean isHolding = false;

    public HoldAbility(IAbilityHolder owner)
    {
        super(owner);
    }

    public abstract boolean hold();

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (this.isHolding && !this.isOnCooldown())
        {
            this.hold();
        }
    }

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);
        if (!this.isHolding)
        {
            if (this.hold())
            {
                this.isHolding = true;
            }
        }
    }

    @Override
    public void onKeyRelease(String keybind)
    {
        super.onKeyRelease(keybind);

        if (keybind != null)
            return;

        if (!this.isHolding)
            return;

        this.isHolding = false;
        this.setIsOnCooldown(true);
    }
}
