package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class ToggleAbility extends AbilityCooldown
{
    private final Map<String, MutableBoolean> isToggled = new NonNullMap<>(this.getKeyBindings().length, MutableBoolean::new, true);

    public ToggleAbility(AbilityHolder owner)
    {
        super(owner);
    }

    /**
     * Called when the ability is toggled (Usually by a press).
     * @param keybind The specific keybinding being pressed
     * @return If the toggle has succeeded:<br><strong>success</strong> - the toggled state is flipped.<br><strong>failure</strong> - the cooldown stays off and the toggled state stays as it is.
     */
    public abstract boolean toggle(String keybind);
    /**
     * Called when the ability is untoggled (After being toggled).
     * @param keybind The specific keybinding being pressed
     * @return If the toggle has succeeded:<br><strong>success</strong> - the cooldown starts and the toggled state is flipped.<br><strong>failure</strong> - the cooldown stays off and the toggled state stays as it is.
     */
    public abstract boolean untoggle(String keybind);

    @Override
    public void onKeyPress(String keybind)
    {
        super.onKeyPress(keybind);

        MutableBoolean isToggled = this.isToggled.get(keybind);
        boolean toggled = isToggled.booleanValue();
        if ((toggled && this.untoggle(keybind)) || (!toggled && this.toggle(keybind)))
            this.flipToggleState(keybind, isToggled);
    }

    protected void setToggled(String keybind, boolean toggled)
    {
        this.isToggled.get(keybind).setValue(toggled);
    }

    protected boolean isToggled(String keybind)
    {
        return this.isToggled.get(keybind).booleanValue();
    }

    protected boolean isToggled()
    {
        return this.isToggled(null);
    }

    private void flipToggleState(String keybind, MutableBoolean isToggled)
    {
        isToggled.setValue(!isToggled.booleanValue());
        if (!isToggled.booleanValue())
            this.setIsOnCooldown(keybind, true);
    }
}
