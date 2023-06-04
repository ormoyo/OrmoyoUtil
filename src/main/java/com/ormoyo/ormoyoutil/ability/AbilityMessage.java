package com.ormoyo.ormoyoutil.ability;

public class AbilityMessage
{
    private final String key;
    private final Object value;

    public AbilityMessage(String key, Object value)
    {
        this.key = key;
        this.value = value;
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
