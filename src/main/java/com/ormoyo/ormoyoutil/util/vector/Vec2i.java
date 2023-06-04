package com.ormoyo.ormoyoutil.util.vector;

import com.google.common.base.MoreObjects;
import net.minecraft.util.math.MathHelper;

import javax.annotation.concurrent.Immutable;

@Immutable
public class Vec2i implements Comparable<Vec2i>
{
    public static final Vec2i NULL_VECTOR = new Vec2i(0, 0);
    private final int x;
    private final int y;

    public Vec2i(int xIn, int yIn)
    {
        this.x = xIn;
        this.y = yIn;
    }

    public Vec2i(double xIn, double yIn)
    {
        this(MathHelper.floor(xIn), MathHelper.floor(yIn));
    }

    public boolean equals(Object p_equals_1_)
    {
        if (this == p_equals_1_)
        {
            return true;
        }
        else if (!(p_equals_1_ instanceof Vec2i))
        {
            return false;
        }
        else
        {
            Vec2i Vec2i = (Vec2i) p_equals_1_;

            if (this.getX() != Vec2i.getX())
            {
                return false;
            }
            else
            {
                return this.getY() == Vec2i.getY();
            }
        }
    }

    public int hashCode()
    {
        return (this.getY() * 31) * 31 + this.getX();
    }

    public int compareTo(Vec2i vector)
    {
        return this.getY() == vector.getY() ? this.getX() - vector.getX() : this.getY() - vector.getY();
    }

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }

    public double getDistance(int xIn, int yIn)
    {
        double d0 = this.getX() - xIn;
        double d1 = this.getY() - yIn;
        return Math.sqrt(d0 * d0 + d1 * d1);
    }

    public double distanceSq(double toX, double toY)
    {
        double d0 = (double) this.getX() - toX;
        double d1 = (double) this.getY() - toY;
        return d0 * d0 + d1 * d1;
    }

    public double distanceSqToCenter(double xIn, double yIn)
    {
        double d0 = (double) this.getX() + 0.5D - xIn;
        double d1 = (double) this.getY() + 0.5D - yIn;
        return d0 * d0 + d1 * d1;
    }

    public double distanceSq(Vec2i to)
    {
        return this.distanceSq(to.getX(), to.getY());
    }

    public String toString()
    {
        return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).toString();
    }
}
