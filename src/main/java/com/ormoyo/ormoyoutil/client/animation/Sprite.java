package com.ormoyo.ormoyoutil.client.animation;

import com.ormoyo.ormoyoutil.client.render.IColorStretchRenderer;
import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Sprite implements IColorStretchRenderer
{
    private final ResourceLocation texture;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final float pivotX;
    private final float pivotY;

    public Sprite(ResourceLocation texture, int x, int y, int width, int height)
    {
        this(texture, x, y, width, height, 0.5f, 0.5f);
    }

    public Sprite(ResourceLocation texture, int x, int y, int width, int height, float pivotX, float pivotY)
    {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    public ResourceLocation getTexture()
    {
        return this.texture;
    }

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public float getPivotX()
    {
        return this.pivotX;
    }

    public float getPivotY()
    {
        return this.pivotY;
    }

    @Override
    public void render(double x, double y, float partialTicks, double scale, int color)
    {
        RenderHelper.drawTexturedRect(this.texture, x - this.width * this.pivotX * scale, y - this.height * this.pivotY * scale, this.x, this.y, this.width, this.height, scale, color);
    }

    @Override
    public void render(double x, double y, double width, double height, float partialTicks, double scale, int color)
    {
        RenderHelper.drawTexturedRect(this.texture, x - width * this.pivotX * scale, y - height * this.pivotY * scale, this.x, this.y, this.width, this.height, width, height, scale, color);
    }

    @Override
    public void renderToBatch(BufferBuilder batchBuilder, double x, double y, float partialTicks, double scale, int color)
    {
        RenderHelper.drawTexturedRectToBuffer(batchBuilder, this.texture, x - this.width * this.pivotX * scale, y - this.height * this.pivotY * scale, this.x, this.y, this.width, this.height, scale, color);
    }

    @Override
    public void renderToBatch(BufferBuilder batchBuilder, double x, double y, double width, double height, float partialTicks, double scale, int color)
    {
        RenderHelper.drawTexturedRectToBuffer(batchBuilder, this.texture, x - width * this.pivotX * scale, y - height * this.pivotY * scale, this.x, this.y, this.width, this.height, width, height, scale, color);
    }
}
