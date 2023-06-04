package com.ormoyo.ormoyoutil.util.resourcelocation;

import com.ormoyo.ormoyoutil.client.ITextureMethod;
import net.minecraft.util.ResourceLocation;

public class TextureObjectResourceLocation extends ResourceLocation
{
    private final ITextureMethod texture;

    public TextureObjectResourceLocation(String resourceName, ITextureMethod texture)
    {
        super(resourceName);
        this.texture = texture;
    }

    public TextureObjectResourceLocation(String resourceDomainIn, String resourcePathIn, ITextureMethod texture)
    {
        super(resourceDomainIn, resourcePathIn);
        this.texture = texture;
    }

    public ITextureMethod getTexture()
    {
        return this.texture;
    }
}
