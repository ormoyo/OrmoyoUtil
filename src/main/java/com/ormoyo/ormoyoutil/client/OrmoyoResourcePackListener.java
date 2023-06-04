package com.ormoyo.ormoyoutil.client;

import com.ormoyo.ormoyoutil.client.font.Font;
import com.ormoyo.ormoyoutil.client.font.FontLoader;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import java.util.Collection;
import java.util.function.Predicate;

public class OrmoyoResourcePackListener implements ISelectiveResourceReloadListener
{
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
    {
        if (resourcePredicate.test(VanillaResourceType.TEXTURES))
        {
            loadFonts();
        }
    }

    private static void loadFonts()
    {
        Collection<Font> entries = Font.getFontRegistry().getValues();
        for (Font entry : entries)
        {
            FontLoader.loadFont(entry);
        }
    }
}