package com.ormoyo.ormoyoutil.ability;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.lang.reflect.Method;

public interface IAbilityEventListener
{
    void invoke(Ability instance, Event event);

    @SuppressWarnings("rawtypes")
    IAbilityEventPredicate getEventPredicate();

    Class<? extends Event> getEventClass();

    Method getMethod();
}
