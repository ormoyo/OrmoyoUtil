package com.ormoyo.ormoyoutil.client.model.obj;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;

//The registey name of this needs to be the path of the obj file
public class OBJModelEntry extends Impl<OBJModelEntry>
{
    private final boolean removeDuplicateVertices;
    private final boolean hasJsonFile;
    private OBJModel model;

    public OBJModelEntry(ResourceLocation name)
    {
        this(name, true);
    }

    public OBJModelEntry(String name)
    {
        this(name, true);
    }

    public OBJModelEntry(ResourceLocation name, boolean removeDuplicateVertices)
    {
        this(name, removeDuplicateVertices, true);
    }

    public OBJModelEntry(String name, boolean removeDuplicateVertices)
    {
        this(name, removeDuplicateVertices, true);
    }

    public OBJModelEntry(String name, boolean removeDuplicateVertices, boolean hasJsonFile)
    {
        this.removeDuplicateVertices = removeDuplicateVertices;
        this.hasJsonFile = hasJsonFile;
        this.setRegistryName(name);
    }

    public OBJModelEntry(ResourceLocation name, boolean removeDuplicateVertices, boolean hasJsonFile)
    {
        this.removeDuplicateVertices = removeDuplicateVertices;
        this.hasJsonFile = hasJsonFile;
        this.setRegistryName(name);
    }

    public boolean removeDuplicateVertices()
    {
        return this.removeDuplicateVertices;
    }

    public boolean hasJsonFile()
    {
        return this.hasJsonFile;
    }

    //This will return null until Init phase finishes
    public OBJModel getModel()
    {
        return this.model;
    }
}
