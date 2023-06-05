package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.AbilityCooldown;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class HoldAbility extends AbilityCooldown
{
    private final Map<String, MutableBoolean> isHolding = new NonNullMap<>(this.getKeybinds().length, new MutableBoolean(), true);

    public HoldAbility(IAbilityHolder owner)
    {
        super(owner);
    }

    public abstract boolean hold(String keybind);

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        for (KeyBinding keybind : this.getKeybinds())
        {
            String keyName = this.getKeybindName(keybind);
            if (this.isHolding.get(keyName).booleanValue() && !this.isOnCooldown())
            {
                this.hold(keyName);
            }
        }
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

    protected boolean isHolding(String keybind)
    {
        return this.isHolding.get(keybind).booleanValue();
    }

    protected boolean isHolding()
    {
        return this.isHolding(null);
    }
}
