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

    public MessageTest(PlayerEntity player)
    {
        this.player = String.valueOf(player.getName());
    }

    @Override
    public void encode(PacketBuffer buf)
    {
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
