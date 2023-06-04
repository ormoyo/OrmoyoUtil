package com.ormoyo.ormoyoutil.ability;


import net.minecraftforge.eventbus.api.Event;

import java.lang.reflect.Method;

public interface IAbilityEventListener
{
    void invoke(Ability ability, Event event);

    @SuppressWarnings("rawtypes")
    IAbilityEventPredicate getEventPredicate();

    Class<?> getEventClass();

    Method getMethod();
}
