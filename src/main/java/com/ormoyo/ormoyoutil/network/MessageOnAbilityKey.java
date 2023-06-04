package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.abilities.AbilityKeybindingBase;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@NetworkMessage(modid = OrmoyoUtil.MODID, side = Side.SERVER)
public class MessageOnAbilityKey extends AbstractMessage<MessageOnAbilityKey>
{
    ResourceLocation location;
    boolean press;

    public MessageOnAbilityKey()
    {
    }

    public MessageOnAbilityKey(Ability ability, boolean press)
    {
        this.location = ability.getRegistryName();
        this.press = press;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = Utils.getPacketBuffer(buf);

        this.location = buffer.readResourceLocation();
        this.press = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = Utils.getPacketBuffer(buf);

        buffer.writeResourceLocation(this.location);
        buf.writeBoolean(this.press);
    }

    @Override
    public void onClientReceived(Minecraft client, EntityPlayer player, MessageContext messageContext)
    {
    }

    @Override
    public void onServerReceived(MinecraftServer server, EntityPlayer player, MessageContext messageContext)
    {
        Ability ability = OrmoyoUtil.PROXY.getAbility(this.location, player);
        if (ability instanceof AbilityKeybindingBase)
        {
            AbilityKeybindingBase keybindingBase = (AbilityKeybindingBase) ability;
            if (this.press)
            {
                keybindingBase.onKeyPress();
            }
            else
            {
                keybindingBase.onKeyRelease();
            }
        }
    }
}
