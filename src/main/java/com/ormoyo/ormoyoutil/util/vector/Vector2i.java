package com.ormoyo.ormoyoutil.util.vector;

public class Vector2i
{
    public static final Vector2i NULL_VECTOR = new Vector2i(0, 0);

    private final int x;
    private final int y;

    public Vector2i(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }
}
