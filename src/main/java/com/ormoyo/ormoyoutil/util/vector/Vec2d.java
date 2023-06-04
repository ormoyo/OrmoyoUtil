package com.ormoyo.ormoyoutil.util.vector;

import com.google.common.base.MoreObjects;

public class Vec2d
{
    public static final Vec2d NULL_VECTOR = new Vec2d(0, 0);
    public final double x;
    public final double y;

    public Vec2d(double xIn, double yIn)
    {
        this.x = xIn;
        this.y = yIn;
    }

    public Vec2d(Vec2i vector)
    {
        this(vector.getX(), vector.getY());
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        else if (!(obj instanceof Vec2d))
        {
            return false;
        }
        else
        {
            Vec2d Vec2d = (Vec2d) obj;

            if (this.x != Vec2d.x)
            {
                return false;
            }
            else
            {
                return this.y == Vec2d.y;
            }
        }
    }

    public double getDistance(double xIn, double yIn)
    {
        double d0 = this.x - xIn;
        double d1 = this.y - yIn;
        return Math.sqrt(d0 * d0 + d1 * d1);
    }

    public double distanceSq(double toX, double toY)
    {
        double d0 = this.x - toX;
        double d1 = this.y - toY;
        return d0 * d0 + d1 * d1;
    }

    public double distanceSqToCenter(double xIn, double yIn)
    {
        double d0 = this.x + 0.5D - xIn;
        double d1 = this.y + 0.5D - yIn;
        return d0 * d0 + d1 * d1;
    }

    public double distanceSq(Vec2d to)
    {
        return this.distanceSq(to.x, to.y);
    }

    public String toString()
    {
        return MoreObjects.toStringHelper(this).add("x", this.x).add("y", this.y).toString();
    }
}
