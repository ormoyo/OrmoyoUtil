package com.ormoyo.ormoyoutil.util;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NonNullMap<K, V> extends HashMap<K, V>
{
    private final Supplier<V> defaultValue;
    private final boolean allowNullKeys;

    public NonNullMap(V defaultValue)
    {
        this(defaultValue, false);
    }

    public NonNullMap(Supplier<V> defaultValueSupplier)
    {
        this(defaultValueSupplier, false);
    }

    public NonNullMap(V defaultValue, boolean allowNullKeys)
    {
        super();

        Preconditions.checkNotNull(defaultValue);
        this.defaultValue = () -> defaultValue;

        this.allowNullKeys = allowNullKeys;
    }

    public NonNullMap(Supplier<V> valueSupplier, boolean allowNullKeys)
    {
        super();

        Preconditions.checkNotNull(valueSupplier);
        this.defaultValue = valueSupplier;

        this.allowNullKeys = allowNullKeys;
    }

    public NonNullMap(int initialCapacity, V defaultValue, boolean allowNullKeys)
    {
        super(initialCapacity);

        Preconditions.checkNotNull(defaultValue);
        this.defaultValue = () -> defaultValue;

        this.allowNullKeys = allowNullKeys;
    }

    public NonNullMap(int initialCapacity, Supplier<V> valueSupplier, boolean allowNullKeys)
    {
        super(initialCapacity);

        Preconditions.checkNotNull(valueSupplier);
        this.defaultValue = valueSupplier;

        this.allowNullKeys = allowNullKeys;
    }

    @Override
    public V put(K key, V value)
    {
        if (!this.allowNullKeys)
            Preconditions.checkNotNull(key);

        Preconditions.checkNotNull(value);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        m.forEach((k, v) ->
        {
            if (!this.allowNullKeys)
                Preconditions.checkNotNull(k);

            Preconditions.checkNotNull(v);
        });
        super.putAll(m);
    }

    @Override
    public V putIfAbsent(K key, V value)
    {
        if (!this.allowNullKeys)
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
