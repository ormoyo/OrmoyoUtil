package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.util.NonNullMap;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class AbilityCooldown extends AbilityKeybindingBase
{
    private final Map<String, MutableInt> cooldownTicks = new NonNullMap<>(MutableInt::new, true);
    private final Map<String, MutableBoolean> isOnCooldown = new NonNullMap<>(MutableBoolean::new, true);

    public AbilityCooldown(AbilityHolder owner)
    {
        super(owner);
    }

    @Override
    public void tick()
    {
        super.tick();
        for (String keybind : this.getKeys())
        {
            MutableBoolean isOnCooldown = this.isOnCooldown.get(keybind);
            MutableInt cooldownTick = this.cooldownTicks.get(keybind);

            cooldownTick.increment();
            cooldownTick.setValue(cooldownTick.intValue() % (this.getCooldown(keybind) + 1));

            if (cooldownTick.intValue() == 0)
            {
                isOnCooldown.setFalse();
            }
        }
    }

    /**
     * @param keybind The keybind description. If it's the key created by {@link #getKeyCode()} then this will be null.
     * @return The specific cooldown in ticks for the keybind
     */
    public abstract int getCooldown(@Nullable String keybind);

    protected void setIsOnCooldown(@Nullable String keybind, boolean isOnCooldown)
    {
        if (this.getCooldown(keybind) <= 0)
            return;

        if (!isOnCooldown)
            this.cooldownTicks.get(keybind).setValue(0);

        this.isOnCooldown.get(keybind).setValue(isOnCooldown);
    }

    public boolean isOnCooldown(String keybind)
    {
        return this.isOnCooldown.get(keybind).booleanValue();
    }

    public boolean isOnCooldown()
    {
        return this.isOnCooldown(null);
    }
}
