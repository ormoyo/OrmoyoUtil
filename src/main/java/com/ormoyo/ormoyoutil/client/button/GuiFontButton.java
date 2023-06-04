package com.ormoyo.ormoyoutil.client.button;

import com.ormoyo.ormoyoutil.client.font.Font;
import com.ormoyo.ormoyoutil.client.font.FontHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

@SideOnly(Side.CLIENT)
public class GuiFontButton extends GuiBetterButton
{
    public double scale;

    public Font font;
    public int color;

    public GuiFontButton(Font font, int buttonId, double x, double y, double widthIn, double heightIn, String buttonText, double scale, int color)
    {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.font = font;
        this.scale = scale;
        this.color = color;
    }

    public GuiFontButton(Font font, int buttonId, double x, double y, double widthIn, double heightIn, String buttonText, int color)
    {
        this(font, buttonId, x, y, widthIn, heightIn, buttonText, 1, color);
    }

    public GuiFontButton(Font font, int buttonId, double x, double y, double widthIn, double heightIn, String buttonText)
    {
        this(font, buttonId, x, y, widthIn, heightIn, buttonText, -1);
    }

    public GuiFontButton(Font font, int buttonId, double x, double y, String buttonText, double scale, int color)
    {
        this(font, buttonId, x, y, FontHelper.getStringWidth(font, buttonText, 200) * scale, FontHelper.getStringHeight(font, buttonText, 200) * scale, buttonText, scale, color);
    }

    public GuiFontButton(Font font, int buttonId, double x, double y, String buttonText, double scale)
    {
        this(font, buttonId, x, y, buttonText, scale, -1);
    }

    public GuiFontButton(Font font, int buttonId, double x, double y, String buttonText, int color)
    {
        this(font, buttonId, x, y, buttonText, 1d);
    }

    public GuiFontButton(Font font, int buttonId, double x, double y, String buttonText)
    {
        this(font, buttonId, x, y, buttonText, -1);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            int prevColor = this.color;
            if (!this.enabled)
            {
                this.color = Color.GRAY.getRGB();
            }

            if (this.hovered)
            {
                this.color = Color.YELLOW.getRGB();
            }
            else
            {
                this.color = prevColor;
            }

            FontHelper.drawString(this.font, this.displayString, this.xD, this.yD, this.scale, this.color, 200);

            this.color = prevColor;
            this.hovered = super.isMouseOver();
        }
    }
}
