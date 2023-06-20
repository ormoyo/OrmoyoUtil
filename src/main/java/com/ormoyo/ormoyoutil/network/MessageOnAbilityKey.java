package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.AbilityKeybindingBase;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_SERVER)
public class MessageOnAbilityKey extends AbstractMessage<MessageOnAbilityKey>
{
    private AbilityEntry entry;
    private String keybind;

    private boolean isPressed;

    public MessageOnAbilityKey()
    {
    }

    public MessageOnAbilityKey(AbilityEntry entry, String keybind, boolean isPressed)
    {
        this.entry = entry;
        this.keybind = keybind;

        this.isPressed = isPressed;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeRegistryIdUnsafe(Ability.getAbilityRegistry(), this.entry);
        buffer.writeVarInt(this.keybind == null ? 0 :
                AbilityKeybindingBase.convertKeyToId(this.keybind));

        buffer.writeBoolean(this.isPressed);
    }

    @NetworkDecoder(MessageOnAbilityKey.class)
    public static MessageOnAbilityKey decode(PacketBuffer buffer)
    {
        AbilityEntry entry = buffer.readRegistryIdUnsafe(Ability.getAbilityRegistry());
        int id = buffer.readVarInt();

        boolean isPressed = buffer.readBoolean();
        String keybind = id == 0 ? null :
                AbilityKeybindingBase.convertIdToKey(id);

        return new MessageOnAbilityKey(entry, keybind, isPressed);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
        Ability ability = ((IAbilityHolder) player).getAbility(this.entry.getAbilityClass());
        if (ability instanceof AbilityKeybindingBase)
        {
            AbilityKeybindingBase keybindingBase = (AbilityKeybindingBase) ability;
            if (this.isPressed)
            {
                keybindingBase.onKeyPress(this.keybind);
                return;
            }

            keybindingBase.onKeyRelease(this.keybind);
        }
    }
}
