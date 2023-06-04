package com.ormoyo.ormoyoutil.ability;

public abstract class AbilityCooldown extends AbilityKeybindingBase
{
    protected int cooldownTick;
    protected boolean isOnCooldown;

    public AbilityCooldown(IAbilityHolder owner)
    {
        super(owner);
    }

    @Override
    public void onUpdate()
    {
        if (!isOnCooldown)
        {
            super.onUpdate();
            return;
        }

        this.cooldownTick++;
        this.cooldownTick %= this.getCooldown();

        if (this.cooldownTick == 0)
        {
            this.isOnCooldown = false;
        }
    }

    public void setIsOnCooldown(boolean isOnCooldown)
    {
        if (!isOnCooldown)
            this.cooldownTick = 0;

        this.isOnCooldown = isOnCooldown;
    }

    public boolean isOnCooldown()
    {
        return this.isOnCooldown;
    }

    public abstract int getCooldown();
}
