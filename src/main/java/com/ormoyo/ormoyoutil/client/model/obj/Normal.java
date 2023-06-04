package com.ormoyo.ormoyoutil.client.model.obj;

import org.lwjgl.util.vector.Vector3f;

import java.util.Locale;


public class Normal
{
    private final Vector3f vec;
    private int index;

    public Normal(float x, float y, float z)
    {
        this(new Vector3f(x, y, z));
    }

    public Normal(Vector3f position)
    {
        this.vec = position;
    }

    public void register(OBJModel model)
    {
        this.index = model.getNormalIndex();
    }

    public Vector3f getVector()
    {
        return this.vec;
    }

    public int getIndex()
    {
        return this.index;
    }

    @Override
    public String toString()
    {
        return "vn " + String.format(Locale.US, "%.6f", this.vec.x) + " " + String.format(Locale.US, "%.6f", this.vec.y) + " " + String.format(Locale.US, "%.6f", this.vec.z);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (obj instanceof Normal)
        {
            Normal normal = (Normal) obj;
            return normal.vec.x == this.vec.x && normal.vec.y == this.vec.y && normal.vec.z == this.vec.z;
        }
        return false;
    }
}
