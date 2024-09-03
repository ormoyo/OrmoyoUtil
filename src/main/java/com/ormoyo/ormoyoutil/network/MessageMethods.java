package com.ormoyo.ormoyoutil.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;

@SuppressWarnings("ConstantConditions")
class MessageMethods
{
    public static class ServerMethods
    {
        public static void onServerMessage()
        {
            Optional<Tuple<? extends AbstractMessage<?>, NetworkEvent.Context>> ctx = AbstractMessage.getContext();

            if (!ctx.isPresent())
                return;

            AbstractMessage<?> message = ctx.get().getA();
            NetworkEvent.Context context = ctx.get().getB();

            context.enqueueWork(() -> message.onServerReceived(context.getSender().getServer(), context.getSender(), context));
            context.setPacketHandled(true);
        }
    }

    public static class ClientMethods
    {
        public static void onClientMessage()
        {
            Optional<Tuple<? extends AbstractMessage<?>, NetworkEvent.Context>> ctx = AbstractMessage.getContext();

            if (!ctx.isPresent())
                return;

            AbstractMessage<?> message = ctx.get().getA();
            NetworkEvent.Context context = ctx.get().getB();

            if (EffectiveSide.get().isServer())
            {
                context.enqueueWork(() -> message.onServerReceived(context.getSender().getServer(), context.getSender(), context));
                context.setPacketHandled(true);

                return;
            }

            context.enqueueWork(() -> message.onClientReceived(Minecraft.getInstance().player, context));
            context.setPacketHandled(true);
        }
    }
}
