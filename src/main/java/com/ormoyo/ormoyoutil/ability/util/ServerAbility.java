package com.ormoyo.ormoyoutil.ability.util;

import net.minecraftforge.api.distmarker.Dist;

/**
 * Use on ability to only be registered on the logical server (Dedicated Server or integrated server).
 */
public @interface ServerAbility
{
    /**
     * @return The dist to register the ability on.
     */
    Dist[] value() default { Dist.CLIENT, Dist.DEDICATED_SERVER };
}
