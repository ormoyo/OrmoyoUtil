package com.ormoyo.ormoyoutil.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

@SideOnly(Side.CLIENT)
public interface IColorRenderer extends IRenderer
{
    void render(double x, double y, float partialTicks, double scale, int color);

    /**
     * <strong> NOTICE </strong> You cannot use this for rendering multiple textures at once
     */
    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, float partialTicks, double scale, int color)
    {
        this.render(x, y, partialTicks, scale, color);
    }

    default void render(double x, double y, float partialTicks, double scale, Color color)
    {
        this.render(x, y, partialTicks, scale, color.getRGB());
    }

    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, float partialTicks, double scale, Color color)
    {
        this.renderToBatch(batchBuilder, x, y, partialTicks, scale, color.getRGB());
    }

    @Override
    default void render(double x, double y, float partialTicks, double scale)
    {
        this.render(x, y, partialTicks, scale, -1);
    }

    @Override
    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, float partialTicks, double scale)
    {
        this.renderToBatch(batchBuilder, x, y, partialTicks, scale, -1);
    }
}
