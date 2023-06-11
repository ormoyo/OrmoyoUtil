package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.util.NonNullMap;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class AbilityCooldown extends AbilityKeybindingBase
{
    private final Map<String, MutableInt> cooldownTicks = new NonNullMap<>(new MutableInt(), true);
    private final Map<String, MutableBoolean> isOnCooldown = new NonNullMap<>(new MutableBoolean(), true);

    public AbilityCooldown(IAbilityHolder owner)
    {
        super(owner);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onUpdate()
    {
        for (KeyBinding keybind : this.getKeybinds())
        {
            String keyName = this.getKeybindName(keybind);
            MutableBoolean isOnCooldown = this.isOnCooldown.get(keyName);

            if (!isOnCooldown.booleanValue())
            {
                super.onUpdate();
                return;
            }

            MutableInt cooldownTick = this.cooldownTicks.get(keyName);

            cooldownTick.increment();
            cooldownTick.setValue(cooldownTick.intValue() % this.getCooldown());

            if (cooldownTick.intValue() == 0)
            {
                isOnCooldown.setFalse();
            }
        }
    }

    protected void setIsOnCooldown(String keybind, boolean isOnCooldown)
    {
        if (this.getCooldown() <= 0)
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

    public abstract int getCooldown();
}
