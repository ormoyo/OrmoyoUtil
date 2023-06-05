package com.ormoyo.ormoyoutil.util;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NonNullMap<K, V> extends HashMap<K, V>
{
    private final Supplier<V> defaultValue;
    private final boolean hasNullKeys;

    public NonNullMap(V defaultValue)
    {
        this(defaultValue, false);
    }

    public NonNullMap(Supplier<V> defaultValueFunc)
    {
        this(defaultValueFunc, false);
    }

    public NonNullMap(V defaultValue, boolean hasNullKeys)
    {
        super();

        Preconditions.checkNotNull(defaultValue);
        this.defaultValue = () -> defaultValue;

        this.hasNullKeys = hasNullKeys;
    }

    public NonNullMap(Supplier<V> defaultValueFunc, boolean hasNullKeys)
    {
        super();

        Preconditions.checkNotNull(defaultValueFunc);
        this.defaultValue = defaultValueFunc;

        this.hasNullKeys = hasNullKeys;
    }

    public NonNullMap(int initialCapacity, V defaultValue, boolean hasNullKeys)
    {
        super(initialCapacity);

        Preconditions.checkNotNull(defaultValue);
        this.defaultValue = () -> defaultValue;

        this.hasNullKeys = hasNullKeys;
    }

    public NonNullMap(int initialCapacity, Supplier<V> defaultValueFunc, boolean hasNullKeys)
    {
        super(initialCapacity);

        Preconditions.checkNotNull(defaultValueFunc);
        this.defaultValue = defaultValueFunc;

        this.hasNullKeys = hasNullKeys;
    }

    @Override
    public V put(K key, V value)
    {
        if (!hasNullKeys)
            Preconditions.checkNotNull(key);

        Preconditions.checkNotNull(value);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        m.forEach((k, v) ->
        {
            if (!hasNullKeys)
                Preconditions.checkNotNull(k);

            Preconditions.checkNotNull(v);
        });
        super.putAll(m);
    }

    @Override
    public V putIfAbsent(K key, V value)
    {
        if (!hasNullKeys)
            Preconditions.checkNotNull(key);

        Preconditions.checkNotNull(value);
        return super.putIfAbsent(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key)
    {
        V v = super.get(key);

        if (v == null)
        {
            v = this.defaultValue.get();
            this.put((K) key, v);
        }

        return v;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue)
    {
        Preconditions.checkNotNull(defaultValue);
        return super.getOrDefault(key, defaultValue);
    }
}
