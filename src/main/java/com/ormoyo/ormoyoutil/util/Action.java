package com.ormoyo.ormoyoutil.util;

import java.util.Objects;

@FunctionalInterface
public interface Action
{
    void execute();

    default Action andThen(Action after)
    {
        Objects.requireNonNull(after);
        return () ->
        {
            this.execute();
            after.execute();
        };
    }
}
