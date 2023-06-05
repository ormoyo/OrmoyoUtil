package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class ToggleAbility extends AbilityCooldown
{
    private final Map<String, MutableBoolean> isToggled = new NonNullMap<>(this.getKeybinds().length, new MutableBoolean(), true);

    public ToggleAbility(IAbilityHolder owner)
    {
        super(owner);
    }

    public abstract boolean toggle(String keybind);

    public abstract boolean untoggle(String keybind);

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);

        MutableBoolean isToggled = this.isToggled.get(keybind);
        if (!isToggled.booleanValue())
        {
            isToggled.setValue(this.toggle(keybind));
            return;
        }

        if (this.untoggle(keybind))
        {
            isToggled.setFalse();
            this.setIsOnCooldown(keybind, true);
        }
    }

    protected void setIsToggled(String keybind, boolean isHolding)
    {
        this.isToggled.get(keybind).setValue(isHolding);
    }

    protected boolean isToggled(String keybind)
    {
        return this.isToggled.get(keybind).booleanValue();
    }

    protected boolean isToggled()
    {
        return this.isToggled(null);
    }
}
