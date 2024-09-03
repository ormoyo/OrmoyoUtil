package com.ormoyo.ormoyoutil.client.font;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistry;

public class Font extends ForgeRegistryEntry<Font>
{
    public static final Font ARIEL = new Font(new ResourceLocation(OrmoyoUtil.MODID, "ariel.ttf"), 64);
    public static final Font MINECRAFT = new Font(new ResourceLocation(OrmoyoUtil.MODID, "minecraft.ttf"), 32, false);

    private final int resolution;
    private final boolean antiAliasing;

    private FontType type;

    public Font(ResourceLocation name)
    {
        this(name, 32);
    }

    public Font(String name)
    {
        this(name, 32);
    }

    public Font(ResourceLocation name, int resolution)
    {
        this(name, resolution, true);
    }

    public Font(String name, int resolution)
    {
        this(name, resolution, true);
    }

    public Font(ResourceLocation name, boolean antiAliasing)
    {
        this(name, 32, antiAliasing);
    }

    public Font(String name, boolean antiAliasing)
    {
        this(name, 32, antiAliasing);
    }

    public Font(ResourceLocation name, int resolution, boolean antiAliasing)
    {
        this.setRegistryName(checkResourceLocation(name));

        this.resolution = resolution;
        this.antiAliasing = antiAliasing;
    }

    public Font(String name, int resolution, boolean antiAliasing)
    {
        this.setRegistryName(checkResourceLocation(new ResourceLocation(name)));

        this.resolution = resolution;
        this.antiAliasing = antiAliasing;
    }

    public int getResolution()
    {
        return this.resolution;
    }

    public boolean hasAntiAliasing()
    {
        return this.antiAliasing;
    }

    public FontType getType()
    {
        return this.type;
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.getRegistryName());
    }

    public static IForgeRegistry<Font> getFontRegistry()
    {
        return FontLoader.FONT_REGISTRY;
    }

    private static ResourceLocation checkResourceLocation(ResourceLocation location)
    {
        return new ResourceLocation(location.getNamespace(), "font/" + location.getPath());
    }

    public enum FontType
    {
        TTF,
        OTF
    }
}
