package com.ormoyo.ormoyoutil.network;

import net.minecraftforge.fml.relauncher.Side;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers the network message with the specified mod network wrapper that was created with {@link NetworkWrapper}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkMessage
{
    String modid();

    Side[] side() default {Side.CLIENT, Side.SERVER};
}
