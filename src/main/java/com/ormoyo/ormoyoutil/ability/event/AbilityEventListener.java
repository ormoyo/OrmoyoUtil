package com.ormoyo.ormoyoutil.ability.event;


import com.ormoyo.ormoyoutil.ability.Ability;
import net.minecraftforge.eventbus.api.Event;

import java.lang.reflect.Method;

public interface AbilityEventListener<T extends Event>
{
    void invoke(Ability ability, T event);

    AbilityEventPredicate<T> getEventPredicate();

    Class<T> getEventClass();

    Method getMethod();
}
