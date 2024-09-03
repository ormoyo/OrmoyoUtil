package com.ormoyo.ormoyoutil.ability.util;

/**
 * Use on ability to only be registered on the client.
 */
public @interface ClientAbility
{
    /**
     * @return If this is true other clients will have the ability of the owner on their machine, otherwise only the client of the owner of the ability will contain it.<br><br>This can be used if client operations like rendering are needed to happen on the ability's owner model from other players machines.<br><br>The ability will be registered on the server side for this.<br><br> If {@link ClientAbility} isn't used the ability will only be contained on the server and on the client of the owner of the ability.
     */
    boolean share() default false;
}
