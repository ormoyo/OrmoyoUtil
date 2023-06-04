package com.ormoyo.ormoyoutil.util.vec;

import com.google.common.base.MoreObjects;
import net.minecraft.util.math.MathHelper;

import javax.annotation.concurrent.Immutable;

@Immutable
public class Vec4i implements Comparable<Vec4i>
{
    public static final Vec4i NULL_VECTOR = new Vec4i(0, 0, 0, 0);
    private final int x;
    private final int y;
    private final int z;
    private final int w;

    public Vec4i(int xIn, int yIn, int zIn, int wIn)
    {
        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
        this.w = wIn;
    }

    public Vec4i(double xIn, double yIn, double zIn, double wIn)
    {
        this(MathHelper.floor(xIn), MathHelper.floor(yIn), MathHelper.floor(zIn), MathHelper.floor(wIn));
    }

    public boolean equals(Object p_equals_1_)
    {
        if (this == p_equals_1_)
        {
            return true;
        }
        else if (!(p_equals_1_ instanceof Vec4i))
        {
            return false;
        }
        else
        {
            Vec4i Vec4i = (Vec4i) p_equals_1_;

            if (this.getX() != Vec4i.getX())
            {
                return false;
            }
            else if (this.getY() != Vec4i.getY())
            {
                return false;
            }
            else if (this.getZ() != Vec4i.getZ())
            {
                return false;
            }
            else
            {
                return this.getW() == Vec4i.getW();
            }
        }
    }

    public int hashCode()
    {
        return (this.getY() + this.getZ() + this.getW() * 31) * 31 + this.getX();
    }

    public int compareTo(Vec4i vector)
    {
        if (this.getY() == vector.getY())
        {
            return this.getZ() == vector.getZ() ? this.getX() - vector.getX() : this.getZ() - vector.getZ();
        }
        else
        {
            return this.getY() - vector.getY();
        }
    }

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }

    public int getZ()
    {
        return this.z;
    }

    public int getW()
    {
        return this.w;
    }

    public Vec4i crossProduct(Vec4i vec)
    {
        return new Vec4i(this.getY() * vec.getZ() - this.getZ() * vec.getY(), this.getZ() * vec.getX() - this.getX() * vec.getZ(), this.getX() * vec.getY() - this.getY() * vec.getX(), w);
    }

    public double getDistance(int xIn, int yIn, int zIn, int wIn)
    {
        double d0 = this.getX() - xIn;
        double d1 = this.getY() - yIn;
        double d2 = this.getZ() - zIn;
        double d3 = this.getW() - wIn;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3);
    }

    public double distanceSq(double toX, double toY, double toZ, int toW)
    {
        double d0 = (double) this.getX() - toX;
        double d1 = (double) this.getY() - toY;
        double d2 = (double) this.getZ() - toZ;
        double d3 = this.getW() - toW;
        return d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3;
    }

    public double distanceSqToCenter(double xIn, double yIn, double zIn, int wIn)
    {
        double d0 = (double) this.getX() + 0.5D - xIn;
        double d1 = (double) this.getY() + 0.5D - yIn;
        double d2 = (double) this.getZ() + 0.5D - zIn;
        double d3 = this.getW() + 0.5D - wIn;
        return d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3;
    }

    public double distanceSq(Vec4i to)
    {
        return this.distanceSq(to.getX(), to.getY(), to.getZ(), to.getW());
    }

    public String toString()
    {
        return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).add("w", this.getW()).toString();
    }
}
