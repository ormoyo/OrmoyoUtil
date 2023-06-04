package com.ormoyo.ormoyoutil.client.model.obj;

import com.google.common.collect.Lists;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.Collections;
import java.util.List;

public class Shape implements Cloneable
{
    public final String name;
    private final OBJModel model;
    private Shape parent;
    private final List<Face> faceList = Lists.newArrayList();
    private final List<Vertex> vertexList = Lists.newArrayList();
    private final List<TextureCoords> textureCoordsList = Lists.newArrayList();
    private final List<Normal> normalList = Lists.newArrayList();
    private final List<Shape> childList = Lists.newArrayList();
    private int displayList;
    private boolean compiled;
    public boolean isHidden;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    public float offsetX;
    public float offsetY;
    public float offsetZ;


    public Shape(OBJModel model, String name)
    {
        this.model = model;
        this.name = name;
    }

    public Shape(OBJModel model, Shape parent, String name)
    {
        this.model = model;
        this.parent = parent;
        this.name = name;
    }

    public Matrix3f rotationMatrix(float angle, float x, float y, float z)
    {
        angle *= (float) Math.PI / 180.0F;
        Vector3f axis = new Vector3f(x, y, z);
        axis.normalise();
        float s = (float) Math.sin(angle);
        float c = (float) Math.cos(angle);
        float oc = 1.0f - c;

        Matrix3f mat = new Matrix3f();
        mat.m00 = oc * axis.x * axis.x + c;
        mat.m01 = oc * axis.x * axis.y - axis.z * s;
        mat.m02 = oc * axis.z * axis.x + axis.y * s;
        mat.m10 = oc * axis.x * axis.y + axis.z * s;
        mat.m11 = oc * axis.y * axis.y + c;
        mat.m12 = oc * axis.y * axis.z - axis.x * s;
        mat.m20 = oc * axis.z * axis.x - axis.y * s;
        mat.m21 = oc * axis.y * axis.z + axis.x * s;
        mat.m22 = oc * axis.z * axis.z + c;
        return mat;
    }

    public void addFace(Face face)
    {
        this.faceList.add(face);
    }

    public void addVertex(Vertex vertex)
    {
        this.vertexList.add(vertex);
        vertex.register(this.model);
    }

    public void addTexCoords(TextureCoords textureCoords)
    {
        this.textureCoordsList.add(textureCoords);
        textureCoords.register(this.model);
    }

    public void addNormal(Normal normal)
    {
        this.normalList.add(normal);
        normal.register(this.model);
    }

    public void addChildShape(Shape shape)
    {
        if (shape == this.parent)
        {
            OrmoyoUtil.LOGGER.error("Cannot make parent shape to child shape");
            return;
        }
        if (this == shape)
        {
            OrmoyoUtil.LOGGER.error("Cannot make shape it's own child");
            return;
        }
        if (this.childList.contains(shape))
        {
            OrmoyoUtil.LOGGER.error("Shape already contains child shape");
            return;
        }
        this.childList.add(shape);
        shape.parent = this;
    }

    public void translate(Vector3f translationVector)
    {
        for (Vertex vertex : this.vertexList)
        {
            Vector3f.add(vertex.getPosition(), translationVector, vertex.getPosition());
        }
    }

    public void scale(Vector3f scaleVector)
    {
        for (Vertex vertex : this.vertexList)
        {
            vertex.getPosition().x *= scaleVector.x;
            vertex.getPosition().y *= scaleVector.y;
            vertex.getPosition().z *= scaleVector.z;
        }
    }

    public void rotate(float angle, float x, float y, float z)
    {
        Matrix3f rotationMatrix = this.rotationMatrix(angle, x, y, z);
        for (Vertex vertex : this.vertexList)
        {
            Matrix3f.transform(rotationMatrix, vertex.getPosition(), vertex.getPosition());
        }
        for (Normal normal : this.normalList)
        {
            Matrix3f.transform(rotationMatrix, normal.getVector(), normal.getVector());
        }
    }

