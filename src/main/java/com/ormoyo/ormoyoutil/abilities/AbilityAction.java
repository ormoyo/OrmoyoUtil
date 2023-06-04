package com.ormoyo.ormoyoutil.abilities;

import net.minecraft.entity.player.EntityPlayer;

public abstract class AbilityAction extends AbilityKeybindingBase
{
    protected int cooldown;

    public AbilityAction(EntityPlayer owner)
    {
        super(owner);
    }

    public abstract boolean action();

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
        if (this.cooldown <= 0)
        {
            if (this.action())
            {
                this.cooldown = this.getCooldown();
            }
        }
    }
}
