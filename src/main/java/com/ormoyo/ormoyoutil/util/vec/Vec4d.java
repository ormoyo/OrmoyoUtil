package com.ormoyo.ormoyoutil.util.vec;

import com.google.common.base.MoreObjects;

public class Vec4d
{
    public static final Vec4d NULL_VECTOR = new Vec4d(0, 0, 0, 0);
    public final double x;
    public final double y;
    public final double z;
    public final double w;

    public Vec4d(double xIn, double yIn, double zIn, double wIn)
    {
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
        this.w = wIn;
    }

    public Vec4d(Vec4i vector)
    {
        this(vector.getX(), vector.getY(), vector.getZ(), vector.getW());
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        else if (!(obj instanceof Vec4d))
        {
            return false;
        }
        else
        {
            Vec4d Vec4d = (Vec4d) obj;

            if (this.x != Vec4d.x)
            {
                return false;
            }
            else if (this.y != Vec4d.y)
            {
                return false;
            }
            else if (this.z != Vec4d.z)
            {
                return false;
            }
            else
            {
                return this.w == Vec4d.w;
            }
        }
    }

    public double getDistance(double xIn, double yIn, double zIn, double wIn)
    {
        double d0 = this.x - xIn;
        double d1 = this.y - yIn;
        double d2 = this.z - zIn;
        double d3 = this.w - wIn;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3);
    }

    public double distanceSq(double toX, double toY, double toZ, double toW)
    {
        double d0 = this.x - toX;
        double d1 = this.y - toY;
        double d2 = this.z - toZ;
        double d3 = this.w - toW;
        return d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3;
    }

    public double distanceSqToCenter(double xIn, double yIn, double zIn, double wIn)
    {
        double d0 = this.x + 0.5D - xIn;
        double d1 = this.y + 0.5D - yIn;
        double d2 = this.z + 0.5D - zIn;
        double d3 = this.w + 0.5D - wIn;
        return d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3;
    }

    public double distanceSq(Vec4d to)
    {
        return this.distanceSq(to.x, to.y, to.z, to.w);
    }

    public String toString()
    {
        return MoreObjects.toStringHelper(this).add("x", this.x).add("y", this.y).add("z", this.z).add("w", this.w).toString();
    }
}
