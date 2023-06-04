package com.ormoyo.ormoyoutil.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

@SideOnly(Side.CLIENT)
public interface ITypeColorRenderer<T> extends ITypeRenderer<T>, IColorRenderer
{
    void render(T type, double x, double y, float partialTicks, double scale, int color);

    /**
     * <strong> NOTICE </strong> You cannot use this for rendering multiple textures at once
     */
    default void renderToBatch(BufferBuilder batchBuilder, T type, double x, double y, float partialTicks, double scale, int color)
    {
        this.render(type, x, y, partialTicks, scale, color);
    }

    default void render(T type, double x, double y, float partialTicks, double scale, Color color)
    {
        this.render(type, x, y, partialTicks, scale, color.getRGB());
    }

    default void renderToBatch(BufferBuilder batchBuilder, T type, double x, double y, float partialTicks, double scale, Color color)
    {
        this.renderToBatch(batchBuilder, type, x, y, partialTicks, scale, color.getRGB());
    }

    @Override
    default void render(double x, double y, float partialTicks, double scale, int color)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void render(double x, double y, float partialTicks, double scale)
    {
        IColorRenderer.super.render(x, y, partialTicks, scale);
    }
}
