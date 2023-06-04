package com.ormoyo.ormoyoutil.network.datasync;

import net.minecraft.network.datasync.DataSerializer;

public class AbilityDataParameter<T>
{
    private final DataSerializer<T> serializer;
    private final int id;

    public AbilityDataParameter(int id, DataSerializer<T> serlizer)
    {
        this.id = id;
        this.serializer = serlizer;
    }

    public DataSerializer<T> getSerializer()
    {
        return this.serializer;
    }

    public int getId()
    {
        return this.id;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof AbilityDataParameter)
        {
            AbilityDataParameter<?> dataValue = (AbilityDataParameter<?>) obj;
            return this.id == dataValue.id;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return this.id;
    }
}
