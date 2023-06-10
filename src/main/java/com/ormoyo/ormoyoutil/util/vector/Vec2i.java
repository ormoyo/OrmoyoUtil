package com.ormoyo.ormoyoutil.util.vector;

public class Vec2i
{
    public static final Vec2i NULL_VECTOR = new Vec2i(0, 0);

    private final int x;
    private final int y;

    public Vec2i(int x, int y)
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
