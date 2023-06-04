package com.ormoyo.ormoyoutil.client.button;

import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import com.ormoyo.ormoyoutil.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiBetterButton extends GuiButton
{
    public double xD;
    public double yD;
    public double widthD;
    public double heightD;
    public int textureOffsetX;
    public int textureOffsetY;
    public double textScale;
    public boolean drawBorder;

    public GuiBetterButton(int buttonId, double x, double y, double width, double height, String buttonText)
    {
        this(buttonId, x, y, width, height, buttonText, 1);
    }

    public GuiBetterButton(int buttonId, double x, double y, String buttonText)
    {
        this(buttonId, x, y, buttonText, 1);
    }

    public GuiBetterButton(int buttonId, double x, double y, double width, double height, String buttonText, double textScale)
    {
        super(buttonId, (int) x, (int) y, (int) width, (int) height, buttonText);
        this.xD = x;
        this.yD = y;
        this.widthD = width;
        this.heightD = height;
        this.textScale = textScale;
        this.drawBorder = true;
    }

    public GuiBetterButton(int buttonId, double x, double y, String buttonText, double textScale)
    {
        this(buttonId, x, y, Minecraft.getMinecraft().fontRenderer.getStringWidth(buttonText) + 4, 20, buttonText, textScale);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        this.xD = this.x == (int) this.xD ? this.xD : this.x;
        this.yD = this.y == (int) this.yD ? this.yD : this.y;
        this.widthD = this.width == (int) this.widthD ? this.widthD : this.width;
        this.heightD = this.height == (int) this.heightD ? this.heightD : this.height;

        if (this.visible)
        {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = this.isMouseOver();

            int i = this.getHoverState(this.hovered);

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

            mc.getTextureManager().bindTexture(BUTTON_TEXTURES);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();

            bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            if (this.drawBorder)
            {
                RenderHelper.drawTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD, this.yD, 0, 46 + i * 20, 200, 1, this.widthD - 1, 1, 1);
                RenderHelper.drawTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD, this.yD + 1, 0, 46 + i * 20 + 1, 2, 15, 2, this.heightD - 4, 1);
                RenderHelper.drawTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD, this.yD + this.heightD - 3, 0, 46 + i * 20 + 17, 2, 2, 1);
                RenderHelper.drawTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD + 2, this.yD + 1, 2, 46 + i * 20 + 1, 196, 1, this.widthD - 4, 1, 1);
                RenderHelper.drawTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD + this.widthD - 1, this.yD, 199, 46 + i * 20 + 1, 1, 18, 1, this.heightD - 1, 1);
                RenderHelper.drawTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 1, 1, 1, 1);
                RenderHelper.drawTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD, this.yD + this.heightD - 1, 0, 46 + i * 20 + 19, 200, 1, this.widthD, 1, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD + 2, this.yD + 2, 2, 46 + i * 20 + 2, 196, 15, this.widthD - 4, this.heightD - 5, this.textureOffsetX, this.textureOffsetY, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 2, 1, 17, 1, this.heightD - 2, this.textureOffsetX, this.textureOffsetY, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD + 2, this.yD + this.heightD - 3, 2, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD + 2 + (this.widthD - 4) / 2, this.yD + this.heightD - 3, 100, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
            }
            else
            {
                RenderHelper.drawSeamlessTexturedRectToBuffer(bb, BUTTON_TEXTURES, this.xD, this.yD, 2, 46 + i * 20 + 2, 196, 15, this.widthD, this.heightD, this.textureOffsetX, this.textureOffsetY, 1);
            }
            tess.draw();
            this.mouseDragged(mc, mouseX, mouseY);

            int j = 14737632;
            if (this.packedFGColour != 0)
            {
                j = this.packedFGColour;
            }
            else if (!this.enabled)
            {
                j = 10526880;
            }
            else if (this.hovered)
            {
                j = 16777120;
            }
            this.drawCenteredString(mc.fontRenderer, this.displayString, (float) (this.xD + this.widthD / 2), (float) (this.yD + this.heightD / 2), (float) this.textScale, j);
        }
    }

    public void drawButtonToBatch(BufferBuilder batchBuilder, Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        this.xD = this.x == (int) this.xD ? this.xD : this.x;
        this.yD = this.y == (int) this.yD ? this.yD : this.y;
        this.widthD = this.width == (int) this.widthD ? this.widthD : this.width;
        this.heightD = this.height == (int) this.heightD ? this.heightD : this.height;

        if (this.visible)
        {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = this.isMouseOver();

            int i = this.getHoverState(this.hovered);

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

            if (this.drawBorder)
            {
                RenderHelper.drawTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD, this.yD, 0, 46 + i * 20, 200, 1, this.widthD - 1, 1, 1);
                RenderHelper.drawTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD, this.yD + 1, 0, 46 + i * 20 + 1, 2, 15, 2, this.heightD - 4, 1);
                RenderHelper.drawTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD, this.yD + this.heightD - 3, 0, 46 + i * 20 + 17, 2, 2, 1);
                RenderHelper.drawTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD + 2, this.yD + 1, 2, 46 + i * 20 + 1, 196, 1, this.widthD - 4, 1, 1);
                RenderHelper.drawTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD + this.widthD - 1, this.yD, 199, 46 + i * 20 + 1, 1, 18, 1, this.heightD - 1, 1);
                RenderHelper.drawTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 1, 1, 1, 1);
                RenderHelper.drawTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD, this.yD + this.heightD - 1, 0, 46 + i * 20 + 19, 200, 1, this.widthD, 1, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD + 2, this.yD + 2, 2, 46 + i * 20 + 2, 196, 15, this.widthD - 4, this.heightD - 5, this.textureOffsetX, this.textureOffsetY, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 2, 1, 17, 1, this.heightD - 2, this.textureOffsetX, this.textureOffsetY, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD + 2, this.yD + this.heightD - 3, 2, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
                RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD + 2 + (this.widthD - 4) / 2, this.yD + this.heightD - 3, 100, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
            }
            else
            {
                RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, BUTTON_TEXTURES, this.xD, this.yD, 2, 46 + i * 20 + 2, 196, 15, this.widthD, this.heightD, this.textureOffsetX, this.textureOffsetY, 1);
            }

            this.mouseDragged(mc, mouseX, mouseY);
            int j = 14737632;
            if (this.packedFGColour != 0)
            {
                j = this.packedFGColour;
            }
            else if (!this.enabled)
            {
                j = 10526880;
            }
            else if (this.hovered)
            {
                j = 16777120;
            }

            this.drawCenteredString(mc.fontRenderer, this.displayString, (float) (this.xD + this.widthD / 2), (float) (this.yD + this.heightD / 2), (float) this.textScale, j);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        return this.enabled && this.visible && this.isMouseOver();
    }

    @Override
    public boolean isMouseOver()
    {
        ScaledResolution scaledresolution = Utils.getResolution();
        double i1 = scaledresolution.getScaledWidth_double();
        double j1 = scaledresolution.getScaledHeight_double();
        double mX = (double) Mouse.getX() * i1 / Minecraft.getMinecraft().displayWidth;
        double mY = j1 - (double) Mouse.getY() * j1 / Minecraft.getMinecraft().displayHeight - 0.25;
        return mX >= this.xD && mY >= this.yD && mX < this.xD + this.widthD && mY < this.yD + this.heightD;
    }

    public void setPosition(double x, double y)
    {
        this.x = (int) x;
        this.xD = x;
        this.y = (int) y;
        this.yD = y;
    }

    @Override
    public void setWidth(int width)
    {
        super.setWidth(width);
        this.widthD = width;
    }

    public void setWidth(double width)
    {
        this.width = (int) width;
        this.widthD = width;
    }

    public void setHeight(double height)
    {
        this.height = (int) height;
        this.heightD = height;
    }

    protected void drawCenteredString(FontRenderer fontRendererIn, String text, float x, float y, float scale, int color)
    {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1);
        fontRendererIn.drawStringWithShadow(text, (x - fontRendererIn.getStringWidth(text) / 2 * scale) / scale, (y - fontRendererIn.FONT_HEIGHT / 2 * scale) / scale, color);
        GlStateManager.popMatrix();
    }
}
