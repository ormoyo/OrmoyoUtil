package com.ormoyo.ormoyoutil.network;

import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Populate the annotated field with the mod's {@link SimpleChannel}. The channel will be used for network messages annotated by {@link NetworkMessage}
 *
 * @apiNote You can put this method in the network message class or anywhere
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkChannel
{
}
