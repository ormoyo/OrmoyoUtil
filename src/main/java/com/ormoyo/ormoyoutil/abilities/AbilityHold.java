package com.ormoyo.ormoyoutil.abilities;

import net.minecraft.entity.player.EntityPlayer;

public abstract class AbilityHold extends AbilityKeybindingBase
{
    protected boolean isHolding = false;
    protected int cooldown;

    public AbilityHold(EntityPlayer owner)
    {
        super(owner);
    }

    public abstract boolean hold();

    public abstract int getCooldown();

    @Override
    public void onUpdate()
    {
        if (this.owner == null)
        {
            return;
        }

        if (this.isHolding && this.cooldown <= 0)
        {
            this.hold();
        }

        this.cooldown = Math.max(this.cooldown - 1, 0);
        super.onUpdate();
    }


    @Override
    public void onKeyPress()
    {
        super.onKeyPress();
        if (!this.isHolding)
        {
            this.isHolding = true;
        }
    }

    @Override
    public void onKeyRelease()
    {
        super.onKeyRelease();
        if (this.isHolding)
        {
            this.isHolding = false;
            if (this.cooldown <= 0)
            {
                this.cooldown = this.getCooldown();
            }
        }
    }
}
