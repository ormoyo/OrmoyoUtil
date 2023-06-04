package com.ormoyo.ormoyoutil.ability;

import net.minecraftforge.eventbus.api.Event;

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