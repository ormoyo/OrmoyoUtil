package com.ormoyo.ormoyoutil.ability;

import it.unimi.dsi.fastutil.Hash;

class KeyHashStrategy implements Hash.Strategy<String>
{
    @Override
    public int hashCode(String o)
    {
        return o.hashCode();
    }

    @Override
    public boolean equals(String a, String b)
    {
        if (a == null || b == null)
            return a == b;
        if (a.length() != b.length())
            return false;

        for (int i = "key.".length(); i < a.length(); i++)
        {
            char c = a.charAt(i);
            if (c != b.charAt(i))
                return false;
        }
        return true;
    }
}
