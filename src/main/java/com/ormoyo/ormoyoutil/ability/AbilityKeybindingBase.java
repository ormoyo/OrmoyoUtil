package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.network.MessageOnAbilityKey;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public abstract class AbilityKeybindingBase extends Ability
{
    private final Map<String, MutableBoolean> hasBeenPressed = new NonNullMap<>(new MutableBoolean(), true);

    @OnlyIn(Dist.CLIENT)
    KeyBinding[] keybinds;

    public AbilityKeybindingBase(IAbilityHolder owner)
    {
        super(owner);

        ClientEventHandler.currentConstruct = this;
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientEventHandler::onKeybindBaseConstruct);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
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
     * @param keybind The pressed keybind description. If it's the key created by {@link #getKeyCode()} then this will be null.
     */
    public void onKeyPress(@Nullable String keybind)
    {
        if (!this.owner.world.isRemote)
            return;

        OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageOnAbilityKey(this.getEntry(), keybind, true));
    }

    /**
     * @param keybind The pressed keybind description. If it's the key created by {@link #getKeyCode()} then this will be null.
     */
    public void onKeyRelease(@Nullable String keybind)
    {
        if (!this.owner.world.isRemote)
            return;

        OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageOnAbilityKey(this.getEntry(), keybind, false));
    }

    /**
     * @return The keycode of the ability to be activated with
     * @see KeyEvent
     */
    public abstract int getKeyCode();

    @OnlyIn(Dist.CLIENT)
    public InputMappings.Type getKeyType()
    {
        return InputMappings.Type.KEYSYM;
    }

    @OnlyIn(Dist.CLIENT)
    public KeyModifier getKeyModifier()
    {
        return KeyModifier.NONE;
    }

    @OnlyIn(Dist.CLIENT)
    public IKeyConflictContext getKeyConflictContext()
    {
        return KeyConflictContext.IN_GAME;
    }

    /**
     * @return The keybind that is assigned to this ability
     */
    @OnlyIn(Dist.CLIENT)
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

    @OnlyIn(Dist.CLIENT)
    protected final KeyBinding getKeybindFromName(@Nullable String keybind)
    {
        return keybind == null ? this.getKey() : Arrays.stream(this.keybinds)
                .filter(key -> keybind.equals(key.getKeyDescription()))
                .findAny()
                .orElseGet(() -> this.keybinds[0]);
    }

    @OnlyIn(Dist.CLIENT)
    protected final String getKeybindName(KeyBinding keybind)
    {
        return keybind.getKeyDescription()
                .equals(this.getKey().getKeyDescription()) ?
                null :
                keybind.getKeyDescription();
    }

    /**
     * If you need to use multiple keybindings.
     */
    @OnlyIn(Dist.CLIENT)
    public KeyBinding[] getKeybinds()
    {
        return this.keybinds;
    }
}