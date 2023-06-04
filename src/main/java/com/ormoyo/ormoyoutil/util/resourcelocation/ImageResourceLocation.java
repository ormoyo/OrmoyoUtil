package com.ormoyo.ormoyoutil.util.resourcelocation;

import net.minecraft.util.ResourceLocation;

public class ImageResourceLocation extends ResourceLocation
{
    protected final int u;
    protected final int v;
    protected final int width;
    protected final int height;

    public ImageResourceLocation(String resourceName, int u, int v, int width, int height)
    {
        super(resourceName);
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
    }

    public ImageResourceLocation(String resourceDomainIn, String resourcePathIn, int u, int v, int width, int height)
    {
        super(resourceDomainIn, resourcePathIn);
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
    }

    public int getU()
    {
        return this.u;
    }

    public int getV()
    {
        return this.v;
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }
}
