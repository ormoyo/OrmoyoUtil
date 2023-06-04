package com.ormoyo.ormoyoutil.client.model.obj;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public enum PolygonType
{
    TRINGLE(GL11.GL_TRIANGLES),
    QUAD(GL11.GL_QUADS),
    POLYGON(GL11.GL_POLYGON);

    public final int mode;

    PolygonType(int mode)
    {
        this.mode = mode;
    }
}
