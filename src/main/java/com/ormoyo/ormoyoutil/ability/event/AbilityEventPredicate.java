package com.ormoyo.ormoyoutil.ability.event;

import com.ormoyo.ormoyoutil.ability.Ability;
import net.minecraftforge.eventbus.api.Event;

/**
 * Checks if an event should get invoked for the ability
 *
 * @param <T> Event type
 */
@FunctionalInterface
public interface AbilityEventPredicate<T extends Event>
{
    boolean test(Ability ability, T event);
}