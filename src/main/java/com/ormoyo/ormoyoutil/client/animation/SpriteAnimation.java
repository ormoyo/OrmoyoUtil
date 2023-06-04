package com.ormoyo.ormoyoutil.client.animation;

import com.google.common.collect.ImmutableList;
import com.ormoyo.ormoyoutil.client.render.IColorStretchRenderer;
import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import com.ormoyo.ormoyoutil.util.Utils;
import com.ormoyo.ormoyoutil.util.vec.Vec2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class SpriteAnimation implements IColorStretchRenderer
{
    private final List<Sprite> sprites;
    private final boolean loop;
    private final float fps;

    private int currentSpriteIndex;
    private float elapsedTime;

    public SpriteAnimation(float fps, Sprite... sprites)
    {
        this(fps, false, sprites);
    }

    public SpriteAnimation(float fps, boolean loop, Sprite... sprites)
    {
        this.sprites = ImmutableList.copyOf(sprites);
        this.loop = loop;
        this.fps = (float) 1 / fps;
    }

    public SpriteAnimation(ResourceLocation spriteSheet, int frameWidth, int frameHeight, float fps)
    {
        this(spriteSheet, frameWidth, frameHeight, fps, false);
    }

    public SpriteAnimation(ResourceLocation spriteSheet, int frameWidth, int frameHeight, float fps, boolean loop)
    {
        Vec2i size = RenderHelper.getTextureSize(spriteSheet);

        if (size.getY() < frameHeight)
        {
            throw new IllegalArgumentException("frame height cannot be bigger then the sprite sheet height");
        }
        if (size.getX() < frameWidth)
        {
            throw new IllegalArgumentException("frame width cannot be bigger then the sprite sheet width");
        }

        int maxW = size.getX() / frameWidth;
        int maxH = size.getY() / frameHeight;

        Sprite[] sprites = new Sprite[maxW * maxH];
        for (int h = 0; h < maxH; h++)
        {
            for (int w = 0; w < maxW; w++)
            {
                sprites[h * maxW + w] = new Sprite(spriteSheet, w * frameWidth, h * frameHeight, frameWidth, frameHeight);
            }
        }

        this.sprites = ImmutableList.copyOf(sprites);
        this.loop = loop;
        this.fps = (float) 1 / fps;
    }

    public void replay()
    {
        this.currentSpriteIndex = 0;
    }

    @Override
    public void render(double x, double y, float partialTicks, double scale, int color)
    {
        if (!Minecraft.getMinecraft().isGamePaused())
        {
            this.elapsedTime += Utils.getDeltaTime();
        }

        if (this.elapsedTime >= this.fps)
        {
            this.elapsedTime -= this.fps;
            this.currentSpriteIndex++;
        }

        if (this.currentSpriteIndex >= this.sprites.size())
        {
            if (this.loop)
            {
                this.currentSpriteIndex = 0;
            }
            else
            {
                this.currentSpriteIndex = this.sprites.size() - 1;
            }
        }

        Sprite currentSprite = this.sprites.get(this.currentSpriteIndex);
        currentSprite.render(x, y, partialTicks, scale, color);
    }

    @Override
    public void render(double x, double y, double width, double height, float partialTicks, double scale, int color)
    {
        if (!Minecraft.getMinecraft().isGamePaused())
        {
            this.elapsedTime += Utils.getDeltaTime();
        }

        if (this.elapsedTime >= this.fps)
        {
            this.elapsedTime -= this.fps;
            this.currentSpriteIndex++;
        }

        if (this.currentSpriteIndex >= this.sprites.size())
        {
            if (this.loop)
            {
                this.currentSpriteIndex = 0;
            }
            else
            {
                this.currentSpriteIndex = this.sprites.size() - 1;
            }
        }

        Sprite currentSprite = this.sprites.get(this.currentSpriteIndex);
        currentSprite.render(x, y, width, height, partialTicks, scale, color);
    }

    @Override
    public void renderToBatch(BufferBuilder batchBuilder, double x, double y, float partialTicks, double scale, int color)
    {
        if (!Minecraft.getMinecraft().isGamePaused())
        {
            this.elapsedTime += Utils.getDeltaTime();
        }

        if (this.elapsedTime >= this.fps)
        {
            this.elapsedTime -= this.fps;
            this.currentSpriteIndex++;
        }

        if (this.currentSpriteIndex >= this.sprites.size())
        {
            if (this.loop)
            {
                this.currentSpriteIndex = 0;
            }
            else
            {
                this.currentSpriteIndex = this.sprites.size() - 1;
            }
        }

        Sprite currentSprite = this.sprites.get(this.currentSpriteIndex);
        currentSprite.renderToBatch(batchBuilder, x, y, partialTicks, scale, color);
    }

    @Override
    public void renderToBatch(BufferBuilder batchBuilder, double x, double y, double width, double height, float partialTicks, double scale, int color)
    {
        if (!Minecraft.getMinecraft().isGamePaused())
        {
            this.elapsedTime += Utils.getDeltaTime();
        }

        if (this.elapsedTime >= this.fps)
        {
            this.elapsedTime -= this.fps;
            this.currentSpriteIndex++;
        }

        if (this.currentSpriteIndex >= this.sprites.size())
        {
            if (this.loop)
            {
                this.currentSpriteIndex = 0;
            }
            else
            {
                this.currentSpriteIndex = this.sprites.size() - 1;
            }
        }

        Sprite currentSprite = this.sprites.get(this.currentSpriteIndex);
        currentSprite.renderToBatch(batchBuilder, x, y, width, height, partialTicks, scale, color);
    }

    public Sprite getCurrentSprite()
    {
        return this.sprites.get(this.currentSpriteIndex);
    }

    public List<Sprite> getSprites()
    {
        return this.sprites;
    }
}
