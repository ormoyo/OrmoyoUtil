package com.ormoyo.ormoyoutil.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ormoyo.ormoyoutil.client.button.BetterButton;
import com.ormoyo.ormoyoutil.util.vector.Vec2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.*;
import static org.lwjgl.opengl.GL11.GL_QUADS;

public class RenderHelper
{
    private static final Map<ResourceLocation, Vec2i> locationToSize = Maps.newHashMap();
    public static final ResourceLocation WHITE;

    public static Vec2i getTextureSize(ResourceLocation texture)
    {
        Vec2i size = null;

        if (!RenderHelper.locationToSize.containsKey(texture))
        {
            size = loadTexture(texture);
        }

        return size == null ? RenderHelper.locationToSize.getOrDefault(texture, Vec2i.NULL_VECTOR) : size;
    }

    private static Vec2i loadTexture(ResourceLocation location)
    {
        Texture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        if (texture instanceof SimpleTexture)
        {
            try (SimpleTexture.TextureData data = SimpleTexture.TextureData.getTextureData(Minecraft.getInstance().getResourceManager(), location))
            {
                NativeImage image = data.getNativeImage();
                Vec2i size = new Vec2i(image.getWidth(), image.getHeight());

                RenderHelper.locationToSize.put(location, size);
                return size;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        if (texture instanceof DynamicTexture)
        {
            DynamicTexture dynamicTexture = (DynamicTexture) texture;
            NativeImage image = dynamicTexture.getTextureData();

            if (image == null)
                return null;

            Vec2i size = new Vec2i(image.getWidth(), image.getHeight());
            RenderHelper.locationToSize.put(location, size);

            return size;
        }

        return null;
    }

    public static void drawTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int width, int height, double scale)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_TEX);
        RenderHelper.drawTexturedRectToBuffer(buffer, texture, x, y, u, v, width, height, scale);
        tessellator.draw();
    }

    public static void drawTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int width, int height, double scale, Color color)
    {
        RenderHelper.drawTexturedRect(texture, x, y, u, v, width, height, scale, color.getRGB());
    }

    public static void drawTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int width, int height, double scale, int color)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_COLOR_TEX);
        RenderHelper.drawTexturedRectToBuffer(buffer, texture, x, y, u, v, width, height, scale, color);
        tessellator.draw();
    }

    public static void drawTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_TEX);
        RenderHelper.drawTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, scale);
        tessellator.draw();
    }

    public static void drawTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, Color color)
    {
        RenderHelper.drawTexturedRect(texture, x, y, u, v, uWidth, vHeight, width, height, scale, color.getRGB());
    }

    public static void drawTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, int color)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_COLOR_TEX);
        RenderHelper.drawTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, scale, color);
        tessellator.draw();
    }

    public static void drawSeamlessTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_TEX);
        RenderHelper.drawSeamlessTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, scale);
        tessellator.draw();
    }

    public static void drawSeamlessTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, Color color)
    {
        RenderHelper.drawSeamlessTexturedRect(texture, x, y, u, v, uWidth, vHeight, width, height, scale, color.getRGB());
    }

    public static void drawSeamlessTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, int color)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_COLOR_TEX);
        RenderHelper.drawSeamlessTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, scale, color);
        tessellator.draw();
    }

    public static void drawSeamlessTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, int textureOffsetX, int textureOffsetY, double scale)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_TEX);
        RenderHelper.drawSeamlessTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, textureOffsetX, textureOffsetY, scale);
        tessellator.draw();
    }

    public static void drawSeamlessTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, int textureOffsetX, int textureOffsetY, double scale, Color color)
    {
        RenderHelper.drawSeamlessTexturedRect(texture, x, y, u, v, uWidth, vHeight, width, height, textureOffsetX, textureOffsetY, scale, color.getRGB());
    }

    public static void drawSeamlessTexturedRect(ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, int textureOffsetX, int textureOffsetY, double scale, int color)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_COLOR_TEX);
        RenderHelper.drawSeamlessTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, textureOffsetX, textureOffsetY, scale, color);
        tessellator.draw();
    }

    public static void drawWidgetHitBox(Widget widget)
    {
        Minecraft.getInstance().getTextureManager().bindTexture(WHITE);
        setupBlend();

        double x = widget instanceof BetterButton ? ((BetterButton) widget).xD : widget.x;
        double y = widget instanceof BetterButton ? ((BetterButton) widget).yD : widget.y;
        double width = widget instanceof BetterButton ? ((BetterButton) widget).widthD : widget.getWidth();
        double height = widget instanceof BetterButton ? ((BetterButton) widget).heightD : widget.getHeight();

        int opacity = 125;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_COLOR);

        buffer.pos(x + width, y + height, 0).color(0, 255, 0, opacity).endVertex();
        buffer.pos(x + width, y, 0).color(0, 255, 0, opacity).endVertex();
        buffer.pos(x, y, 0).color(0, 255, 0, opacity).endVertex();
        buffer.pos(x, y + height, 0).color(0, 255, 0, opacity).endVertex();

        tessellator.draw();
    }

    public static void drawWidgetHitBox(Widget widget, Color color)
    {
        RenderHelper.drawWidgetHitBox(widget, color.getRGB());
    }

    public static void drawWidgetHitBox(Widget widget, int color)
    {
        Minecraft.getInstance().getTextureManager().bindTexture(WHITE);
        setupBlend();

        double x = widget instanceof BetterButton ? ((BetterButton) widget).xD : widget.x;
        double y = widget instanceof BetterButton ? ((BetterButton) widget).yD : widget.y;
        double width = widget instanceof BetterButton ? ((BetterButton) widget).widthD : widget.getWidth();
        double height = widget instanceof BetterButton ? ((BetterButton) widget).heightD : widget.getHeight();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        int opacity = Math.max(125, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_COLOR);

        buffer.pos(x + width, y + height, 0).color(red, green, blue, opacity).endVertex();
        buffer.pos(x + width, y, 0).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y, 0).color(red, green, blue, opacity).endVertex();
        buffer.pos(x, y + height, 0).color(red, green, blue, opacity).endVertex();

        tessellator.draw();
    }

    public static void drawTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int width, int height, double scale)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + width) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + height) / imageHeight;

        buffer.pos(x + scale * width, y + scale * height, 0).tex(maxU, maxV).endVertex();
        buffer.pos(x + scale * width, y, 0).tex(maxU, minV).endVertex();
        buffer.pos(x, y, 0).tex(minU, minV).endVertex();
        buffer.pos(x, y + scale * height, 0).tex(minU, maxV).endVertex();
    }

    public static void drawTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int width, int height, double scale, Color color)
    {
        RenderHelper.drawTexturedRectToBuffer(buffer, texture, x, y, u, v, width, height, scale, color.getRGB());
    }

    public static void drawTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int width, int height, double scale, int color)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + width) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + height) / imageHeight;

        buffer.pos(x + scale * width, y + scale * height, 0).color(red, green, blue, alpha).tex(maxU, maxV).endVertex();
        buffer.pos(x + scale * width, y, 0).color(red, green, blue, alpha).tex(maxU, minV).endVertex();
        buffer.pos(x, y, 0).color(red, green, blue, alpha).tex(minU, minV).endVertex();
        buffer.pos(x, y + scale * height, 0).color(red, green, blue, alpha).tex(minU, maxV).endVertex();
    }

    public static void drawTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + uWidth) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + vHeight) / imageHeight;

        buffer.pos(x + scale * width, y + scale * height, 0).tex(maxU, maxV).endVertex();
        buffer.pos(x + scale * width, y, 0).tex(maxU, minV).endVertex();
        buffer.pos(x, y, 0).tex(minU, minV).endVertex();
        buffer.pos(x, y + scale * height, 0).tex(minU, maxV).endVertex();
    }

    public static void drawTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, Color color)
    {
        RenderHelper.drawTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, scale, color.getRGB());
    }

    public static void drawTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, int color)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + uWidth) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + vHeight) / imageHeight;

        buffer.pos(x + scale * width, y + scale * height, 0).color(red, green, blue, alpha).tex(maxU, maxV).endVertex();
        buffer.pos(x + scale * width, y, 0).color(red, green, blue, alpha).tex(maxU, minV).endVertex();
        buffer.pos(x, y, 0).color(red, green, blue, alpha).tex(minU, minV).endVertex();
        buffer.pos(x, y + scale * height, 0).color(red, green, blue, alpha).tex(minU, maxV).endVertex();
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + uWidth) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + vHeight) / imageHeight;

        double wRatio = width / uWidth;
        double hRatio = height / vHeight;

        double w = Math.min(width, uWidth);
        double h = Math.min(height, vHeight);

        double xPos = x;
        double yPos = y;

        for (int iy = 0; iy < hRatio; iy++)
        {
            for (int ix = 0; ix < wRatio; ix++)
            {
                double wOff = w;
                double hOff = h;

                float maxUOff = maxU;
                float maxVOff = maxV;

                if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (float) (u + (width % uWidth)) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width;
                        maxUOff = (float) (u + width) / imageWidth;
                    }
                }

                if (iy + 1 >= hRatio)
                {
                    if (height > vHeight)
                    {
                        if (height % vHeight != 0)
                        {
                            hOff = height % vHeight;
                            maxVOff = (float) (v + (height % vHeight)) / imageHeight;
                        }
                    }
                    else
                    {
                        hOff = height;
                        maxVOff = (float) (v + height) / imageHeight;
                    }
                }

                buffer.pos(xPos + scale * wOff, yPos + scale * hOff, 0).tex(maxUOff, maxVOff).endVertex();
                buffer.pos(xPos + scale * wOff, yPos, 0).tex(maxUOff, minV).endVertex();
                buffer.pos(xPos, yPos, 0).tex(minU, minV).endVertex();
                buffer.pos(xPos, yPos + scale * hOff, 0).tex(minU, maxVOff).endVertex();

                xPos += scale * w;
            }

            xPos = x;
            yPos += scale * h;
        }
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, Color color)
    {
        RenderHelper.drawSeamlessTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, scale, color.getRGB());
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale, int color)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + uWidth) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + vHeight) / imageHeight;

        double wRatio = width / uWidth;
        double hRatio = height / vHeight;

        double w = MathHelper.clamp(width, Integer.MIN_VALUE, uWidth);
        double h = MathHelper.clamp(height, Integer.MIN_VALUE, vHeight);

        double xPos = x;
        double yPos = y;

        for (int iy = 0; iy < hRatio; iy++)
        {
            for (int ix = 0; ix < wRatio; ix++)
            {
                double wOff = w;
                double hOff = h;
                float maxUOff = maxU;
                float maxVOff = maxV;

                if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (float) (u + (width % uWidth)) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width;
                        maxUOff = (float) (u + width) / imageWidth;
                    }
                }

                if (iy + 1 >= hRatio)
                {
                    if (height > vHeight)
                    {
                        if (height % vHeight != 0)
                        {
                            hOff = height % vHeight;
                            maxVOff = (float) (v + (height % vHeight)) / imageHeight;
                        }
                    }
                    else
                    {
                        hOff = height;
                        maxVOff = (float) (v + height) / imageHeight;
                    }
                }

                buffer.pos(xPos + scale * wOff, yPos + scale * hOff, 0).color(red, green, blue, alpha).tex(maxUOff, maxVOff).endVertex();
                buffer.pos(xPos + scale * wOff, yPos, 0).color(red, green, blue, alpha).tex(maxUOff, minV).endVertex();
                buffer.pos(xPos, yPos, 0).color(red, green, blue, alpha).tex(minU, minV).endVertex();
                buffer.pos(xPos, yPos + scale * hOff, 0).color(red, green, blue, alpha).tex(minU, maxVOff).endVertex();

                xPos += scale * w;
            }

            xPos = x;
            yPos += scale * h;
        }
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, int textureOffsetX, int textureOffsetY, double scale)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + uWidth) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + vHeight) / imageHeight;

        textureOffsetX %= uWidth;
        textureOffsetY %= vHeight;
        textureOffsetX = (textureOffsetX <= 0 ? textureOffsetX : textureOffsetX - uWidth);
        textureOffsetY = (textureOffsetY <= 0 ? textureOffsetY : textureOffsetY - vHeight);

        double wRatio = width / uWidth + (textureOffsetX < 0 ? 1 : 0);
        double hRatio = height / vHeight + (textureOffsetY < 0 ? 1 : 0);

        double w = Math.min(width, uWidth);
        double h = Math.min(height, vHeight);

        double xPos = x;
        double yPos = y;

        for (int iy = 0; iy < hRatio; iy++)
        {
            double hOff = h;

            float minVOff = minV;
            float maxVOff = maxV;

            if (iy == 0)
            {
                if (textureOffsetY != 0)
                {
                    minVOff = (float) (v + textureOffsetY + vHeight) / imageHeight;
                    hOff = -textureOffsetY;

                    if (yPos + hOff > y + height)
                    {
                        hOff = height;
                        maxVOff = (float) ((v + textureOffsetY + vHeight) + hOff) / imageHeight;
                    }
                }
                else
                {
                    maxVOff = (float) (v + h) / imageHeight;
                }
            }
            else if (iy + 1 >= hRatio)
            {
                if (height > vHeight)
                {
                    if (height % vHeight != 0)
                    {
                        hOff = height % vHeight;
                        maxVOff = (float) (v + hOff) / imageHeight;
                    }
                }
                else
                {
                    hOff = height + textureOffsetY;
                    maxVOff = (float) (v + textureOffsetY + height) / imageHeight;
                }
            }

            if (yPos + hOff > y + height)
            {
                double vy = yPos - y;

                hOff = height - vy;
                maxVOff = (float) (v + hOff) / imageHeight;
            }

            for (int ix = 0; ix < wRatio; ix++)
            {
                double wOff = w;

                float minUOff = minU;
                float maxUOff = maxU;

                if (ix == 0)
                {
                    if (textureOffsetX != 0)
                    {
                        minUOff = (float) (u + textureOffsetX + uWidth) / imageWidth;
                        wOff = -textureOffsetX;
                        if (xPos + wOff > x + width)
                        {
                            wOff = width;
                            maxUOff = (float) ((u + textureOffsetX + uWidth) + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        maxUOff = (float) (u + w) / imageWidth;
                    }
                }
                else if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (float) (u + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width + textureOffsetX;
                        maxUOff = (float) (u + textureOffsetX + width) / imageWidth;
                    }
                }

                if (xPos + wOff > x + width)
                {
                    double ux = xPos - x;
                    wOff = width - ux;
                    maxUOff = (float) (u + wOff) / imageWidth;
                }

                buffer.pos(xPos + scale * wOff, yPos + scale * hOff, 0).tex(maxUOff, maxVOff).endVertex();
                buffer.pos(xPos + scale * wOff, yPos, 0).tex(maxUOff, minVOff).endVertex();
                buffer.pos(xPos, yPos, 0).tex(minUOff, minVOff).endVertex();
                buffer.pos(xPos, yPos + scale * hOff, 0).tex(minUOff, maxVOff).endVertex();

                xPos += scale * wOff;
            }

            xPos = x;
            yPos += scale * hOff;
        }
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, int textureOffsetX, int textureOffsetY, double scale, Color color)
    {
        RenderHelper.drawSeamlessTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, textureOffsetX, textureOffsetY, scale, color.getRGB());
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, int textureOffsetX, int textureOffsetY, double scale, int color)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        float minU = (float) u / imageWidth;
        float maxU = (float) (u + uWidth) / imageWidth;
        float minV = (float) v / imageHeight;
        float maxV = (float) (v + vHeight) / imageHeight;

        textureOffsetX %= uWidth;
        textureOffsetY %= vHeight;
        textureOffsetX = (textureOffsetX <= 0 ? textureOffsetX : textureOffsetX - uWidth);
        textureOffsetY = (textureOffsetY <= 0 ? textureOffsetY : textureOffsetY - vHeight);

        double wRatio = width / uWidth + (textureOffsetX < 0 ? 1 : 0);
        double hRatio = height / vHeight + (textureOffsetY < 0 ? 1 : 0);

        double w = Math.min(width, uWidth);
        double h = Math.min(height, vHeight);

        double xPos = x;
        double yPos = y;

        for (int iy = 0; iy < hRatio; iy++)
        {
            double hOff = h;

            float minVOff = minV;
            float maxVOff = maxV;

            if (iy == 0)
            {
                if (textureOffsetY != 0)
                {
                    minVOff = (float) (v + textureOffsetY + vHeight) / imageHeight;
                    hOff = -textureOffsetY;
                    if (yPos + hOff > y + height)
                    {
                        hOff = height;
                        maxVOff = (float) ((v + textureOffsetY + vHeight) + hOff) / imageHeight;
                    }
                }
                else
                {
                    maxVOff = (float) (v + h) / imageHeight;
                }
            }
            else if (iy + 1 >= hRatio)
            {
                if (height > vHeight)
                {
                    if (height % vHeight != 0)
                    {
                        hOff = height % vHeight;
                        maxVOff = (float) (v + hOff) / imageHeight;
                    }
                }
                else
                {
                    hOff = height + textureOffsetY;
                    maxVOff = (float) (v + textureOffsetY + height) / imageHeight;
                }
            }

            if (yPos + hOff > y + height)
            {
                double vy = yPos - y;

                hOff = height - vy;
                maxVOff = (float) (v + hOff) / imageHeight;
            }

            for (int ix = 0; ix < wRatio; ix++)
            {
                double wOff = w;

                float minUOff = minU;
                float maxUOff = maxU;

                if (ix == 0)
                {
                    if (textureOffsetX != 0)
                    {
                        minUOff = (float) (u + textureOffsetX + uWidth) / imageWidth;
                        wOff = -textureOffsetX;

                        if (xPos + wOff > x + width)
                        {
                            wOff = width;
                            maxUOff = (float) ((u + textureOffsetX + uWidth) + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        maxUOff = (float) (u + w) / imageWidth;
                    }
                }
                else if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (float) (u + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width + textureOffsetX;
                        maxUOff = (float) (u + textureOffsetX + width) / imageWidth;
                    }
                }

                if (xPos + wOff > x + width)
                {
                    double ux = xPos - x;

                    wOff = width - ux;
                    maxUOff = (float) (u + wOff) / imageWidth;
                }

                buffer.pos(xPos + scale * wOff, yPos + scale * hOff, 0).color(red, green, blue, alpha).tex(maxUOff, maxVOff).endVertex();
                buffer.pos(xPos + scale * wOff, yPos, 0).color(red, green, blue, alpha).tex(maxUOff, minVOff).endVertex();
                buffer.pos(xPos, yPos, 0).color(red, green, blue, alpha).tex(minUOff, minVOff).endVertex();
                buffer.pos(xPos, yPos + scale * hOff, 0).color(red, green, blue, alpha).tex(minUOff, maxVOff).endVertex();

                xPos += scale * wOff;
            }

            xPos = x;
            yPos += scale * hOff;
        }
    }

    public static void setupBlend()
    {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static class ReloadListener implements ISelectiveResourceReloadListener
    {
        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
        {
            if (resourcePredicate.test(VanillaResourceType.TEXTURES))
            {
                for (Iterator<Map.Entry<ResourceLocation, Vec2i>> iterator = locationToSize.entrySet().iterator(); iterator.hasNext(); )
                {
                    Map.Entry<ResourceLocation, Vec2i> entry = iterator.next();
                    Texture obj = Minecraft.getInstance().getTextureManager().getTexture(entry.getKey());

                    if (obj == null)
                    {
                        iterator.remove();
                        continue;
                    }

                    loadTexture(entry.getKey());
                }
            }
        }
    }

    static
    {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        try
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            ImageIO.write(image, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());

            NativeImage img = NativeImage.read(is);
            WHITE = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation("white", new DynamicTexture(img));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        IResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager)
        {
            IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) resourceManager;
            reloadableResourceManager.addReloadListener(new ReloadListener());
        }
    }
}
