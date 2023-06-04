package com.ormoyo.ormoyoutil.client.font;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;
import net.minecraftforge.registries.RegistryBuilder;

public class Font extends Impl<Font>
{
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
        Init(name);
        this.setRegistryName(checkResourceLocation(name));
        this.resolution = resolution;
        this.antiAliasing = antiAliasing;
    }

    public Font(String name, int resolution, boolean antiAliasing)
    {
        String[] arr = ResourceLocation.splitObjectName(name);
        Init(new ResourceLocation(arr[0], arr[1]));
        this.setRegistryName(checkResourceLocation(name));
        this.resolution = resolution;
        this.antiAliasing = antiAliasing;
    }

    private void Init(ResourceLocation name)
    {
        int i = name.getPath().lastIndexOf('.');
        if (i > -1)
        {
            String t = name.getPath().substring(i > -1 ? i + 1 : 0);
            try
            {
                this.type = FontType.valueOf(t.toUpperCase());
            }
            catch (IllegalArgumentException e)
            {
                OrmoyoUtil.LOGGER.error("Font of type " + t + " doesn't exist");
            }
        }
        else
        {
            OrmoyoUtil.LOGGER.error("The font " + this.getRegistryName() + " doesn't have a type");
        }
    }

    private ResourceLocation checkResourceLocation(ResourceLocation location)
    {
        return new ResourceLocation(location.getNamespace(), "font/" + location.getPath());
    }

    private String checkResourceLocation(String location)
    {
        int i = location.indexOf(':');
        if (i > -1)
        {
            String newLoc = location.substring(0, i + 1) + "font/" + location.substring(i + 1);
            return newLoc;
        }
        return location;
    }

    public int getResolution()
    {
        return this.resolution;
    }

    public boolean isAntiAliasing()
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

    public static final Font ARIEL = new Font(new ResourceLocation(OrmoyoUtil.MODID, "ariel.ttf"), 64);
    public static final Font MINECRAFT = new Font(new ResourceLocation(OrmoyoUtil.MODID, "minecraft.ttf"), 32, false);

    private static IForgeRegistry<Font> FONT_REGISTRY;

    public static IForgeRegistry<Font> getFontRegistry()
    {
        return FONT_REGISTRY;
    }

    public enum FontType
    {
        TTF,
        OTF
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID)
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
