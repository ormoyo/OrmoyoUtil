package com.ormoyo.ormoyoutil.util.resourcelocation;

import com.ormoyo.ormoyoutil.client.font.Font;
import net.minecraft.util.SoundEvent;

public class IconResourceLocation extends ImageResourceLocation
{
    private final double scale;
    private final Font font;
    private final SoundEvent soundBeep;

    public IconResourceLocation(String resourceName, int u, int v, int width, int height, double scale, Font font, SoundEvent vocalBeep)
    {
        super(resourceName, u, v, width, height);
        this.scale = scale;
        this.font = font;
        this.soundBeep = vocalBeep;
    }

    public IconResourceLocation(String resourceDomainIn, String resourcePathIn, int u, int v, int width, int height, double scale, Font font, SoundEvent vocalBeep)
    {
        super(resourceDomainIn, resourcePathIn, u, v, width, height);
        this.scale = scale;
        this.font = font;
        this.soundBeep = vocalBeep;
    }

    public double getScale()
    {
        return this.scale;
    }

    public Font getFont()
    {
        return this.font;
    }

    public SoundEvent getVocalBeep()
    {
        return this.soundBeep;
    }
}
