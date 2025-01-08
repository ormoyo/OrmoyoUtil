package com.ormoyo.ormoyoutil.ability;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import com.ormoyo.ormoyoutil.network.MessageOnAbilityKey;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbilityKeybindingBase extends Ability
{
    static final BiMap<String, Integer> KEYBIND_IDS = HashBiMap.create();
    final Map<String, MutableBoolean> hasBeenPressed = new NonNullMap<>(MutableBoolean::new, true);

    public AbilityKeybindingBase(AbilityHolder owner)
    {
        super(owner);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AbilityEventHandler.ClientEventHandler.onKeybindBaseConstruct(this));
    }

    @Override
    public void tick()
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.clientTick(this));
    }

    /**
     * @param keybind The pressed keybind description. If it's the key returned by {@link #getKeybinding()} ()} then this will be null.
     */
    public void onKeyPress(@Nullable String keybind)
    {
        if (!this.getOwner().world.isRemote)
            return;

        if (!this.isServerAbility())
            return;

        OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageOnAbilityKey(this.getEntry(), keybind, true));
    }

    /**
     * @param keybind The pressed keybind description. If it's the key returned by {@link #getKeybinding()} ()} then this will be null.
     */
    public void onKeyRelease(@Nullable String keybind)
    {
        if (!this.getOwner().world.isRemote)
            return;

        if (!this.isServerAbility())
            return;

        OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageOnAbilityKey(this.getEntry(), keybind, false));
    }

    public final Collection<String> getKeys()
    {
        return Collections.unmodifiableSet(this.hasBeenPressed.keySet());
    }

    /**
     * @return The first keybinding that is assigned to this ability.
     */
    @OnlyIn(Dist.CLIENT)
    public KeyBinding getKeybinding()
    {
        return this.getKeyBindings()[0];
    }

    @OnlyIn(Dist.CLIENT)
    protected final KeyBinding getKeyBindingFromName(@Nullable String keybind)
    {
        return keybind == null ? this.getKeybinding() : Arrays.stream(this.getKeyBindings())
                .filter(key -> keybind.equals(key.getKeyDescription()))
                .findAny()
                .orElse(null);
    }

    @OnlyIn(Dist.CLIENT)
    protected final String getKeyBindingName(KeyBinding keybind)
    {
        return keybind.getKeyDescription()
                .equals(this.getKeybinding().getKeyDescription()) ?
                null :
                keybind.getKeyDescription();
    }

    private KeyBinding[] keyBindings;

    @OnlyIn(Dist.CLIENT)
    public KeyBinding[] getKeyBindings()
    {
        return keyBindings != null ? (keyBindings = ClientHandler.createKeybindingsFromRegistry(this)) : new KeyBinding[0];
    }

    public static int convertKeyToId(String keybind)
    {
        return KEYBIND_IDS.get(keybind);
    }

    public static String convertIdToKey(int id)
    {
        return KEYBIND_IDS.inverse().get(id);
    }

    private static class ClientHandler
    {
        private static void clientTick(AbilityKeybindingBase ability)
        {
            if (!ability.getOwner().world.isRemote)
                return;

            for (KeyBinding keybind : ability.getKeyBindings())
            {
                String keyName = ability.getKeyBindingName(keybind);

                if (ability instanceof AbilityCooldown && ((AbilityCooldown) ability).isOnCooldown(keyName))
                    continue;

                MutableBoolean hasBeenPressed = ability.hasBeenPressed.get(keyName);
                if (keybind.isKeyDown() && !hasBeenPressed.booleanValue())
                {
                    ability.onKeyPress(keyName);
                    hasBeenPressed.setTrue();
                }
                else if (!keybind.isKeyDown() && hasBeenPressed.booleanValue())
                {
                    ability.onKeyRelease(keyName);
                    hasBeenPressed.setFalse();
                }
            }
        }

        private static KeyBinding[] createKeybindingsFromRegistry(AbilityKeybindingBase ability)
        {
            List<Integer> indices = ability.getEntry().getKeyBindingIndices();
            KeyBinding[] keybindings = new KeyBinding[indices.size()];

            for (int i = 0; i < keybindings.length; i++)
                keybindings[i] = Minecraft.getInstance().gameSettings.keyBindings[indices.get(i)];

            return keybindings;
        }
    }
}