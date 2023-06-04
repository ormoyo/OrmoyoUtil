package com.ormoyo.ormoyoutil.client.model.obj;

import org.lwjgl.util.vector.Vector3f;

import java.util.Locale;

public class Vertex
{
    private final Vector3f position;
    private int index;

    public Vertex(float x, float y, float z)
    {
        this(new Vector3f(x, y, z));
    }

    public Vertex(Vector3f position)
    {
        this.position = position;
    }

    public void register(OBJModel model)
    {
        this.index = model.getVertexIndex();
    }

    public Vector3f getPosition()
    {
        return this.position;
    }

    public int getIndex()
    {
        return this.index;
    }

    @Override
    public String toString()
    {
        return "v " + String.format(Locale.US, "%.6f", this.position.x) + " " + String.format(Locale.US, "%.6f", this.position.y) + " " + String.format(Locale.US, "%.6f", this.position.z);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Vertex)
        {
            Vertex vertex = (Vertex) obj;
            return vertex.position.x == this.position.x && vertex.position.y == this.position.y && vertex.position.z == this.position.z;
        }
        return false;
    }
}
