package com.ormoyo.ormoyoutil.abilities;

import net.minecraft.entity.player.EntityPlayer;

public abstract class AbilityToggle extends AbilityKeybindingBase
{
    protected boolean isToggled;
    protected int cooldown;

    public AbilityToggle(EntityPlayer owner)
    {
        super(owner);
    }

    public abstract boolean toggle();

    public abstract boolean untoggle();

    public abstract int getCooldown();

    @Override
    public void onUpdate()
    {
        if (this.owner == null)
        {
            return;
        }

        this.cooldown = Math.max(this.cooldown - 1, 0);
        super.onUpdate();
    }

    @Override
    public void onKeyPress()
    {
        super.onKeyPress();
        if (!this.isToggled)
        {
            if (this.toggle())
            {
                this.isToggled = true;
            }
        }
        else
        {
            if (this.untoggle())
            {
                this.isToggled = false;
                this.cooldown = this.getCooldown();
            }
        }
    }
}
