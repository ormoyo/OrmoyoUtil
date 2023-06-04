package com.ormoyo.ormoyoutil.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Populate the annotated field with the mod's NetworkWrapper. The wrapper will be used for network messages annotated by {@link NetworkMessage}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkWrapper
{
}
