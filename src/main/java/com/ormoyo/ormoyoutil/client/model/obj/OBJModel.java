package com.ormoyo.ormoyoutil.client.model.obj;

import com.google.common.collect.Lists;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.Collections;
import java.util.List;

public class OBJModel
{
    private static final ResourceLocation WHITE = new ResourceLocation(OrmoyoUtil.MODID, "textures/entity/white.png");
    private final List<Shape> shapeList = Lists.newArrayList();
    private final List<Material> materialList = Lists.newArrayList();
    private int vertexIndex = 1;
    private int textureIndex = 1;
    private int normalIndex = 1;

    protected void addShape(Shape shape)
    {
        this.shapeList.add(shape);
    }

    protected void addMaterial(Material material)
    {
        if (this.materialList.contains(material))
        {
            return;
        }
        Material mat = material;
        if (material.getColor() == null)
        {
            mat = new Material(material.getName(), material.getTexture(), 1, 1, 1);
        }
        if (material.getTexture() == null)
        {
            mat = new Material(material.getName(), WHITE, material.getColor().x, material.getColor().y, material.getColor().z, material.getColor().w);
        }
        this.materialList.add(mat);
    }

    protected int getVertexIndex()
    {
        return this.vertexIndex++;
    }

    protected int getUVIndex()
    {
        return this.textureIndex++;
    }

    protected int getNormalIndex()
    {
        return this.normalIndex++;
    }

    public List<Shape> getShapes()
    {
        return Collections.unmodifiableList(this.shapeList);
    }

    public List<Material> getMaterials()
    {
        return Collections.unmodifiableList(this.materialList);
    }

    public Shape getShapeByName(String name)
    {
        for (Shape shape : this.shapeList)
        {
            if (shape.getName().equals(name))
            {
                return shape;
            }
        }
        return null;
    }

    public Material getMaterialByName(String name)
    {
        for (Material material : this.materialList)
        {
            if (material.getName().equals(name))
            {
                return material;
            }
        }
        return null;
    }

    public void render(float scale)
    {
        for (Shape shape : this.shapeList)
        {
            shape.render(scale);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (Shape shape : this.shapeList)
        {
            for (String l : shape.toString().split(System.lineSeparator()))
            {
                builder.append(l).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private static IForgeRegistry<OBJModelEntry> OBJ_MODEL_REGISTRY;

    public static IForgeRegistry<OBJModelEntry> getObjModelRegistery()
    {
        return OBJ_MODEL_REGISTRY;
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID)
    private static class EventHandler
    {
        @SubscribeEvent
        public static void onNewRegistry(RegistryEvent.NewRegistry event)
        {
            OBJ_MODEL_REGISTRY = new RegistryBuilder<OBJModelEntry>().setName(new ResourceLocation(OrmoyoUtil.MODID, "obj_model")).setType(OBJModelEntry.class).setIDRange(0, 2048).create();
        }
    }
}
