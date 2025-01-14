package com.ormoyo.ormoyoutil.ability.util;

import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;

public class AbilityMessage
{
    private final AbilityEntry<?> sender;
    private final String key;
    private final Object value;

    public AbilityMessage(String key, Object value)
    {
        this.sender = null;
        this.key = key;
        this.value = value;
    }

    public AbilityMessage(Ability sender, String key, Object value)
    {
        this.sender = sender.getEntry();
        this.key = key;
        this.value = value;
    }

    public AbilityEntry<?> getSender()
    {
        return this.sender;
    }

    public String getKey()
    {
        return this.key;
    }

    public Object getValue()
    {
        return this.value;
    }
}
