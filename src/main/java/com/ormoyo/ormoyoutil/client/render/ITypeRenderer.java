package com.ormoyo.ormoyoutil.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface ITypeRenderer<T> extends IRenderer
{
    void render(T type, double x, double y, float partialTicks, double scale);

    /**
     * <strong> NOTICE </strong> You cannot use this for rendering multiple textures at once
     */
    default void renderToBatch(BufferBuilder batchBuilder, T type, double x, double y, float partialTicks, double scale)
    {
        this.render(type, x, y, partialTicks, scale);
    }

    default void render(double x, double y, float partialTicks, double scale)
    {
        throw new UnsupportedOperationException();
    }
}
