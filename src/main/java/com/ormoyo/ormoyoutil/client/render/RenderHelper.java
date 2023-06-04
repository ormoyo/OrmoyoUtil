package com.ormoyo.ormoyoutil.client.render;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.client.ITextureMethod;
import com.ormoyo.ormoyoutil.client.button.GuiBetterButton;
import com.ormoyo.ormoyoutil.util.Utils;
import com.ormoyo.ormoyoutil.util.resourcelocation.FilterResourceLocation;
import com.ormoyo.ormoyoutil.util.resourcelocation.TextureObjectResourceLocation;
import com.ormoyo.ormoyoutil.util.vec.Vec2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.*;
import static org.lwjgl.opengl.GL11.GL_QUADS;

@SideOnly(Side.CLIENT)
public class RenderHelper
{
    private static final Map<ResourceLocation, Vec2i> locationToSize = Maps.newHashMap();
    private static final Map<Class<? extends ITextureObject>, TextureObjectHandlers> handlerRegistry = Maps.newHashMap();

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
        if (location instanceof TextureObjectResourceLocation)
        {
            TextureObjectResourceLocation textureLocation = (TextureObjectResourceLocation) location;
            ITextureMethod texture = textureLocation.getTexture();

            Vec2i size = texture.getTextureSize();

            Minecraft.getMinecraft().getTextureManager().loadTexture(textureLocation, texture);
            RenderHelper.locationToSize.put(location, size == null ? Vec2i.NULL_VECTOR : size);

            return size;
        }

        if (location instanceof FilterResourceLocation)
        {
            FilterResourceLocation filterLocation = (FilterResourceLocation) location;

            class FilterTexture extends AbstractTexture implements ITextureMethod
            {
                int width;
                int height;

                @Override
                public void loadTexture(IResourceManager resourceManager) throws IOException
                {
                    this.deleteGlTexture();
                    IResource iresource = null;
                    try
                    {
                        iresource = resourceManager.getResource(filterLocation);

                        BufferedImage image = ImageIO.read(iresource.getInputStream());
                        Utils.loadImageToTexture(this.getGlTextureId(), image, filterLocation.getScaledUpFilter(),
                                filterLocation.getScaledDownFilter(),
                                false);

                        this.width = image.getWidth();
                        this.height = image.getHeight();
                    }
                    finally
                    {
                        IOUtils.closeQuietly(iresource);
                    }
                }

                @Override
                public Vec2i getTextureSize()
                {
                    return new Vec2i(this.width, this.height);
                }
            }

            FilterTexture texture = new FilterTexture();
            Minecraft.getMinecraft().getTextureManager().loadTexture(filterLocation, texture);

            RenderHelper.locationToSize.put(location, texture.getTextureSize());
            return texture.getTextureSize();
        }

        ITextureObject obj = Minecraft.getMinecraft().getTextureManager().getTexture(location);
        if (obj == null)
        {
            obj = new SimpleTexture(location);
            Minecraft.getMinecraft().getTextureManager().loadTexture(location, obj);
        }

        TextureObjectHandlers handler = getHandler(obj.getClass());
        if (handler != null)
        {
            Vec2i size = handler.getSize(obj);
            RenderHelper.locationToSize.put(location, size == null ? Vec2i.NULL_VECTOR : size);

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

        buffer.begin(GL_QUADS, POSITION_TEX_COLOR);
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

        buffer.begin(GL_QUADS, POSITION_TEX_COLOR);
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

        buffer.begin(GL_QUADS, POSITION_TEX_COLOR);
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

        buffer.begin(GL_QUADS, POSITION_TEX_COLOR);
        RenderHelper.drawSeamlessTexturedRectToBuffer(buffer, texture, x, y, u, v, uWidth, vHeight, width, height, textureOffsetX, textureOffsetY, scale, color);
        tessellator.draw();
    }

