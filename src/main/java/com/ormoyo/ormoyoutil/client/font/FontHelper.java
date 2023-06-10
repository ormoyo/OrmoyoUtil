package com.ormoyo.ormoyoutil.client.font;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.client.RenderHelper;
import com.ormoyo.ormoyoutil.event.FontRenderEvent;
import com.ormoyo.ormoyoutil.util.NonNullMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.lang.Character.UnicodeBlock;
import java.util.Collection;
import java.util.Map;

public class FontHelper
{
    private static final int fontSizeMult = 8;
    static final Map<ResourceLocation, FontInfo> fontInfo = new NonNullMap<>(FontInfo::new);

    public static void drawString(Font font, String text, float x, float y, float scale, Color color)
    {
        FontHelper.drawString(font, text, x, y, scale, color.getRGB());
    }

    public static void drawString(Font font, String text, float x, float y, float scale, int color)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Multimap<ResourceLocation, TexturedRect> batches = HashMultimap.create();

        FontRenderEvent.Pre event = new FontRenderEvent.Pre(text, font);
        if (MinecraftForge.EVENT_BUS.post(event)) return;

        text = event.getText();
        font = event.getFont();

        float yoff = FontHelper.getWordLowestYoffset(font, text);
        float cursorX = 0;

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

            float w = (float) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5f : 1f);
            float h = (float) glyph.height / font.getResolution() * fontSizeMult;

            float hoff = ((float) glyph.yoffset / font.getResolution() * fontSizeMult);

            RenderHelper.setupBlend();
            if (location == TextureManager.RESOURCE_LOCATION_EMPTY)
            {
                NativeImage image = info.blockMaps.get(blockName);
                if (image == null)
                    return;

                DynamicTextureImpl tex = new DynamicTextureImpl(image, font.hasAntiAliasing());
                location = FontHelper.getDynamicImplTextureLocation(font.getRegistryName().getPath(), tex);

                info.blockTextures.put(blockName, location);
            }

            batches.put(location, new TexturedRect(convertFloatsToLong(x + cursorX, y + hoff * scale - yoff), convertFloatsToLong(w, h), convertShortsToLong(glyph.x, glyph.y, glyph.width, glyph.height)));
            cursorX += (w + glyph.xoffset) * scale;
        }

        int red = (color >> 16 & 0xFF);
        int green = (color >> 8 & 0xFF);
        int blue = (color & 0xFF);
        int alpha = (color >> 24 & 0xFF);

        for (ResourceLocation location : batches.keySet())
        {
            DynamicTextureImpl dynamicTexture = (DynamicTextureImpl) Minecraft.getInstance().getTextureManager().getTexture(location);

            int imageWidth = dynamicTexture.getTextureData().getWidth();
            int imageHeight = dynamicTexture.getTextureData().getHeight();

            Collection<TexturedRect> rects = batches.get(location);
            Minecraft.getInstance().getTextureManager().bindTexture(location);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();

            bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            for (TexturedRect rect : rects)
            {
                long pos = rect.pos;

                long size = rect.size;
                long tex = rect.texCord;

                float posX = Float.intBitsToFloat((int)(pos >> 32));
                float posY = Float.intBitsToFloat((int)pos);
                float sizeX = Float.intBitsToFloat((int)(size >> 32));
                float sizeY = Float.intBitsToFloat((int)size);

                short texX = (short) (tex >> 0);
                short texY = (short) (tex >> 16);
                short texWidth = (short) (tex >> 32);
                short texHeight = (short) (tex >> 48);

                float minU = (float) texX / imageWidth;
                float maxU = (float) (texX + texWidth) / imageWidth;
                float minV = (float) texY / imageHeight;
                float maxV = (float) (texY + texHeight) / imageHeight;

                bb.pos(posX + scale * sizeX, posY + scale * sizeY, 0).color(red, green, blue, alpha).tex(maxU, maxV).endVertex();
                bb.pos(posX + scale * sizeX, posY, 0).color(red, green, blue, alpha).tex(maxU, minV).endVertex();
                bb.pos(posX, posY, 0).color(red, green, blue, alpha).tex(minU, minV).endVertex();
                bb.pos(posX, posY + scale * sizeY, 0).color(red, green, blue, alpha).tex(minU, maxV).endVertex();
            }
            tess.draw();
        }
        MinecraftForge.EVENT_BUS.post(new FontRenderEvent.Post(text, font));
    }

    public static void drawString(Font font, String text, float x, float y, float scale, Color color, float lineWidth)
    {
        FontHelper.drawString(font, text, x, y, scale, color.getRGB(), lineWidth, TextAlignment.LEFT);
    }

    public static void drawString(Font font, String text, float x, float y, float scale, int color, float lineWidth)
    {
        FontHelper.drawString(font, text, x, y, scale, color, lineWidth, TextAlignment.LEFT);
    }

    public static void drawString(Font font, String text, float x, float y, float scale, Color color, float lineWidth, TextAlignment alignment)
    {
        FontHelper.drawString(font, text, x, y, scale, color.getRGB(), lineWidth, alignment);
    }

    public static void drawString(Font font, String text, float x, float y, float scale, int color, float lineWidth, TextAlignment alignment)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Multimap<ResourceLocation, TexturedRect> batches = HashMultimap.create();

        FontRenderEvent.Pre event = new FontRenderEvent.Pre(text, font);
        if (MinecraftForge.EVENT_BUS.post(event)) return;

        text = event.getText();
        font = event.getFont();

        float yoff = FontHelper.getWordLowestYoffset(font, text);

        float cursorX = 0;
        float cursorY = 0;

        RenderHelper.setupBlend();

        String[] lines = getLinesForFont(font, text, scale, lineWidth);
        for (String line : lines)
        {
            line = line.trim();
            switch (alignment)
            {
                case CENTER:
                    cursorX = lineWidth / 2;
                    cursorX -= FontHelper.getStringWidth(font, line) * scale * 0.5;

                    break;
                case RIGHT:
                    cursorX = lineWidth - FontHelper.getStringWidth(font, line) * scale;
                    break;
                default:
                    break;
            }

            float height = 0;
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

                float w = (float) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5f : 1f);
                float h = (float) glyph.height / font.getResolution() * fontSizeMult;

                float hoff = (float) glyph.yoffset / font.getResolution() * fontSizeMult;

                if (location == TextureManager.RESOURCE_LOCATION_EMPTY)
                {
                    NativeImage image = info.blockMaps.get(blockName);
                    if (image != null)
                    {
                        DynamicTextureImpl tex = new DynamicTextureImpl(image, font.hasAntiAliasing());
                        location = FontHelper.getDynamicImplTextureLocation(font.getRegistryName().getPath(), tex);

                        info.blockTextures.put(blockName, location);
                    }
                }

                batches.put(location, new TexturedRect(convertFloatsToLong(x + cursorX, y + (hoff - yoff) * scale + cursorY), convertFloatsToLong(w, h), convertShortsToLong(glyph.x, glyph.y, glyph.width, glyph.height)));
                cursorX += (w + glyph.xoffset) * scale;

                if (character == ' ') continue;
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
            DynamicTextureImpl dynamicTexture = (DynamicTextureImpl) Minecraft.getInstance().getTextureManager().getTexture(location);

            int imageWidth = dynamicTexture.getTextureData().getWidth();
            int imageHeight = dynamicTexture.getTextureData().getHeight();

            Collection<TexturedRect> rects = batches.get(location);

            Minecraft.getInstance().getTextureManager().bindTexture(location);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();

            bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            for (TexturedRect rect : rects)
            {
                long pos = rect.pos;

                long size = rect.size;
                long tex = rect.texCord;

                float posX = Float.intBitsToFloat((int)(pos >> 32));
                float posY = Float.intBitsToFloat((int)pos);
                float sizeX = Float.intBitsToFloat((int)(size >> 32));
                float sizeY = Float.intBitsToFloat((int)size);

                short texX = (short) (tex >> 0);
                short texY = (short) (tex >> 16);
                short texWidth = (short) (tex >> 32);
                short texHeight = (short) (tex >> 48);

                float minU = (float) texX / imageWidth;
                float maxU = (float) (texX + texWidth) / imageWidth;
                float minV = (float) texY / imageHeight;
                float maxV = (float) (texY + texHeight) / imageHeight;

                bb.pos(posX + scale * sizeX, posY + scale * sizeY, 0).color(red, green, blue, alpha).tex(maxU, maxV).endVertex();
                bb.pos(posX + scale * sizeX, posY, 0).color(red, green, blue, alpha).tex(maxU, minV).endVertex();
                bb.pos(posX, posY, 0).color(red, green, blue, alpha).tex(minU, minV).endVertex();
                bb.pos(posX, posY + scale * sizeY, 0).color(red, green, blue, alpha).tex(minU, maxV).endVertex();
            }
            tess.draw();
        }
        MinecraftForge.EVENT_BUS.post(new FontRenderEvent.Post(text, font));
    }

    public static float getStringWidth(Font font, String text)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());

        float width = 0;
        for (int i = 0; i < text.length(); i++)
        {
            char character = text.charAt(i);
            Glyph glyph = info.glyphs.get(character);
            if (glyph == null)
            {
                OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                return 0;
            }

            float w = (float) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5f : 1f);
            width += i == 0 ? w : w + glyph.xoffset;
        }

        return width;
    }

    public static float getStringHeight(Font font, String text)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        float yoff = FontHelper.getWordLowestYoffset(font, text);

        float height = 0;
        for (char character : text.toCharArray())
        {
            Glyph glyph = info.glyphs.get(character);
            if (glyph == null)
            {
                OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                return 0;
            }

            if (glyph.ch == ' ')
                continue;

            float h = (float) glyph.height / font.getResolution() * fontSizeMult;
            float ho = (float) glyph.yoffset / font.getResolution() * fontSizeMult;

            height = Math.max(height, h + ho - yoff);
        }

        return height;
    }

    public static float getStringWidth(Font font, String text, float lineWidth)
    {
        String[] lines = getLinesForFont(font, text, 1, lineWidth);
        float longestLine = 0;

        for (String line : lines)
        {
            float w = FontHelper.getStringWidth(font, line);
            longestLine = Math.min(longestLine, w);
        }

        return Math.min(longestLine, lineWidth);
    }

    public static double getStringHeight(Font font, String text, float lineWidth)
    {
        String[] lines = getLinesForFont(font, text, 1, lineWidth);

        float height = 0;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            height += getStringHeight(font, line) + (i == 0 ? 0 : 1);
        }

        return height;
    }

    public static float getStringHeight(Font font, String text, float scale, float lineWidth)
    {
        String[] lines = getLinesForFont(font, text, scale, lineWidth);

        float height = 0;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            height += getStringHeight(font, line) + (i == 0 ? 0 : 1);
        }

        return height * scale;
    }

    public static float getCharWidth(Font font, char character)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Glyph glyph = info.glyphs.get(character);

        return (float) glyph.width / font.getResolution() * fontSizeMult * (character == ' ' ? 0.5f : 1f);
    }

    public static float getCharHeight(Font font, char character)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Glyph glyph = info.glyphs.get(character);

        return (float) glyph.height / font.getResolution() * fontSizeMult;
    }

    private static float getWordLowestYoffset(Font font, String text)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());

        float height = Integer.MAX_VALUE;
        for (char character : text.toCharArray())
        {
            Glyph glyph = info.glyphs.get(character);
            if (glyph == null)
            {
                OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + character + " in it's character set");
                return 0;
            }

            if (glyph.ch == ' ') continue;
            height = Math.min(height, (float) glyph.yoffset / font.getResolution() * fontSizeMult);
        }

        return height;
    }

    private static String[] getLinesForFont(Font font, String text, float scale, float lineWidth)
    {
        FontInfo info = fontInfo.get(font.getRegistryName());
        Glyph whitespace = info.glyphs.get(' ');

        if (whitespace == null)
        {
            OrmoyoUtil.LOGGER.error("The font " + font.getRegistryName() + " doesn't have the character " + ' ' + " in it's character set");
            return new String[0];
        }

        float whitespaceWidth = whitespace.width * 0.5f * scale;

        int cursorX = 0;
        int currentLineStartIndex = 0;

        char[] formattedText = text.toCharArray();
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

                float width = (float) glyph.width / font.getResolution() * fontSizeMult;
                cursorX += (ch == chars.length - 1 ? width : width + glyph.xoffset) * scale;

                if (cursorX > lineWidth)
                {
                    boolean b = i == 0 || formattedText[currentLineStartIndex] == '\n';
                    formattedText = ArrayUtils.insert(b ? currentLineStartIndex + ch + 1 : currentLineStartIndex, formattedText, '\n');

                    ch = b ? ch : 0;
                    cursorX = 0;
                }
            }

            cursorX += whitespaceWidth;
            currentLineStartIndex += word.length() + 1;
        }

        return new String(formattedText).split("\n");
    }

    private static ResourceLocation getDynamicImplTextureLocation(String name, DynamicTextureImpl texture)
    {
        return Minecraft.getInstance().getTextureManager().getDynamicTextureLocation(name, texture);
    }

    private static long convertFloatsToLong(float a, float b)
    {
        return (((long)Float.floatToIntBits(a)) << 32) | (Float.floatToIntBits(b) & 0xffffffffL);
    }

    private static long convertShortsToLong(int a, int b, int c, int d)
    {
        return (a & 0xFFFFL) | ((b & 0xFFFFL) << 16) | ((c & 0xFFFFL) << 32) | ((d & 0xFFFFL) << 48);
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
        final int xoffset;
        final int yoffset;
        final int heightoffset;

        Glyph(char ch, int x, int y, int width, int height, int xoffset, int yoffset, int heightoffset)
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
        Map<String, NativeImage> blockMaps = Maps.newHashMap();

        Map<String, ResourceLocation> blockTextures = Maps.newHashMap();
    }

    private static class DynamicTextureImpl extends DynamicTexture
    {
        private final boolean antiAliasing;

        public DynamicTextureImpl(NativeImage image, boolean antiAliasing)
        {
            super(image);
            this.antiAliasing = antiAliasing;
        }

        @Override
        public void updateDynamicTexture()
        {
            if (this.getTextureData() != null)
            {
                this.bindTexture();

                NativeImage image = this.getTextureData();
                image.uploadTextureSub(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), this.antiAliasing, false, this.antiAliasing, false);
            }
        }
    }

    private static class TexturedRect
    {
        private final long pos;
        private final long size;

        private final long texCord;

        private TexturedRect(long pos, long size, long tex)
        {
            this.pos = pos;
            this.size = size;
            this.texCord = tex;
        }
    }
}
