package com.ormoyo.ormoyoutil.util.resourcelocation;

import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import com.ormoyo.ormoyoutil.util.Utils.Filter;
import net.minecraft.util.ResourceLocation;

/**
 * When used the specificed OpenGL filtering will be applied
 * <br>
 * <br>
 * You will need to use {@link RenderHelper} for the filtering to be applied
 */
public class FilterResourceLocation extends ResourceLocation
{
    private final Filter scaledUpFilter;
    private final Filter scaledDownFilter;

    public FilterResourceLocation(String resourceName, Filter scaledUpFilter, Filter scaledDownFilter)
    {
        super(resourceName);

        this.scaledUpFilter = scaledUpFilter;
        this.scaledDownFilter = scaledDownFilter;
    }

    public FilterResourceLocation(String resourceDomainIn, String resourcePathIn, Filter scaledUpFilter, Filter scaledDownFilter)
    {
        super(resourceDomainIn, resourcePathIn);

        this.scaledUpFilter = scaledUpFilter;
        this.scaledDownFilter = scaledDownFilter;
    }

    public Filter getScaledUpFilter()
    {
        return this.scaledUpFilter;
    }

    public Filter getScaledDownFilter()
    {
        return this.scaledDownFilter;
    }
}