    public static void drawButtonHitBox(GuiButton button)
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE);
        setupOpacity();

        double x = button instanceof GuiBetterButton ? ((GuiBetterButton) button).xD : button.x;
        double y = button instanceof GuiBetterButton ? ((GuiBetterButton) button).yD : button.y;
        double width = button instanceof GuiBetterButton ? ((GuiBetterButton) button).widthD : button.width;
        double height = button instanceof GuiBetterButton ? ((GuiBetterButton) button).heightD : button.height;
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

    public static void drawButtonHitBox(GuiButton button, Color color)
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE);
        setupOpacity();

        double x = button instanceof GuiBetterButton ? ((GuiBetterButton) button).xD : button.x;
        double y = button instanceof GuiBetterButton ? ((GuiBetterButton) button).yD : button.y;
        double width = button instanceof GuiBetterButton ? ((GuiBetterButton) button).widthD : button.width;
        double height = button instanceof GuiBetterButton ? ((GuiBetterButton) button).heightD : button.height;
        int opacity = Math.max(125, color.getAlpha());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL_QUADS, POSITION_COLOR);

        buffer.pos(x + width, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), opacity).endVertex();
        buffer.pos(x + width, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), opacity).endVertex();
        buffer.pos(x, y, 0).color(color.getRed(), color.getGreen(), color.getBlue(), opacity).endVertex();
        buffer.pos(x, y + height, 0).color(color.getRed(), color.getGreen(), color.getBlue(), opacity).endVertex();

        tessellator.draw();
    }

    public static void drawButtonHitBox(GuiButton button, int color)
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE);
        setupOpacity();

        double x = button instanceof GuiBetterButton ? ((GuiBetterButton) button).xD : button.x;
        double y = button instanceof GuiBetterButton ? ((GuiBetterButton) button).yD : button.y;
        double width = button instanceof GuiBetterButton ? ((GuiBetterButton) button).widthD : button.width;
        double height = button instanceof GuiBetterButton ? ((GuiBetterButton) button).heightD : button.height;

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
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + width) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + height) / imageHeight;

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
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + width) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + height) / imageHeight;

        buffer.pos(x + scale * width, y + scale * height, 0).tex(maxU, maxV).color(red, green, blue, alpha).endVertex();
        buffer.pos(x + scale * width, y, 0).tex(maxU, minV).color(red, green, blue, alpha).endVertex();
        buffer.pos(x, y, 0).tex(minU, minV).color(red, green, blue, alpha).endVertex();
        buffer.pos(x, y + scale * height, 0).tex(minU, maxV).color(red, green, blue, alpha).endVertex();
    }

    public static void drawTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + uWidth) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + vHeight) / imageHeight;

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
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + uWidth) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + vHeight) / imageHeight;

        buffer.pos(x + scale * width, y + scale * height, 0).tex(maxU, maxV).color(red, green, blue, alpha).endVertex();
        buffer.pos(x + scale * width, y, 0).tex(maxU, minV).color(red, green, blue, alpha).endVertex();
        buffer.pos(x, y, 0).tex(minU, minV).color(red, green, blue, alpha).endVertex();
        buffer.pos(x, y + scale * height, 0).tex(minU, maxV).color(red, green, blue, alpha).endVertex();
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, double scale)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + uWidth) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + vHeight) / imageHeight;

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
                double maxUOff = maxU;
                double maxVOff = maxV;

                if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (u + (width % uWidth)) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width;
                        maxUOff = (u + width) / imageWidth;
                    }
                }

                if (iy + 1 >= hRatio)
                {
                    if (height > vHeight)
                    {
                        if (height % vHeight != 0)
                        {
                            hOff = height % vHeight;
                            maxVOff = (v + (height % vHeight)) / imageHeight;
                        }
                    }
                    else
                    {
                        hOff = height;
                        maxVOff = (v + height) / imageHeight;
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
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + uWidth) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + vHeight) / imageHeight;

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
                double maxUOff = maxU;
                double maxVOff = maxV;

                if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (u + (width % uWidth)) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width;
                        maxUOff = (u + width) / imageWidth;
                    }
                }

                if (iy + 1 >= hRatio)
                {
                    if (height > vHeight)
                    {
                        if (height % vHeight != 0)
                        {
                            hOff = height % vHeight;
                            maxVOff = (v + (height % vHeight)) / imageHeight;
                        }
                    }
                    else
                    {
                        hOff = height;
                        maxVOff = (v + height) / imageHeight;
                    }
                }

                buffer.pos(xPos + scale * wOff, yPos + scale * hOff, 0).tex(maxUOff, maxVOff).color(red, green, blue, alpha).endVertex();
                buffer.pos(xPos + scale * wOff, yPos, 0).tex(maxUOff, minV).color(red, green, blue, alpha).endVertex();
                buffer.pos(xPos, yPos, 0).tex(minU, minV).color(red, green, blue, alpha).endVertex();
                buffer.pos(xPos, yPos + scale * hOff, 0).tex(minU, maxVOff).color(red, green, blue, alpha).endVertex();

                xPos += scale * w;
            }

            xPos = x;
            yPos += scale * h;
        }
    }

    public static void drawSeamlessTexturedRectToBuffer(BufferBuilder buffer, ResourceLocation texture, double x, double y, int u, int v, int uWidth, int vHeight, double width, double height, int textureOffsetX, int textureOffsetY, double scale)
    {
        Vec2i size = getTextureSize(texture);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + uWidth) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + vHeight) / imageHeight;

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

            double minVOff = minV;
            double maxVOff = maxV;

            if (iy == 0)
            {
                if (textureOffsetY != 0)
                {
                    minVOff = (double) (v + textureOffsetY + vHeight) / imageHeight;
                    hOff = -textureOffsetY;
                    if (yPos + hOff > y + height)
                    {
                        hOff = height;
                        maxVOff = ((v + textureOffsetY + vHeight) + hOff) / imageHeight;
                    }
                }
                else
                {
                    maxVOff = (v + h) / imageHeight;
                }
            }
            else if (iy + 1 >= hRatio)
            {
                if (height > vHeight)
                {
                    if (height % vHeight != 0)
                    {
                        hOff = height % vHeight;
                        maxVOff = (v + hOff) / imageHeight;
                    }
                }
                else
                {
                    hOff = height + textureOffsetY;
                    maxVOff = (v + textureOffsetY + height) / imageHeight;
                }
            }

            if (yPos + hOff > y + height)
            {
                double vy = yPos - y;
                hOff = height - vy;
                maxVOff = (v + hOff) / imageHeight;
            }

            for (int ix = 0; ix < wRatio; ix++)
            {
                double wOff = w;

                double minUOff = minU;
                double maxUOff = maxU;

                if (ix == 0)
                {
                    if (textureOffsetX != 0)
                    {
                        minUOff = (double) (u + textureOffsetX + uWidth) / imageWidth;
                        wOff = -textureOffsetX;
                        if (xPos + wOff > x + width)
                        {
                            wOff = width;
                            maxUOff = ((u + textureOffsetX + uWidth) + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        maxUOff = (u + w) / imageWidth;
                    }
                }
                else if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (u + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width + textureOffsetX;
                        maxUOff = (u + textureOffsetX + width) / imageWidth;
                    }
                }

                if (xPos + wOff > x + width)
                {
                    double ux = xPos - x;
                    wOff = width - ux;
                    maxUOff = (u + wOff) / imageWidth;
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
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        int imageWidth = size.getX();
        int imageHeight = size.getY();

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        double minU = (double) u / imageWidth;
        double maxU = (double) (u + uWidth) / imageWidth;
        double minV = (double) v / imageHeight;
        double maxV = (double) (v + vHeight) / imageHeight;

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

            double minVOff = minV;
            double maxVOff = maxV;

            if (iy == 0)
            {
                if (textureOffsetY != 0)
                {
                    minVOff = (double) (v + textureOffsetY + vHeight) / imageHeight;
                    hOff = -textureOffsetY;
                    if (yPos + hOff > y + height)
                    {
                        hOff = height;
                        maxVOff = ((v + textureOffsetY + vHeight) + hOff) / imageHeight;
                    }
                }
                else
                {
                    maxVOff = (v + h) / imageHeight;
                }
            }
            else if (iy + 1 >= hRatio)
            {
                if (height > vHeight)
                {
                    if (height % vHeight != 0)
                    {
                        hOff = height % vHeight;
                        maxVOff = (v + hOff) / imageHeight;
                    }
                }
                else
                {
                    hOff = height + textureOffsetY;
                    maxVOff = (v + textureOffsetY + height) / imageHeight;
                }
            }

            if (yPos + hOff > y + height)
            {
                double vy = yPos - y;
                hOff = height - vy;
                maxVOff = (v + hOff) / imageHeight;
            }

            for (int ix = 0; ix < wRatio; ix++)
            {
                double wOff = w;

                double minUOff = minU;
                double maxUOff = maxU;

                if (ix == 0)
                {
                    if (textureOffsetX != 0)
                    {
                        minUOff = (double) (u + textureOffsetX + uWidth) / imageWidth;
                        wOff = -textureOffsetX;

                        if (xPos + wOff > x + width)
                        {
                            wOff = width;
                            maxUOff = ((u + textureOffsetX + uWidth) + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        maxUOff = (u + w) / imageWidth;
                    }
                }
                else if (ix + 1 >= wRatio)
                {
                    if (width > uWidth)
                    {
                        if (width % uWidth != 0)
                        {
                            wOff = width % uWidth;
                            maxUOff = (u + wOff) / imageWidth;
                        }
                    }
                    else
                    {
                        wOff = width + textureOffsetX;
                        maxUOff = (u + textureOffsetX + width) / imageWidth;
                    }
                }

                if (xPos + wOff > x + width)
                {
                    double ux = xPos - x;

                    wOff = width - ux;
                    maxUOff = (u + wOff) / imageWidth;
                }

                buffer.pos(xPos + scale * wOff, yPos + scale * hOff, 0).tex(maxUOff, maxVOff).color(red, green, blue, alpha).endVertex();
                buffer.pos(xPos + scale * wOff, yPos, 0).tex(maxUOff, minVOff).color(red, green, blue, alpha).endVertex();
                buffer.pos(xPos, yPos, 0).tex(minUOff, minVOff).color(red, green, blue, alpha).endVertex();
                buffer.pos(xPos, yPos + scale * hOff, 0).tex(minUOff, maxVOff).color(red, green, blue, alpha).endVertex();

                xPos += scale * wOff;
            }

            xPos = x;
            yPos += scale * hOff;
        }
    }

    public static void setupOpacity()
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private static TextureObjectHandlers getHandler(Class<? extends ITextureObject> clazz)
    {
        TextureObjectHandlers handler = handlerRegistry.get(clazz);

        if (handler == null)
        {
            for (Entry<Class<? extends ITextureObject>, TextureObjectHandlers> entry : handlerRegistry.entrySet())
            {
                if (entry.getKey().isAssignableFrom(clazz))
                {
                    handler = entry.getValue();
                    break;
                }
            }
        }

        return handler;
    }

    @SideOnly(Side.CLIENT)
    private enum TextureObjectHandlers
    {
        SIMPLE_TEXTURE(SimpleTexture.class)
                {
                    @Override
                    public Vec2i getSize(ITextureObject obj)
                    {
                        SimpleTexture texture = (SimpleTexture) obj;

                        int width = 0;
                        int height = 0;

                        IResource iresource = null;
                        try
                        {
                            ResourceLocation location = (ResourceLocation) simpleTextureLocationField.get(texture);
                            iresource = Minecraft.getMinecraft().getResourceManager().getResource(location);

                            BufferedImage img = TextureUtil.readBufferedImage(iresource.getInputStream());

                            width = img.getWidth();
                            height = img.getHeight();
                        }
                        catch (IllegalArgumentException | IllegalAccessException | IOException e)
                        {
                            e.printStackTrace();
                        }
                        finally
                        {
                            IOUtils.closeQuietly(iresource);
                        }
                        return new Vec2i(width, height);
                    }
                },

        DYNAMIC_TEXTURE(DynamicTexture.class)
                {
                    @Override
                    public Vec2i getSize(ITextureObject obj)
                    {
                        int width = 0;
                        int height = 0;

                        DynamicTexture texture = (DynamicTexture) obj;
                        int[] dynamicTextureData = texture.getTextureData();

                        try
                        {
                            width = dynamicTextureWidthField.getInt(obj);
                            height = dynamicTextureData.length / width;
                        }
                        catch (IllegalArgumentException | IllegalAccessException e)
                        {
                            e.printStackTrace();
                        }
                        return new Vec2i(width, height);
                    }
                },

        LAYERED_TEXTURE(LayeredTexture.class)
                {
                    @Override
                    public Vec2i getSize(ITextureObject obj)
                    {
                        LayeredTexture texture = (LayeredTexture) obj;
                        for (String s : texture.layeredTextureNames)
                        {
                            if (s != null)
                            {
                                IResource iresource = null;
                                try
                                {
                                    iresource = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(s));
                                    BufferedImage bufferedimage = TextureUtil.readBufferedImage(iresource.getInputStream());

                                    return new Vec2i(bufferedimage.getWidth(), bufferedimage.getHeight());
                                }
                                catch (IOException io)
                                {
                                    io.printStackTrace();
                                }
                                finally
                                {
                                    IOUtils.closeQuietly(iresource);
                                }
                            }
                            break;
                        }
                        return null;
                    }
                },

        LAYRED_COLOR_MASK_TEXTURE(LayeredColorMaskTexture.class)
                {
                    @Override
                    public Vec2i getSize(ITextureObject obj)
                    {
                        LayeredColorMaskTexture texture = (LayeredColorMaskTexture) obj;

                        int width = 0;
                        int height = 0;

                        IResource iresource = null;
                        try
                        {
                            ResourceLocation location = (ResourceLocation) layeredColorMaskTextureLocationField.get(texture);
                            iresource = Minecraft.getMinecraft().getResourceManager().getResource(location);

                            BufferedImage img = TextureUtil.readBufferedImage(iresource.getInputStream());

                            width = img.getWidth();
                            height = img.getHeight();
                        }
                        catch (IllegalArgumentException | IllegalAccessException | IOException e)
                        {
                            e.printStackTrace();
                        }
                        finally
                        {
                            IOUtils.closeQuietly(iresource);
                        }
                        return new Vec2i(width, height);
                    }
                };

        private final Class<? extends ITextureObject> clazz;
        private static final Field simpleTextureLocationField = ObfuscationReflectionHelper.findField(SimpleTexture.class, "field_110568_b");
        private static final Field dynamicTextureWidthField = ObfuscationReflectionHelper.findField(DynamicTexture.class, "field_94233_j");
        private static final Field layeredColorMaskTextureLocationField = ObfuscationReflectionHelper.findField(LayeredColorMaskTexture.class, "field_174948_g");

        TextureObjectHandlers(Class<? extends ITextureObject> clazz)
        {
            this.clazz = clazz;
        }

        public abstract Vec2i getSize(ITextureObject obj);

        static
        {
            for (TextureObjectHandlers handler : TextureObjectHandlers.values())
            {
                handlerRegistry.put(handler.clazz, handler);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ReloadLisitener implements ISelectiveResourceReloadListener
    {
        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
        {
            if (resourcePredicate.test(VanillaResourceType.TEXTURES))
            {
                for (Iterator<Entry<ResourceLocation, Vec2i>> iterator = locationToSize.entrySet().iterator(); iterator.hasNext(); )
                {
                    Entry<ResourceLocation, Vec2i> entry = iterator.next();
                    ITextureObject obj = Minecraft.getMinecraft().getTextureManager().getTexture(entry.getKey());

                    if (obj != null)
                    {
                        if (obj == TextureUtil.MISSING_TEXTURE)
                        {
                            iterator.remove();
                            continue;
                        }

                        getTextureSize(entry.getKey());
                    }
                }
            }
        }
    }

    static
    {
        try
        {
            Class.forName(RenderHelper.class.getName() + "$TextureObjectHandlers");
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }

        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        WHITE = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("white", new DynamicTexture(image));

        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager)
        {
            IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) resourceManager;
            reloadableResourceManager.registerReloadListener(new ReloadLisitener());
        }
    }
}
