package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class HoldAbility extends AbilityCooldown
{
    private final Map<String, MutableBoolean> isHolding = new NonNullMap<>(this.getKeybinds().length, MutableBoolean::new, true);

    public HoldAbility(AbilityHolder owner)
    {
        super(owner);
    }

    /**
     * Called when the ability is activated (Usually by a press).
     * @param keybind The specific keybinding being pressed (nykk
     * @return If the ability has succeeded:<br><strong>success</strong> - the method would be kept called every tick until release, by then the cooldown will start.<br><strong>failure</strong> - the cooldown stays off.
     * @implNote <strong>warning</strong> - the returned value will only be evaluated when the player initially presses the keybinding (not on hold)
     */
    public abstract boolean hold(String keybind);

    @Override
    public void tick()
    {
        super.tick();
        for (String keybind : this.isHolding.keySet())
            if (this.isHolding.get(keybind).booleanValue() && !this.isOnCooldown())
                this.hold(keybind);
    }

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);

        MutableBoolean isHolding = this.isHolding.get(keybind);
        isHolding.setValue(!isHolding.booleanValue() && this.hold(keybind));
    }

    @Override
    public void onKeyRelease(String keybind)
    {
        super.onKeyRelease(keybind);

        MutableBoolean isHolding = this.isHolding.get(keybind);
        if (!isHolding.booleanValue())
            return;

        isHolding.setFalse();
        this.setIsOnCooldown(keybind, true);
    }

    protected void setIsHolding(String keybind, boolean isHolding)
    {
        this.isHolding.get(keybind).setValue(isHolding);
    }

    @SuppressWarnings("SameParameterValue")
    protected boolean isHolding(String keybind)
    {
        return this.isHolding.get(keybind).booleanValue();
    }

    protected boolean isHolding()
    {
        return this.isHolding(null);
    }
}
