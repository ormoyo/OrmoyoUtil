package com.ormoyo.ormoyoutil.client.font;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.client.font.FontHelper.FontInfo;
import com.ormoyo.ormoyoutil.client.font.FontHelper.Glyph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.resources.IResource;
import net.minecraft.util.math.MathHelper;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public class FontLoader
{
    public static void loadFont(com.ormoyo.ormoyoutil.client.font.Font font)
    {
        loadTTF(font);
    }

    private static void loadTTF(com.ormoyo.ormoyoutil.client.font.Font font)
    {
        class glyph
        {
            final BufferedImage image;

            final int x;
            final int y;

            public glyph(int x, int y, BufferedImage image)
            {
                this.x = x;
                this.y = y;
                this.image = image;
            }
        }

        int resolution = font.getResolution();
        boolean antiAliasing = font.hasAntiAliasing();

        try (IResource iresource = Minecraft.getInstance().getResourceManager().getResource(font.getRegistryName()))
        {
            InputStream stream = iresource.getInputStream();

            Font f = Font.createFont(Font.TRUETYPE_FONT, stream);
            f = f.deriveFont((float) resolution);

            try
            {
                FontInfo info = new FontInfo();

                int rowHeight = 0;

                int positionX = 0;
                int positionY = 0;

                Character.UnicodeBlock currentBlock = null;

                Multimap<String, glyph> blocks = ArrayListMultimap.create();
                Map<String, Integer> sizes = Maps.newHashMap();

                for (char c = Character.MIN_VALUE; c < Character.MAX_VALUE; c++)
                {
                    if (!f.canDisplay(c)) continue;

                    Character.UnicodeBlock characterBlock = Character.UnicodeBlock.of(c);

                    String blockName = String.valueOf(characterBlock);
                    BufferedImage fontImage = getFontImage(f, c, antiAliasing);

                    int yoffset = getEmptyY(fontImage);
                    fontImage = Crop(fontImage);

                    if (characterBlock != currentBlock)
                    {
                        sizes.put(String.valueOf(currentBlock), positionY + rowHeight + 5);
                        rowHeight = 0;
                        positionX = 0;
                        positionY = 0;
                        currentBlock = characterBlock;
                    }

                    if (positionX + fontImage.getWidth() >= resolution * 10)
                    {
                        positionY += rowHeight + 10;
                        positionX = 0;
                        rowHeight = 0;
                    }

                    if (fontImage.getHeight() > rowHeight)
                        rowHeight = fontImage.getHeight();

                    info.glyphs.put(c, new Glyph(c, positionX, positionY, fontImage.getWidth(), fontImage.getHeight(), c != ' ' ? 1 : 0, yoffset, 0));
                    blocks.put(blockName, new glyph(positionX, positionY, fontImage));

                    positionX += fontImage.getWidth() + 10;
                }

                sizes.put(String.valueOf(currentBlock), positionY + rowHeight + 5);
                for (String block : blocks.keySet())
                {
                    Collection<glyph> glyphs = blocks.get(block);
                    Integer height = sizes.get(block);

                    int width = resolution * 10;

                    BufferedImage img = new BufferedImage(width + width % 2, height > 0 ? height : f.getSize(), BufferedImage.TYPE_INT_ARGB);
                    Graphics g = img.getGraphics();

                    g.setColor(Color.WHITE);
                    glyphs.forEach(glyph -> g.drawImage(glyph.image, glyph.x, glyph.y, null));

                    ByteArrayOutputStream os = new ByteArrayOutputStream();

                    ImageIO.write(img, "png", os);
                    InputStream is = new ByteArrayInputStream(os.toByteArray());

                    info.blockMaps.put(block, NativeImage.read(is));
                }

                FontHelper.fontInfo.put(font.getRegistryName(), info);
            }
            catch (Exception e)
            {
                OrmoyoUtil.LOGGER.fatal("Failed to create font " + font);
                e.printStackTrace();
            }
        }
        catch (IOException | FontFormatException e)
        {
            e.printStackTrace();
        }
    }

    private static BufferedImage getFontImage(Font font, char ch, boolean antiAliasing)
    {
        BufferedImage tempfontImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) tempfontImage.getGraphics();

        if (antiAliasing)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        int charwidth = Math.max(fm.charWidth(ch), 1);
        int charheight = fm.getHeight();

        if (charheight <= 0)
            charheight = font.getSize();

        BufferedImage fontImage = new BufferedImage(charwidth, charheight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt = (Graphics2D) fontImage.getGraphics();

        if (antiAliasing)
            gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

        gt.setFont(font);
        gt.setColor(Color.WHITE);

        int charx = 0;
        int chary = 0;

        gt.drawString(String.valueOf(ch), charx, chary + fm.getAscent());
        return fontImage;
    }

    private static BufferedImage Crop(BufferedImage image)
    {
        int minY = 0, maxY = 0, minX = Integer.MAX_VALUE, maxX = 0;
        boolean isBlank, isAllZero = true, minYIsDefined = false;
        int[] arr = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int yoff = 0;
        int off;
        for (int y = 0; y < image.getHeight(); y++, yoff += image.getWidth())
        {
            isBlank = true;
            off = yoff;
            for (int x = 0; x < image.getWidth(); x++)
            {
                int pixel = arr[off++];
                if (pixel != 0)
                {
                    isAllZero = false;
                    isBlank = false;

                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }

                if (!isBlank)
                {
                    if (!minYIsDefined)
                    {
                        minY = y;
                        minYIsDefined = true;
                    }
                    else
                    {
                        if (y > maxY) maxY = y;
                    }
                }
            }
        }

        if (maxX < minX)
            maxX = minX;
        if (maxY < minY)
            maxY = minY;

        return isAllZero ? image : image.getSubimage(MathHelper.clamp(minX, 0, image.getWidth()), MathHelper.clamp(minY, 0, image.getHeight()), MathHelper.clamp(maxX - minX + 1, 0, image.getWidth()), MathHelper.clamp(maxY - minY + 1, 0, image.getHeight()));
    }

    private static int getEmptyY(BufferedImage image)
    {
        int[] arr = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int minY = 0;
        boolean isBlank, yIsDefined = false;
        int yoff = 0;
        int off;
        for (int y = 0; y < image.getHeight(); y++, yoff += image.getWidth())
        {
            isBlank = true;
            off = yoff;
            for (int x = 0; x < image.getWidth(); x++)
            {
                int pixel = arr[off++];
                if (pixel != 0)
                    isBlank = false;
                if (!isBlank)
                {
                    if (!yIsDefined)
                    {
                        minY = y;
                        yIsDefined = true;
                    }
                }
            }
        }
        return minY;
    }
}
