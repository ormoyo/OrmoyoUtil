package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;

@NetworkMessage(modid = OrmoyoUtil.MODID)
public class MessageTest extends AbstractMessage<MessageTest>
{
    String player;

    public MessageTest(String player)
    {
        this.player = player;
    }

    @Override
    public void encode(PacketBuffer buf)
    {
        buf.writeString(this.player);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
        System.out.println(this.player);
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
        System.out.println(this.player);
    }
}
