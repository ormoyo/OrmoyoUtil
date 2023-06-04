package com.ormoyo.ormoyoutil.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IStretchRenderer extends IRenderer
{
    void render(double x, double y, double width, double height, float partialTicks, double scale);

    /**
     * <strong> NOTICE </strong> You cannot use this for rendering multiple textures at once
     */
    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, double width, double height, float partialTicks, double scale)
    {
        this.render(x, y, width, height, partialTicks, scale);
    }
}
