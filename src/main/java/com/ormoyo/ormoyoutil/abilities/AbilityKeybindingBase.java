package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.network.MessageOnAbilityKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AbilityKeybindingBase extends Ability
{
    private KeyBinding keybind;
    private boolean hasBeenPressed;

    public AbilityKeybindingBase(EntityPlayer owner)
    {
        super(owner);
    }

    @Override
    public void onUpdate()
    {
        if (this.owner == null)
        {
            return;
        }

        if (this.owner.world.isRemote)
        {
            if (this.getKeybind() != null)
            {
                if (this.getKeybind().isKeyDown() && !this.hasBeenPressed)
                {
                    this.onKeyPress();
                    this.hasBeenPressed = true;
                }
                else if (!this.getKeybind().isKeyDown() && this.hasBeenPressed)
                {
                    this.onKeyRelease();
                    this.hasBeenPressed = false;
                }
            }
        }
    }

    /**
     * @return The keybind that is assigned to this ability
     */
    @SideOnly(Side.CLIENT)
    public KeyBinding getKeybind()
    {
        if (this.keybind == null)
        {
            if (this.getKeybindCode() > 0)
            {
                ResourceLocation location = this.getRegistryName();
                for (KeyBinding keybind : Minecraft.getMinecraft().gameSettings.keyBindings)
                {
                    if (keybind.getKeyDescription().equals("key." + location.getNamespace() + "." + location.getPath()))
                    {
                        this.keybind = keybind;
                    }
                }
            }
        }
        return this.keybind;
    }

    public void onKeyPress()
    {
        if (this.owner.world.isRemote)
        {
            OrmoyoUtil.NETWORK_WRAPPER.sendToServer(new MessageOnAbilityKey(this, true));
        }
    }

    public void onKeyRelease()
    {
        if (this.owner.world.isRemote)
        {
            OrmoyoUtil.NETWORK_WRAPPER.sendToServer(new MessageOnAbilityKey(this, false));
        }
    }

    /**
     * @return The keycode of the ability to be activated with
     */
    @SideOnly(Side.CLIENT)
    public abstract int getKeybindCode();
}
