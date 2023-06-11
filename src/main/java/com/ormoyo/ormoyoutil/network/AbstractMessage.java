package com.ormoyo.ormoyoutil.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Tuple;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A template message to make networking easier.
 * <br><br>
 * Every class who extends this needs to have a {@link NetworkDecoder} for it to work
 *
 * @param <T> The message type (You can just use the class itself)
 */
@SuppressWarnings("unchecked")
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
        return (T) new MessageTest("test");
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

    private static Tuple<? extends AbstractMessage<?>, NetworkEvent.Context> contextTuple;
    static Optional<Tuple<? extends AbstractMessage<?>, NetworkEvent.Context>> getContext()
    {
        return Optional.ofNullable(contextTuple);
    }

    public static <T extends AbstractMessage<T>> void onMessage(AbstractMessage<T> message, Supplier<NetworkEvent.Context> ctx)
    {
        if (message == null)
            return;

        NetworkEvent.Context context = ctx.get();

        contextTuple = new Tuple<>(message, context);

        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> MessageMethods.ServerMethods::onServerMessage);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MessageMethods.ClientMethods::onClientMessage);
    }
}
