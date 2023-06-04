package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IMessage, IMessageHandler<T, IMessage>
{
    @Override
    public IMessage onMessage(T message, MessageContext messageContext)
    {
        OrmoyoUtil.PROXY.handleMessage(message, messageContext);
        return null;
    }

    /**
     * Executes when the message is received on CLIENT side.
     *
     * @param client         the minecraft client instance.
     * @param player         The client player entity.
     * @param messageContext the message context.
     */
    @SideOnly(Side.CLIENT)
    public abstract void onClientReceived(Minecraft client, EntityPlayer player, MessageContext messageContext);

    /**
     * Executes when the message is received on SERVER side.
     *
     * @param server         the minecraft server instance.
     * @param player         The player who sent the message to the server.
     * @param messageContext the message context.
     */
    public abstract void onServerReceived(MinecraftServer server, EntityPlayer player, MessageContext messageContext);
}
