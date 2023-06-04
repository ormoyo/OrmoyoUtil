package com.ormoyo.ormoyoutil.client.font;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

public class Font extends ForgeRegistryEntry<Font>
{
    public static final Font ARIEL = new Font(new ResourceLocation(OrmoyoUtil.MODID, "ariel.ttf"), 64);
    public static final Font MINECRAFT = new Font(new ResourceLocation(OrmoyoUtil.MODID, "minecraft.ttf"), 32, false);

    private static IForgeRegistry<Font> FONT_REGISTRY;

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
        this.setRegistryName(name);

        this.resolution = resolution;
        this.antiAliasing = antiAliasing;
    }

    public Font(String name, int resolution, boolean antiAliasing)
    {
        this.setRegistryName(new ResourceLocation(name));

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
        return this.getRegistryName().toString();
    }

    public static IForgeRegistry<Font> getFontRegistry()
    {
        return FONT_REGISTRY;
    }

    public enum FontType
    {
        TTF,
        OTF
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID, bus = EventBusSubscriber.Bus.MOD)
    private static class EventHandler
    {
        @SubscribeEvent
        public static void onNewRegistry(RegistryEvent.NewRegistry event)
        {
            FONT_REGISTRY = new RegistryBuilder<Font>().setName(new ResourceLocation(OrmoyoUtil.MODID, "font")).setType(Font.class).setIDRange(0, 2048).create();
        }

        @SubscribeEvent
        public static void registerFonts(RegistryEvent.Register<Font> event)
        {
            event.getRegistry().register(ARIEL);
            event.getRegistry().register(MINECRAFT);
        }
    }
}