    public void render(float scale)
    {
        if (!this.isHidden)
        {
            if (!this.compiled)
            {
                this.compileDisplayList(scale);
            }

            GlStateManager.translate(this.offsetX, this.offsetY, this.offsetZ);

            if (this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F)
            {
                if (this.rotationPointX == 0.0F && this.rotationPointY == 0.0F && this.rotationPointZ == 0.0F)
                {
                    GlStateManager.callList(this.displayList);

                    if (!this.childList.isEmpty())
                    {
                        for (int k = 0; k < this.childList.size(); ++k)
                        {
                            this.childList.get(k).render(scale);
                        }
                    }
                }
                else
                {
                    GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
                    GlStateManager.callList(this.displayList);

                    if (this.childList != null)
                    {
                        for (int j = 0; j < this.childList.size(); ++j)
                        {
                            this.childList.get(j).render(scale);
                        }
                    }

                    GlStateManager.translate(-this.rotationPointX * scale, -this.rotationPointY * scale, -this.rotationPointZ * scale);
                }
            }
            else
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);

                if (this.rotateAngleZ != 0.0F)
                {
                    GlStateManager.rotate(this.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
                }

                if (this.rotateAngleY != 0.0F)
                {
                    GlStateManager.rotate(this.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                }

                if (this.rotateAngleX != 0.0F)
                {
                    GlStateManager.rotate(this.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
                }

                GlStateManager.callList(this.displayList);

                if (this.childList != null)
                {
                    for (int i = 0; i < this.childList.size(); ++i)
                    {
                        this.childList.get(i).render(scale);
                    }
                }

                GlStateManager.popMatrix();
            }
        }
    }

    private void compileDisplayList(float scale)
    {
        this.displayList = GLAllocation.generateDisplayLists(1);
        GlStateManager.glNewList(this.displayList, GL11.GL_COMPILE);
        ResourceLocation prevTexture = null;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        RenderHelper.setupOpacity();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0);
        for (Face face : this.faceList)
        {
            if (face.getMaterial().getTexture() != null && !face.getMaterial().getTexture().equals(prevTexture))
            {
                Minecraft.getMinecraft().getTextureManager().bindTexture(face.getMaterial().getTexture());
                prevTexture = face.getMaterial().getTexture();
            }
            else if (face.getMaterial().getTexture() != prevTexture)
            {
                Minecraft.getMinecraft().getTextureManager().bindTexture(face.getMaterial().getTexture());
            }
            bb.begin(face.getType().mode, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
            for (int i = 0; i < face.getVertices().size(); i++)
            {
                Vertex vertex = face.getVertices().get(i);
                TextureCoords coords = face.getTextureCoords().get(i);
                Normal normal = face.getNormals().get(i);
                bb.pos(vertex.getPosition().x * scale, vertex.getPosition().y * scale, vertex.getPosition().z * scale).tex(coords.getCoords().x, coords.getCoords().y).color(face.getMaterial().getColor().x, face.getMaterial().getColor().y, face.getMaterial().getColor().z, face.getMaterial().getColor().w).normal(normal.getVector().x, normal.getVector().y, normal.getVector().z).endVertex();
            }
            tess.draw();
        }
        GlStateManager.disableBlend();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.glEndList();
        this.compiled = true;
    }

    public String getName()
    {
        return this.name;
    }

    public List<Face> getFaces()
    {
        return Collections.unmodifiableList(this.faceList);
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

    public Shape getParent()
    {
        return this.parent;
    }

    public OBJModel getModel()
    {
        return this.model;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("o ").append(this.name).append(System.lineSeparator());
        for (Vertex vertex : this.vertexList)
        {
            builder.append(vertex.toString()).append(System.lineSeparator());
        }
        for (TextureCoords textureCoords : this.textureCoordsList)
        {
            builder.append(textureCoords.toString()).append(System.lineSeparator());
        }
        for (Normal normal : this.normalList)
        {
            builder.append(normal.toString()).append(System.lineSeparator());
        }
        for (Face face : this.faceList)
        {
            builder.append(face.toString()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    public Shape clone(OBJModel model)
    {
        Shape shape = null;
        try
        {
            shape = (Shape) this.clone();
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        return shape;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        Shape shape = (Shape) super.clone();
        return shape;
    }
}
