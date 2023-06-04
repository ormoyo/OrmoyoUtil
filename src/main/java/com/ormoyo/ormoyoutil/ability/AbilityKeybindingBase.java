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

public abstract class AbilityKeybindingBase extends Ability
{
    private KeyBinding[] keybinds;
    private final NonNullMap<String, Boolean> hasBeenPressed = new NonNullMap<>(false, true);

    public AbilityKeybindingBase(IAbilityHolder owner)
    {
        super(owner);
    }

    @Override
    public void onUpdate()
    {
        if (!this.owner.world.isRemote)
            return;

        for (KeyBinding keybind : this.keybinds)
        {
            if (keybind.isKeyDown() && !this.hasBeenPressed.get(keybind.getKeyDescription()))
            {
                String keyName = keybind.getKeyDescription()
                        .equals(this.getKey().getKeyDescription()) ?
                        null :
                        keybind.getKeyDescription();

                this.onKeyPress(keyName);
                this.hasBeenPressed.put(keyName, true);
            }
            else if (!keybind.isKeyDown() && this.hasBeenPressed.get(keybind.getKeyDescription()))
            {
                String keyName = keybind.getKeyDescription()
                        .equals(this.getKey().getKeyDescription()) ?
                        null :
                        keybind.getKeyDescription();

                this.onKeyRelease(keyName);
                this.hasBeenPressed.put(keyName, false);
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
                    this.keybinds = new KeyBinding[]{keybinding};
                }
            }
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

    public InputMappings.Type getKeyType()
    {
        return InputMappings.Type.KEYSYM;
    }

    /**
     * @return The keycode of the ability to be activated with
     */
    public abstract int getKeyCode();

    public KeyModifier getKeyModifier()
    {
        return KeyModifier.NONE;
    }

    public IKeyConflictContext getKeyConflictContext()
    {
        return KeyConflictContext.IN_GAME;
    }
}
