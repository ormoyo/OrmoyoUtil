package com.ormoyo.ormoyoutil.client.model.obj;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.vector.Vector4f;

public class Material
{
    public static Material DEFAULT_MATERIAL = new Material("default", 1, 1, 1);
    private static final ResourceLocation WHITE = new ResourceLocation(OrmoyoUtil.MODID, "textures/white.png");
    private final String name;
    private final Vector4f color;
    private final ResourceLocation texture;

    public Material(String name, ResourceLocation texture)
    {
        this.name = name;
        this.color = new Vector4f(1, 1, 1, 1);
        this.texture = texture;
    }

    public Material(String name, ResourceLocation texture, float r, float g, float b)
    {
        this.name = name;
        this.color = new Vector4f(r, g, b, 1);
        this.texture = texture;
    }

    public Material(String name, ResourceLocation texture, float r, float g, float b, float a)
    {
        this.name = name;
        this.color = new Vector4f(r, g, b, a);
        this.texture = texture;
    }

    public Material(String name, float r, float g, float b)
    {
        this.name = name;
        this.color = new Vector4f(r, g, b, 1);
        this.texture = WHITE;
    }

    public Material(String name, float r, float g, float b, float a)
    {
        this.name = name;
        this.color = new Vector4f(r, g, b, a);
        this.texture = WHITE;
    }

    public String getName()
    {
        return name;
    }

    public Vector4f getColor()
    {
        return this.color;
    }

    public ResourceLocation getTexture()
    {
        return this.texture;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.name).append(System.lineSeparator());
        sb.append("Kd").append(" ").append(this.color.x).append(" ").append(this.color.y).append(" ").append(this.color.z).append(System.lineSeparator());
        sb.append("d").append(" ").append(this.color.w);
        if (this.texture != null)
        {
            sb.append(System.lineSeparator());
            sb.append("map_Kd").append(" ").append(this.texture);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (obj instanceof Material)
        {
            Material material = (Material) obj;
            return this.name == material.name;
        }
        return false;
    }
}
