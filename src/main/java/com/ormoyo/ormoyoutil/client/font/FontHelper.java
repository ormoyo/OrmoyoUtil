package com.ormoyo.ormoyoutil.client.font;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.client.ITextureMethod;
import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import com.ormoyo.ormoyoutil.event.FontRenderEvent;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import com.ormoyo.ormoyoutil.util.Utils;
import com.ormoyo.ormoyoutil.util.Utils.Filter;
import com.ormoyo.ormoyoutil.util.resourcelocation.TextureObjectResourceLocation;
import com.ormoyo.ormoyoutil.util.vec.Vec2i;
import com.ormoyo.ormoyoutil.util.vec.Vec4d;
import com.ormoyo.ormoyoutil.util.vec.Vec4i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.Collection;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class FontHelper
{
    private static final int fontSizeMult = 8;
    static final NonNullMap<ResourceLocation, FontInfo> fontInfo = new NonNullMap<>(FontInfo::new);

    public static FontRenderer createFontRendererFromFont(Font font)
    {
        return new FontRenderer(Minecraft.getMinecraft().gameSettings, null, null, true)
        {
            @Override
            protected void bindTexture(ResourceLocation location)
            {
                this.FONT_HEIGHT = font.getResolution();
            }

            @Override
            public int drawString(String text, float x, float y, int color, boolean dropShadow)
            {
                int i;
                if (dropShadow)
                {
                    i = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
                    i = Math.max(i, this.renderString(text, x, y, color, false));
                }
                else
                {
                    i = this.renderString(text, x, y, color, false);
                }
                return i;
            }

            @Override
            public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor)
            {
                if ((textColor & -67108864) == 0)
                {
                    textColor |= -16777216;
                }
                FontHelper.drawString(font, str, x, y, 1, textColor, wrapWidth);
            }

            private int renderString(String text, float x, float y, int color, boolean dropShadow)
            {
                if ((color & -67108864) == 0)
                {
                    color |= -16777216;
                }

                if (dropShadow)
                {
                    color = (color & 16579836) >> 2 | color & -16777216;
                }
                FontHelper.drawString(font, text, x, y, 1, color);
                return (int) x;
            }

            @Override
            public int getStringWidth(String text)
            {
                return (int) FontHelper.getStringWidth(font, text);
            }

            @Override
            public int getCharWidth(char character)
            {
                return (int) FontHelper.getCharWidth(font, character);
            }
        };
    }

    public static void drawString(Font font, String text, double x, double y, double scale, Color color)
    {
        FontHelper.drawString(font, text, x, y, scale, color.getRGB());
    }

    public static void drawString(Font font, String text, double x, double y, double scale, int color)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Multimap<ResourceLocation, TexturedRect> batches = HashMultimap.create();

        FontRenderEvent.Pre event = new FontRenderEvent.Pre(text, font);
		if (MinecraftForge.EVENT_BUS.post(event))
		{
			return;
		}

        text = event.getText();
        font = event.getFont();

        double yoff = FontHelper.getWordLowestYoffset(font, text);
        double cursorX = 0;

        for (char character : text.toCharArray())
        {
            Glyph glyph = info.glyphs.get(character);
            if (glyph == null)
            {
                OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                return;
            }
            UnicodeBlock block = UnicodeBlock.of(character);
            String blockName = String.valueOf(block);
            ResourceLocation location = info.blockTextures.getOrDefault(blockName, TextureManager.RESOURCE_LOCATION_EMPTY);
            double w = (double) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5 : 1);
            double h = (double) glyph.height / font.getResolution() * fontSizeMult;
            double hoff = ((double) glyph.yoffset / font.getResolution() * fontSizeMult);
            RenderHelper.setupOpacity();
            if (location == TextureManager.RESOURCE_LOCATION_EMPTY)
            {
                BufferedImage image = info.blockMaps.get(blockName);
                if (image != null)
                {
                    DynamicTextureImpl tex = new DynamicTextureImpl(image, font.isAntiAliasing());
                    location = FontHelper.getDynamicImplTextureLocation(font.getRegistryName().toString(), tex);
                    info.blockTextures.put(blockName, location);
                }
            }
            batches.put(location, new TexturedRect(new Vec4d(x + cursorX, y + hoff * scale - yoff, w, h), convertShortsToLong(glyph.x, glyph.y, glyph.width, glyph.height)));
            cursorX += (w + glyph.xoffset) * scale;
        }

        int red = (color >> 16 & 0xFF);
        int green = (color >> 8 & 0xFF);
        int blue = (color & 0xFF);
        int alpha = (color >> 24 & 0xFF);
        for (ResourceLocation location : batches.keySet())
        {
            Vec2i s = RenderHelper.getTextureSize(location);
            int imageWidth = s.getX();
            int imageHeight = s.getY();
            Collection<TexturedRect> rects = batches.get(location);
            Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();
            bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (TexturedRect rect : rects)
            {
                Vec4d pos = rect.position;
                long tex = rect.texCoord;
                short texX = (short) (tex >> 0);
                short texY = (short) (tex >> 16);
                short texWidth = (short) (tex >> 32);
                short texHeight = (short) (tex >> 48);
                double minU = (double) texX / imageWidth;
                double maxU = (double) (texX + texWidth) / imageWidth;
                double minV = (double) texY / imageHeight;
                double maxV = (double) (texY + texHeight) / imageHeight;
                bb.pos(pos.x + scale * pos.z, pos.y + scale * pos.w, 0).tex(maxU, maxV).color(red, green, blue, alpha).endVertex();
                bb.pos(pos.x + scale * pos.z, pos.y, 0).tex(maxU, minV).color(red, green, blue, alpha).endVertex();
                bb.pos(pos.x, pos.y, 0).tex(minU, minV).color(red, green, blue, alpha).endVertex();
                bb.pos(pos.x, pos.y + scale * pos.w, 0).tex(minU, maxV).color(red, green, blue, alpha).endVertex();
            }
            tess.draw();
        }
        MinecraftForge.EVENT_BUS.post(new FontRenderEvent.Post(text, font));
    }

    public static void drawString(Font font, String text, double x, double y, double scale, Color color, double lineWidth)
    {
        FontHelper.drawString(font, text, x, y, scale, color.getRGB(), lineWidth, TextAlignment.LEFT);
    }

    public static void drawString(Font font, String text, double x, double y, double scale, int color, double lineWidth)
    {
        FontHelper.drawString(font, text, x, y, scale, color, lineWidth, TextAlignment.LEFT);
    }

    public static void drawString(Font font, String text, double x, double y, double scale, Color color, double lineWidth, TextAlignment alignment)
    {
        FontHelper.drawString(font, text, x, y, scale, color.getRGB(), lineWidth, alignment);
    }

    public static void drawString(Font font, String text, double x, double y, double scale, int color, double lineWidth, TextAlignment alignment)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Multimap<ResourceLocation, TexturedRect> batches = HashMultimap.create();
        FontRenderEvent.Pre event = new FontRenderEvent.Pre(text, font);
		if (MinecraftForge.EVENT_BUS.post(event))
		{
			return;
		}
        text = event.getText();
        font = event.getFont();
        double yoff = FontHelper.getWordLowestYoffset(font, text);
        double cursorX = 0;
        double cursorY = 0;
        String[] lines = getLinesForFont(font, text, scale, lineWidth);
        RenderHelper.setupOpacity();
        for (String line : lines)
        {
            line = line.trim();
            switch (alignment)
            {
                case LEFT:
                    cursorX = 0;
                    break;
                case CENTER:
                    cursorX = lineWidth / 2;
                    cursorX -= FontHelper.getStringWidth(font, line) * scale * 0.5;
                    break;
                case RIGHT:
                    cursorX = lineWidth - FontHelper.getStringWidth(font, line) * scale;
                    break;
                default:
                    cursorX = 0;
                    return;
            }
            double height = 0;
            for (char character : line.toCharArray())
            {
                Glyph glyph = info.glyphs.get(character);
                if (glyph == null)
                {
                    OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                    continue;
                }
                UnicodeBlock block = UnicodeBlock.of(character);
                String blockName = String.valueOf(block);
                ResourceLocation location = info.blockTextures.getOrDefault(blockName, TextureManager.RESOURCE_LOCATION_EMPTY);
                double w = (double) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5 : 1);
                double h = (double) glyph.height / font.getResolution() * fontSizeMult;
                double hoff = (double) glyph.yoffset / font.getResolution() * fontSizeMult;
                if (location == TextureManager.RESOURCE_LOCATION_EMPTY)
                {
                    BufferedImage image = info.blockMaps.get(blockName);
                    if (image != null)
                    {
                        DynamicTextureImpl tex = new DynamicTextureImpl(image, font.isAntiAliasing());
                        location = FontHelper.getDynamicImplTextureLocation(font.getRegistryName().toString(), tex);
                        info.blockTextures.put(blockName, location);
                    }
                }
                batches.put(location, new TexturedRect(new Vec4d(x + cursorX, y + (hoff - yoff) * scale + cursorY, w, h), convertShortsToLong(glyph.x, glyph.y, glyph.width, glyph.height)));
                cursorX += (w + glyph.xoffset) * scale;
				if (character == ' ')
				{
					continue;
				}
                height = Math.max(height, h + hoff - yoff);
            }
            cursorY += (height + 1) * scale;
        }

        int red = (color >> 16 & 255);
        int green = (color >> 8 & 255);
        int blue = (color & 255);
        int alpha = (color >> 24 & 255);
        for (ResourceLocation location : batches.keySet())
        {
            Vec2i s = RenderHelper.getTextureSize(location);
            int imageWidth = s.getX();
            int imageHeight = s.getY();
            Collection<TexturedRect> rects = batches.get(location);
            Minecraft.getMinecraft().getTextureManager().bindTexture(location);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();
            bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (TexturedRect rect : rects)
            {
                Vec4d pos = rect.position;
                long tex = rect.texCoord;
                short texX = (short) (tex >> 0);
                short texY = (short) (tex >> 16);
                short texWidth = (short) (tex >> 32);
                short texHeight = (short) (tex >> 48);
                double minU = (double) texX / imageWidth;
                double maxU = (double) (texX + texWidth) / imageWidth;
                double minV = (double) texY / imageHeight;
                double maxV = (double) (texY + texHeight) / imageHeight;
                bb.pos(pos.x + scale * pos.z, pos.y + scale * pos.w, 0).tex(maxU, maxV).color(red, green, blue, alpha).endVertex();
                bb.pos(pos.x + scale * pos.z, pos.y, 0).tex(maxU, minV).color(red, green, blue, alpha).endVertex();
                bb.pos(pos.x, pos.y, 0).tex(minU, minV).color(red, green, blue, alpha).endVertex();
                bb.pos(pos.x, pos.y + scale * pos.w, 0).tex(minU, maxV).color(red, green, blue, alpha).endVertex();
            }
            tess.draw();
        }
        MinecraftForge.EVENT_BUS.post(new FontRenderEvent.Post(text, font));
    }

    public static double getStringWidth(Font font, String text)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        double width = 0;
        for (int i = 0; i < text.length(); i++)
        {
            char character = text.charAt(i);
            Glyph glyph = info.glyphs.get(character);
            if (glyph == null)
            {
                OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                return 0;
            }

            double w = (double) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5 : 1);
            if (i == 0)
            {
                width += w;
            }
            else
            {
                width += w + glyph.xoffset;
            }
        }
        return width;
    }

    public static double getStringHeight(Font font, String text)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        double yoff = getWordLowestYoffset(font, text);
        double height = 0;
        for (char character : text.toCharArray())
        {
            Glyph glyph = info.glyphs.get(character);
            if (glyph == null)
            {
                OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                return 0;
            }
			if (glyph.ch == ' ')
			{
				continue;
			}
            double h = (double) glyph.height / font.getResolution() * fontSizeMult;
            double ho = (double) glyph.yoffset / font.getResolution() * fontSizeMult;
            height = Math.max(height, h + ho - yoff);
        }
        return height;
    }

    public static double getStringWidth(Font font, String text, double lineWidth)
    {
        String[] lines = getLinesForFont(font, text, 1, lineWidth);
        double longestLine = 0;
        for (String line : lines)
        {
            double w = FontHelper.getStringWidth(font, line);
            longestLine = Math.min(longestLine, w);
        }
        return Math.min(longestLine, lineWidth);
    }

    public static double getStringHeight(Font font, String text, double lineWidth)
    {
        String[] lines = getLinesForFont(font, text, 1, lineWidth);
        double height = 0;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            height += getStringHeight(font, line) + (i == 0 ? 0 : 1);
        }
        return height;
    }

    public static double getStringHeight(Font font, String text, double scale, double lineWidth)
    {
        String[] lines = getLinesForFont(font, text, scale, lineWidth);
        double height = 0;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            height += getStringHeight(font, line) + (i == 0 ? 0 : 1);
        }
        return height * scale;
    }

    public static double getCharWidth(Font font, char character)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Glyph glyph = info.glyphs.get(character);
        return (double) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5 : 1);
    }

    public static double getCharHeight(Font font, char character)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Glyph glyph = info.glyphs.get(character);
        return (double) glyph.height / font.getResolution() * fontSizeMult;
    }

    private static double getWordLowestYoffset(Font font, String text)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        double height = Integer.MAX_VALUE;
        for (char character : text.toCharArray())
        {
            Glyph glyph = info.glyphs.get(character);
            if (glyph == null)
            {
                OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                return 0;
            }
			if (glyph.ch == ' ')
			{
				continue;
			}
            height = Math.min(height, (double) glyph.yoffset / font.getResolution() * fontSizeMult);
        }
        return height;
    }

    private static String[] getLinesForFont(Font font, String text, double scale, double lineWidth)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Glyph whitespace = info.glyphs.get(' ');
        if (whitespace == null)
        {
            OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + ' ' + " in it's character set");
            return new String[0];
        }
        double whitespaceWidth = whitespace.width * 0.5 * scale;
        int cursorX = 0;
        int currentLineStartIndex = 0;
        char[] formatedText = text.toCharArray();
        String[] words = text.split(" ");
        for (int i = 0; i < words.length; i++)
        {
            String word = words[i];
            char[] chars = word.toCharArray();
            for (int ch = 0; ch < chars.length; ch++)
            {
                char c = chars[ch];
                if (c == '\n')
                {
                    cursorX = 0;
                    break;
                }
                Glyph glyph = info.glyphs.get(c);
                if (glyph == null)
                {
                    OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + c + " in it's character set");
                    continue;
                }
                double width = (double) glyph.width / font.getResolution() * fontSizeMult;
                cursorX += (ch == chars.length - 1 ? width : width + glyph.xoffset) * scale;
                if (cursorX > lineWidth)
                {
                    boolean b = i == 0 || formatedText[currentLineStartIndex] == '\n';
                    formatedText = ArrayUtils.add(formatedText, b ? currentLineStartIndex + ch + 1 : currentLineStartIndex, '\n');
                    ch = b ? ch : 0;
                    cursorX = 0;
                }
            }
            cursorX += whitespaceWidth;
            currentLineStartIndex += word.length() + 1;
        }
        return new String(formatedText).split("\n");
    }

    public enum TextAlignment
    {
        LEFT,
        CENTER,
        RIGHT
	}

    static class Glyph
    {
        final char ch;
        final int x;
        final int y;
        final int width;
        final int height;
        final double xoffset;
        final int yoffset;
        final int heightoffset;

        Glyph(char ch, int x, int y, int width, int height, double xoffset, int yoffset, int heightoffset)
        {
            this.ch = ch;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.xoffset = xoffset;
            this.yoffset = yoffset;
            this.heightoffset = heightoffset;
        }

        @Override
        public String toString()
        {
            ToStringHelper helper = MoreObjects.toStringHelper("");
            helper.add("char", (int) this.ch);
            helper.add("x", this.x);
            helper.add("y", this.y);
            helper.add("width", this.width);
            helper.add("height", this.height);
            helper.add("xoffset", this.xoffset);
            helper.add("yoffset", this.yoffset);
            return helper.toString();
        }
    }

    static class FontInfo
    {
        Map<Character, Glyph> glyphs = Maps.newHashMap();
        Map<String, BufferedImage> blockMaps = Maps.newHashMap();
        Map<String, ResourceLocation> blockTextures = Maps.newHashMap();
        Map<Integer, Page> pages = Maps.newHashMap();

        Vec4i padding = Vec4i.NULL_VECTOR;
    }

    static class Page
    {
        final ResourceLocation texture;
        final int width;
        final int height;

        public Page(ResourceLocation texture, int width, int height)
        {
            this.texture = texture;
            this.width = width;
            this.height = height;
        }
    }

    private static final Map<String, Integer> mapTextureCounters = Maps.newHashMap();

    private static ResourceLocation getDynamicImplTextureLocation(String name, DynamicTextureImpl texture)
    {
        Integer integer = mapTextureCounters.get(name);
        if (integer == null)
        {
            integer = 1;
        }
        else
        {
            integer = integer + 1;
        }
        mapTextureCounters.put(name, integer);
        ResourceLocation resourcelocation = new TextureObjectResourceLocation(String.format("dynamicImpl/%s_%d", name, integer), texture);
        return resourcelocation;
    }

    private static long convertShortsToLong(int a, int b, int c, int d)
    {
        return (a & 0xFFFFL) | ((b & 0xFFFFL) << 16) | ((c & 0xFFFFL) << 32) | ((d & 0xFFFFL) << 48);
    }

    private static class DynamicTextureImpl extends AbstractTexture implements ITextureMethod
    {
        private final int width;
        private final int height;

        public DynamicTextureImpl(BufferedImage bufferedImage, boolean antiAliasing)
        {
            this.width = bufferedImage.getWidth();
            this.height = bufferedImage.getHeight();

            Utils.loadImageToTexture(this.getGlTextureId(), bufferedImage, antiAliasing ? Filter.LINEAR : Filter.NEAREST, antiAliasing ? Filter.LINEAR_MIPMAP_NEAREST : Filter.NEAREST, false);
        }

        @Override
        public Vec2i getTextureSize()
        {
            return new Vec2i(this.width, this.height);
        }

        @Override
        public void loadTexture(IResourceManager resourceManager) throws IOException
        {
        }
    }

    private static class TexturedRect
    {
        private final Vec4d position;
        private final long texCoord;

        public TexturedRect(Vec4d pos, long tex)
        {
            this.position = pos;
            this.texCoord = tex;
        }
    }
}
