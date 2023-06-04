package com.ormoyo.ormoyoutil.client.button;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ormoyo.ormoyoutil.client.RenderHelper;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;

public class BetterButton extends Button
{
    public double xD;
    public double yD;
    public double widthD;
    public double heightD;
    public int textureOffsetX;
    public int textureOffsetY;
    public double textScale;
    public boolean drawBorder;

    public BetterButton(int x, int y, int width, int height, ITextComponent title, IPressable pressedAction)
    {
        super(x, y, width, height, title, pressedAction);
    }

    @Override
    public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft mc = Minecraft.getInstance();

        this.xD = this.x == (int) this.xD ? this.xD : this.x;
        this.yD = this.y == (int) this.yD ? this.yD : this.y;

        this.widthD = this.width == (int) this.widthD ? this.widthD : this.width;
        this.heightD = this.height == (int) this.heightD ? this.heightD : this.height;

        if (!this.visible)
            return;

        this.isHovered = this.isMouseOver();
        int i = this.getYImage(this.isHovered());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        mc.getTextureManager().bindTexture(Button.WIDGETS_LOCATION);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();

        IRenderTypeBuffer.getImpl(bb).finish();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        if (this.drawBorder)
        {
            RenderHelper.drawTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD, this.yD, 0, 46 + i * 20, 200, 1, this.widthD - 1, 1, 1);
            RenderHelper.drawTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD, this.yD + 1, 0, 46 + i * 20 + 1, 2, 15, 2, this.heightD - 4, 1);
            RenderHelper.drawTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD, this.yD + this.heightD - 3, 0, 46 + i * 20 + 17, 2, 2, 1);
            RenderHelper.drawTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD + 2, this.yD + 1, 2, 46 + i * 20 + 1, 196, 1, this.widthD - 4, 1, 1);
            RenderHelper.drawTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD + this.widthD - 1, this.yD, 199, 46 + i * 20 + 1, 1, 18, 1, this.heightD - 1, 1);
            RenderHelper.drawTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 1, 1, 1, 1);
            RenderHelper.drawTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD, this.yD + this.heightD - 1, 0, 46 + i * 20 + 19, 200, 1, this.widthD, 1, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD + 2, this.yD + 2, 2, 46 + i * 20 + 2, 196, 15, this.widthD - 4, this.heightD - 5, this.textureOffsetX, this.textureOffsetY, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 2, 1, 17, 1, this.heightD - 2, this.textureOffsetX, this.textureOffsetY, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD + 2, this.yD + this.heightD - 3, 2, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD + 2 + (this.widthD - 4) / 2, this.yD + this.heightD - 3, 100, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
        }
        else
        {
            RenderHelper.drawSeamlessTexturedRectToBuffer(bb, WIDGETS_LOCATION, this.xD, this.yD, 2, 46 + i * 20 + 2, 196, 15, this.widthD, this.heightD, this.textureOffsetX, this.textureOffsetY, 1);
        }
        tess.draw();

        int j = getFGColor();
        this.drawCenteredString(matrixStack, mc.fontRenderer, this.toString(), (float) (this.xD + this.widthD / 2), (float) (this.yD + this.heightD / 2), (float) this.textScale, j | MathHelper.ceil(this.alpha * 255.0F) << 24);
    }

    public void drawButtonToBatch(MatrixStack matrixStack, BufferBuilder batchBuilder, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft mc = Minecraft.getInstance();

        this.xD = this.x == (int) this.xD ? this.xD : this.x;
        this.yD = this.y == (int) this.yD ? this.yD : this.y;
        this.widthD = this.width == (int) this.widthD ? this.widthD : this.width;
        this.heightD = this.height == (int) this.heightD ? this.heightD : this.height;

        if (!this.visible)
            return;

        this.isHovered = this.isMouseOver();
        int i = this.getYImage(this.isHovered());

        if (this.drawBorder)
        {
            RenderHelper.drawTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD, this.yD, 0, 46 + i * 20, 200, 1, this.widthD - 1, 1, 1);
            RenderHelper.drawTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD, this.yD + 1, 0, 46 + i * 20 + 1, 2, 15, 2, this.heightD - 4, 1);
            RenderHelper.drawTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD, this.yD + this.heightD - 3, 0, 46 + i * 20 + 17, 2, 2, 1);
            RenderHelper.drawTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD + 2, this.yD + 1, 2, 46 + i * 20 + 1, 196, 1, this.widthD - 4, 1, 1);
            RenderHelper.drawTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD + this.widthD - 1, this.yD, 199, 46 + i * 20 + 1, 1, 18, 1, this.heightD - 1, 1);
            RenderHelper.drawTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 1, 1, 1, 1);
            RenderHelper.drawTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD, this.yD + this.heightD - 1, 0, 46 + i * 20 + 19, 200, 1, this.widthD, 1, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD + 2, this.yD + 2, 2, 46 + i * 20 + 2, 196, 15, this.widthD - 4, this.heightD - 5, this.textureOffsetX, this.textureOffsetY, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD + this.widthD - 2, this.yD + 1, 198, 46 + i * 20 + 2, 1, 17, 1, this.heightD - 2, this.textureOffsetX, this.textureOffsetY, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD + 2, this.yD + this.heightD - 3, 2, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
            RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD + 2 + (this.widthD - 4) / 2, this.yD + this.heightD - 3, 100, 46 + i * 20 + 17, 98, 2, (this.widthD - 4) / 2, 2, this.textureOffsetX, this.textureOffsetY, 1);
        }
        else
        {
            RenderHelper.drawSeamlessTexturedRectToBuffer(batchBuilder, WIDGETS_LOCATION, this.xD, this.yD, 2, 46 + i * 20 + 2, 196, 15, this.widthD, this.heightD, this.textureOffsetX, this.textureOffsetY, 1);
        }

        int j = this.getFGColor();
        this.drawCenteredString(matrixStack, mc.fontRenderer, this.getMessage().getString(), (float) (this.xD + this.widthD / 2), (float) (this.yD + this.heightD / 2), (float) this.textScale, j | MathHelper.ceil(this.alpha * 255.0F) << 24);
    }

    public boolean isMouseOver()
    {
        MouseHelper mouseHelper = Minecraft.getInstance().mouseHelper;
        MainWindow window = Minecraft.getInstance().getMainWindow();

        double i1 = window.getFramebufferWidth();
        double j1 = window.getFramebufferHeight();
        double mX = mouseHelper.getMouseX() * i1 / window.getGuiScaleFactor();
        double mY = j1 - mouseHelper.getMouseY() * j1 / window.getGuiScaleFactor() - 0.25;

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

    protected void drawCenteredString(MatrixStack matrixStack, FontRenderer fontRendererIn, String text, float x, float y, float scale, int color)
    {
        matrixStack.push();
        matrixStack.scale(scale, scale, 1);

        fontRendererIn.drawStringWithShadow(matrixStack, text, (x - fontRendererIn.getStringWidth(text) / 2f * scale) / scale, (y - fontRendererIn.FONT_HEIGHT / 2f * scale) / scale, color);

        matrixStack.pop();
    }
}
