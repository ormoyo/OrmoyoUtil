package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;

@NetworkMessage(modid = OrmoyoUtil.MODID)
public class MessageTest extends AbstractMessage<MessageTest>
{
    int playerId;

    public MessageTest(AbilityHolder holder)
    {
        this.playerId = holder.asPlayer().getEntityId();
    }

    public MessageTest(int playerId)
    {
        this.playerId = playerId;
    }

    @Override
    public void encode(PacketBuffer buf)
    {
        buf.writeVarInt(this.playerId);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
        PlayerEntity playerEntity = (PlayerEntity) player.getEntityWorld().getEntityByID(this.playerId);
        System.out.println(playerEntity.getGameProfile().getName());
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
        PlayerEntity playerEntity = (PlayerEntity) player.getEntityWorld().getEntityByID(this.playerId);
        System.out.println(playerEntity.getGameProfile().getName());
    }
}
