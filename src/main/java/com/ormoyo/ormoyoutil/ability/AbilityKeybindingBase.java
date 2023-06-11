package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.network.MessageOnAbilityKey;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.awt.event.KeyEvent;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class AbilityKeybindingBase extends Ability
{
    private final KeyBinding[] keybinds;
    private final NonNullMap<String, MutableBoolean> hasBeenPressed = new NonNullMap<>(new MutableBoolean(), true);

    public AbilityKeybindingBase(IAbilityHolder owner)
    {
        super(owner);

        KeyBinding[] keybinds = this.getKeybinds();
        if(keybinds == null)
            keybinds = new KeyBinding[] {this.getKey()};

        this.keybinds = keybinds;
    }

    @Override
    public void onUpdate()
    {
        if (!this.owner.world.isRemote)
            return;

        for (KeyBinding keybind : this.keybinds)
        {
            String keyName = this.getKeybindName(keybind);
            MutableBoolean hasBeenPressed = this.hasBeenPressed.get(keyName);

            if (keybind.isKeyDown() && !hasBeenPressed.booleanValue())
            {
                this.onKeyPress(keyName);
                hasBeenPressed.setTrue();
            }
            else if (!keybind.isKeyDown() && hasBeenPressed.booleanValue())
            {
                this.onKeyRelease(keyName);
                hasBeenPressed.setFalse();
            }
        }
    }

    /**
     * @return The keybind that is assigned to this ability
     */
    public KeyBinding getKey()
    {
        if (this.keybinds == null || this.keybinds.length == 0)
        {
            if (this.getKeyCode() <= 0)
                return null;

            ResourceLocation location = this.getRegistryName();
            for (KeyBinding keybinding : Minecraft.getInstance().gameSettings.keyBindings)
            {
                if (keybinding.getKeyDescription().equals("key." + location.getNamespace() + "." + location.getPath()))
                {
                    return keybinding;
                }
            }

            return null;
        }

        return this.keybinds[0];
    }

    public KeyBinding[] getKeybinds()
    {
        return this.keybinds;
    }

    /**
     * @param keybind The pressed keybind description. If you are using {@link #getKeyCode()} you don't have to worry about this.
     */
    public void onKeyPress(String keybind)
    {
        if (!this.owner.world.isRemote)
            return;

        OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageOnAbilityKey(this.getEntry(), keybind, true));
    }

    /**
     * @param keybind The pressed keybind description. If you are using {@link #getKeyCode()} you don't have to worry about this.
     */
    public void onKeyRelease(String keybind)
    {
        if (!this.owner.world.isRemote)
            return;

        OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageOnAbilityKey(this.getEntry(), keybind, false));
    }

    protected final String getKeybindName(KeyBinding keybind)
    {
        return keybind.getKeyDescription()
                .equals(this.getKey().getKeyDescription()) ?
                null :
                keybind.getKeyDescription();
    }

    /**
     * @return The keycode of the ability to be activated with
     * @see KeyEvent
     */
    public abstract int getKeyCode();

    public InputMappings.Type getKeyType()
    {
        return InputMappings.Type.KEYSYM;
    }

    public KeyModifier getKeyModifier()
    {
        return KeyModifier.NONE;
    }

    public IKeyConflictContext getKeyConflictContext()
    {
        return KeyConflictContext.IN_GAME;
    }
}
