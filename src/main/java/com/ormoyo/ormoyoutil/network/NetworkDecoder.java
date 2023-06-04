package com.ormoyo.ormoyoutil.network;

import net.minecraft.network.PacketBuffer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This needs to have a {@link PacketBuffer} as a parameter, and it needs to return a new instance of the network message
 *
 * @see AbstractMessage#decode(PacketBuffer)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkDecoder
{
    /**
     * All the messages the decoder will be used for.
     */
    Class<? extends AbstractMessage<?>>[] value();
}
