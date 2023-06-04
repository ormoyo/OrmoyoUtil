package com.ormoyo.ormoyoutil.client.model.obj;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

public class Face
{
    private final Shape parentShape;
    private final Material material;
    private final PolygonType type;
    private final List<Vertex> vertexList = Lists.newArrayList();
    private final List<TextureCoords> textureCoordsList = Lists.newArrayList();
    private final List<Normal> normalList = Lists.newArrayList();

    public Face(Shape shape)
    {
        this(shape, PolygonType.POLYGON);
    }

    public Face(Shape shape, PolygonType type)
    {
        this(shape, type, Material.DEFAULT_MATERIAL);
    }

    public Face(Shape shape, PolygonType type, Material material)
    {
        this.parentShape = shape;
        this.type = type;
        this.material = material;
    }

    public void append(Vertex vertex, TextureCoords textureCoords, Normal normal)
    {
        this.vertexList.add(vertex);
        this.parentShape.addVertex(vertex);
        this.textureCoordsList.add(textureCoords);
        this.parentShape.addTexCoords(textureCoords);
        this.normalList.add(normal);
        this.parentShape.addNormal(normal);

    }

    public Face appendWithoutParent(Vertex vertex, TextureCoords textureCoords, Normal normal)
    {
        this.vertexList.add(vertex);
        this.textureCoordsList.add(textureCoords);
        this.normalList.add(normal);
        return this;
    }

    public List<Vertex> getVertices()
    {
        return Collections.unmodifiableList(this.vertexList);
    }

    public List<TextureCoords> getTextureCoords()
    {
        return Collections.unmodifiableList(this.textureCoordsList);
    }

    public List<Normal> getNormals()
    {
        return Collections.unmodifiableList(this.normalList);
    }

    public PolygonType getType()
    {
        return this.type;
    }

    public Material getMaterial()
    {
        return this.material;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("f");
        for (int i = 0; i < this.vertexList.size(); i++)
        {
            sb.append(" ").append(this.vertexList.get(i).getIndex()).append("/").append(this.textureCoordsList.get(i).getIndex()).append("/").append(this.normalList.get(i).getIndex());
        }
        return sb.toString();
    }
}
