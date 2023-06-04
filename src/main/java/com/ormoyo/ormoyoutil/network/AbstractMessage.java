package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.util.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A template message to make networking easier.
 * <br><br>
 * Every class who extends this needs to have a {@link NetworkDecoder} for it to work
 *
 * @param <T> The message type (You can just use the class itself)
 */
public abstract class AbstractMessage<T extends AbstractMessage<T>>
{

    /**
     * Use to write the message data into the {@link PacketBuffer}
     *
     * @param buffer The buffer to write data into
     */
    public abstract void encode(PacketBuffer buffer);

    /**
     * A template for a message's {@link NetworkDecoder} method
     *
     * @param <T>    The message type
     * @param buffer The buffer to read data from
     * @return A new instance of the message
     */
    @NetworkDecoder(MessageTest.class)
    public static <T extends AbstractMessage<T>> T decode(PacketBuffer buffer)
    {
        return null;
    }

    /**
     * Executes when the message is received on CLIENT side.
     *
     * @param player         The client player entity.
     * @param messageContext the message context.
     */
    public abstract void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext);

    /**
     * Executes when the message is received on SERVER side.
     *
     * @param player         The player who sent the message to the server.
     * @param messageContext the message context.
     */
    public abstract void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext);


    public static <T extends AbstractMessage<T>> void onMessage(AbstractMessage<T> message, Supplier<NetworkEvent.Context> ctx)
    {
        if (message == null)
            return;

        NetworkEvent.Context context = ctx.get();
        Action serverAction = () ->
        {
            context.enqueueWork(() -> message.onServerReceived(context.getSender().getServer(), context.getSender(), context));
            context.setPacketHandled(true);
        };

        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> serverAction::execute);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
        {
            if (EffectiveSide.get().isServer())
            {
                serverAction.execute();
                return;
            }

            context.enqueueWork(() -> message.onClientReceived(Minecraft.getInstance().player, context));
            context.setPacketHandled(true);
        });
    }
}
