package com.ormoyo.ormoyoutil.network;

import net.minecraftforge.fml.network.NetworkDirection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers the network message with the specified mod network wrapper that was created with {@link NetworkChannel}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkMessage
{
    String modid();

    NetworkDirection[] direction() default {};
}
