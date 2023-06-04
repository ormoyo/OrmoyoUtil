package com.ormoyo.ormoyoutil.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

@SideOnly(Side.CLIENT)
public interface IColorStretchRenderer extends IColorRenderer, IStretchRenderer
{
    void render(double x, double y, double width, double height, float partialTicks, double scale, int color);

    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, double width, double height, float partialTicks, double scale, int color)
    {
        this.render(x, y, width, height, partialTicks, scale, color);
    }

    default void render(double x, double y, double width, double height, float partialTicks, double scale, Color color)
    {
        this.render(x, y, width, height, partialTicks, scale, color.getRGB());
    }

    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, double width, double height, float partialTicks, double scale, Color color)
    {
        this.renderToBatch(batchBuilder, x, y, width, height, partialTicks, scale, color.getRGB());
    }

    @Override
    default void render(double x, double y, double width, double height, float partialTicks, double scale)
    {
        this.render(x, y, width, height, partialTicks, scale, -1);
    }

    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, double width, double height, float partialTicks, double scale)
    {
        this.renderToBatch(batchBuilder, x, y, width, height, partialTicks, scale, -1);
    }
}
