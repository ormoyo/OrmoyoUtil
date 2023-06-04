package com.ormoyo.ormoyoutil.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRenderer
{
    void render(double x, double y, float partialTicks, double scale);

    default void renderToBatch(BufferBuilder batchBuilder, double x, double y, float partialTicks, double scale)
    {
        this.render(x, y, partialTicks, scale);
    }
}
