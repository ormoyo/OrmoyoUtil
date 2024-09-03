package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class ToggleAbility extends AbilityCooldown
{
    private final Map<String, MutableBoolean> isToggled = new NonNullMap<>(this.getKeybinds().length, MutableBoolean::new, true);

    public ToggleAbility(AbilityHolder owner)
    {
        super(owner);
    }

    /**
     * Called when the ability is toggled (Usually by a press).
     * @param keybind The specific keybinding being pressed
     * @param toggled The current toggle state
     * @return If the toggle has succeeded:<br><strong>success</strong> - the cooldown starts and the toggled state is flipped.<br><strong>failure</strong> - the cooldown stays off and the toggled state stays as it is.
     */
    public abstract boolean toggle(String keybind, boolean toggled);

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);

        MutableBoolean isToggled = this.isToggled.get(keybind);
        if (this.toggle(keybind, isToggled.booleanValue()))
        {
            isToggled.setValue(!isToggled.booleanValue());
            if (!isToggled.booleanValue())
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
