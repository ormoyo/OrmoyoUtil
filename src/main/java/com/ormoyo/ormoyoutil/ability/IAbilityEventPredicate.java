package com.ormoyo.ormoyoutil.ability;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Checks if an event should get invoked for the ability
 *
 * @param <T> Event type
 */
@FunctionalInterface
public interface IAbilityEventPredicate<T extends Event>
{
    boolean test(Ability ability, T event);
}