package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@NetworkMessage(modid = OrmoyoUtil.MODID)
public class MessageTest extends AbstractMessage<MessageTest>
{
    String player;

    public MessageTest()
    {
    }

    public MessageTest(EntityPlayer player)
    {
        this.player = player.getName();
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = Utils.getPacketBuffer(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = Utils.getPacketBuffer(buf);
    }

    @Override
    public void onClientReceived(Minecraft client, EntityPlayer player, MessageContext messageContext)
    {
        System.out.println(this.player);
    }

    @Override
    public void onServerReceived(MinecraftServer server, EntityPlayer player, MessageContext messageContext)
    {
        System.out.println(this.player);
    }
}
