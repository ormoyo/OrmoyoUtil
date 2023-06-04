package com.ormoyo.ormoyoutil.util;

import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NonNullMap<K, V> extends HashMap<K, V>
{
    final Supplier<V> defaultValue;

    @SuppressWarnings("unchecked")
    public NonNullMap()
    {
        this((V) new Object());
    }

    public NonNullMap(V defaultValue)
    {
        Validate.notNull(defaultValue);
        this.defaultValue = () -> defaultValue;
    }

    public NonNullMap(Supplier<V> defaultValueFunc)
    {
        Validate.notNull(defaultValueFunc);
        this.defaultValue = defaultValueFunc;
    }

    @Override
    public V put(K key, V value)
    {
        Validate.notNull(key);
        Validate.notNull(value);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        m.forEach((k, v) ->
        {
            Validate.notNull(k);
            Validate.notNull(v);
        });
        super.putAll(m);
    }

    @Override
    public V putIfAbsent(K key, V value)
    {
        Validate.notNull(key);
        Validate.notNull(value);
        return super.putIfAbsent(key, value);
    }

    @Override
    public V get(Object key)
    {
        V v = super.get(key);
        if (v == null)
        {
            v = this.defaultValue.get();
        }
        return v;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue)
    {
        Validate.notNull(defaultValue);
        return super.getOrDefault(key, defaultValue);
    }
}
