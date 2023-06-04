package com.ormoyo.ormoyoutil.client;

import com.ormoyo.ormoyoutil.client.button.GuiBetterButton;
import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import com.ormoyo.ormoyoutil.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@SideOnly(Side.CLIENT)
public class GuiScrollbar extends GuiBetterButton
{
    private static final ResourceLocation TABS_SCROLLBAR = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static final Color DEFAULT_COLOR = new Color(139, 139, 139);
    public Color bgColor;
    public double blockHeight;
    public boolean isScrolling;
    private double scroll;

    public GuiScrollbar(int id, double x, double y)
    {
        this(id, x, y, DEFAULT_COLOR);
    }

    public GuiScrollbar(int id, double x, double y, double width, double height)
    {
        this(id, x, y, width, height, DEFAULT_COLOR);
    }

    public GuiScrollbar(int id, double x, double y, Color bgColor)
    {
        this(id, x, y, 12, 112, bgColor);
    }

    public GuiScrollbar(int id, double x, double y, double width, double height, Color bgColor)
    {
        this(id, x, y, width, height, bgColor, -1);
    }

    public GuiScrollbar(int id, double x, double y, double blockHeight)
    {
        this(id, x, y, DEFAULT_COLOR, blockHeight);
    }

    public GuiScrollbar(int id, double x, double y, double width, double height, double blockHeight)
    {
        this(id, x, y, width, height, DEFAULT_COLOR, blockHeight);
    }

    public GuiScrollbar(int id, double x, double y, Color bgColor, double blockHeight)
    {
        this(id, x, y, 12, 112, bgColor, blockHeight);
    }

    public GuiScrollbar(int id, double x, double y, double width, double height, Color bgColor, double blockHeight)
    {
        super(id, x, y, width, height, "");
        this.bgColor = bgColor;
        this.blockHeight = blockHeight;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        if (!this.visible)
        {
            return;
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.hovered = this.isMouseOver();
        double height = this.blockHeight < 0 ? this.widthD * 1.5 : Math.min(this.blockHeight, this.heightD);
        boolean flag = Mouse.isButtonDown(0);
        if (this.hovered && flag)
        {
            this.isScrolling = true;
        }
        if (!flag)
        {
            this.isScrolling = false;
        }
        if (this.isScrolling)
        {
            this.scroll = ((this.getMouseY() - this.yD) - height * 0.5) / (this.heightD - height);
            this.scroll = MathHelper.clamp(this.scroll, 0, 1);
        }

        double offset = Utils.lerp(0, this.heightD - height, this.scroll);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderHelper.drawTexturedRect(RenderHelper.WHITE, this.xD, this.yD, 0, 0, 16, 16, this.widthD, this.heightD, 1, this.bgColor);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();

        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        RenderHelper.drawTexturedRectToBuffer(bb, TABS_SCROLLBAR, this.xD, this.yD + offset, 232, 0, 11, 1, this.widthD - 1, 1, 1);
        RenderHelper.drawTexturedRectToBuffer(bb, TABS_SCROLLBAR, this.xD + this.widthD - 1, this.yD + offset, 243, 0, 1, 1, 1, 1, 1);
        RenderHelper.drawTexturedRectToBuffer(bb, TABS_SCROLLBAR, this.xD, this.yD + offset + 1, 232, 1, 2, 13, 2, height - 2, 1);
        RenderHelper.drawSeamlessTexturedRectToBuffer(bb, TABS_SCROLLBAR, this.xD + 2, this.yD + offset + 1, 234, 1, 8, 12, this.width - 4, height - 2, 1);
        RenderHelper.drawTexturedRectToBuffer(bb, TABS_SCROLLBAR, this.xD + this.widthD - 2, this.yD + offset + 1, 242, 1, 2, 13, 2, height - 2, 1);
        RenderHelper.drawTexturedRectToBuffer(bb, TABS_SCROLLBAR, this.xD + 1, this.yD + offset + height - 1, 233, 14, 11, 1, this.widthD - 1, 1, 1);
        RenderHelper.drawTexturedRectToBuffer(bb, TABS_SCROLLBAR, this.xD, this.yD + offset + height - 1, 232, 14, 1, 1, 1, 1, 1);

        tess.draw();
    }

    @Override
    public boolean isMouseOver()
    {
        ScaledResolution scaledresolution = Utils.getResolution();
        double i1 = scaledresolution.getScaledWidth_double();
        double j1 = scaledresolution.getScaledHeight_double();
        double mX = (double) Mouse.getX() * i1 / Minecraft.getMinecraft().displayWidth;
        double mY = j1 - (double) Mouse.getY() * j1 / Minecraft.getMinecraft().displayHeight - 0.25;
        return mX >= this.x && mY >= this.y && mX < this.x + this.widthD && mY < this.y + this.heightD;
    }

    private double getMouseY()
    {
        ScaledResolution scaledresolution = Utils.getResolution();
        double j1 = scaledresolution.getScaledHeight_double();
        return j1 - (double) Mouse.getY() * j1 / Minecraft.getMinecraft().displayHeight - 0.25;
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn)
    {
    }

    public void setScroll(double scroll)
    {
        this.scroll = MathHelper.clamp(scroll, 0, 1);
    }

    public double getScroll()
    {
        return this.scroll = MathHelper.clamp(this.scroll, 0, 1);
    }
}
