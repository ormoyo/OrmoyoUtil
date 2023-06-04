package com.ormoyo.ormoyoutil.util;

import com.ormoyo.ormoyoutil.client.font.Font;
import com.ormoyo.ormoyoutil.client.font.FontLoader;
import com.ormoyo.ormoyoutil.client.model.obj.OBJLoader;
import com.ormoyo.ormoyoutil.client.model.obj.OBJModel;
import com.ormoyo.ormoyoutil.client.model.obj.OBJModelEntry;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;

import java.util.Collection;
import java.util.function.Predicate;

public class OrmoyoResourcePackListener implements ISelectiveResourceReloadListener
{
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
    {
        if (resourcePredicate.test(OrmoyoResourceTypes.OBJ_MODELS))
        {
            loadOBJModels();
        }
        if (resourcePredicate.test(VanillaResourceType.TEXTURES))
        {
            loadFonts();
        }
    }

    private static void loadOBJModels()
    {
        Collection<OBJModelEntry> entries = OBJModel.getObjModelRegistery().getValuesCollection();
        ProgressBar bar = ProgressManager.push("Loading Obj Models", entries.size(), true);
        for (OBJModelEntry entry : entries)
        {
            bar.step("Loading");
            ResourceLocation registryName = entry.getRegistryName();
            OBJModel model = OBJLoader.loadOBJModel(new ResourceLocation(registryName.getNamespace(), "models/" + registryName.getPath()), entry.removeDuplicateVertices(), entry.hasJsonFile());
            ObfuscationReflectionHelper.setPrivateValue(OBJModelEntry.class, entry, model, "model");
        }
        ProgressManager.pop(bar);
    }

    private static void loadFonts()
    {
        Collection<Font> entries = Font.getFontRegistry().getValuesCollection();
        ProgressBar bar = ProgressManager.push("Loading Custom Fonts", entries.size(), true);
        for (Font entry : entries)
        {
            bar.step(entry.getRegistryName().toString());
            FontLoader.loadFont(entry);
        }
        ProgressManager.pop(bar);

    }
}