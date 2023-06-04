package com.ormoyo.ormoyoutil.client.label;

import com.google.common.collect.Lists;
import com.ormoyo.ormoyoutil.client.font.Font;
import com.ormoyo.ormoyoutil.client.font.FontHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiBetterLabel extends GuiLabel
{
    public final Font font;
    protected final List<String> labels = Lists.newArrayList();
    public double xD;
    public double yD;
    public double widthD;
    public double heightD;
    public boolean centeredX;
    public boolean centeredY;
    public double scale;
    public int color;
    private final boolean widthIsDefined;
    private final boolean heightIsDefined;

    public GuiBetterLabel(Font font, int id, double x, double y, int textColor)
    {
        super(FontHelper.createFontRendererFromFont(font), id, (int) x, (int) y, -1, -1, textColor);
        this.font = font;
        this.xD = x;
        this.yD = y;
        this.scale = 1;
        this.color = textColor;
        this.widthIsDefined = false;
        this.heightIsDefined = false;
    }

    public GuiBetterLabel(Font font, int id, double x, double y, double scale, int textColor)
    {
        super(FontHelper.createFontRendererFromFont(font), id, (int) x, (int) y, -1, -1, textColor);
        this.font = font;
        this.xD = x;
        this.yD = y;
        this.widthD = 200;
        this.heightD = 20;
        this.scale = scale;
        this.color = textColor;
        this.widthIsDefined = false;
        this.heightIsDefined = false;
    }

    public GuiBetterLabel(Font font, int id, double x, double y, double width, double height, int textColor)
    {
        this(font, id, x, y, width, height, 1, textColor);
    }

    public GuiBetterLabel(Font font, int id, double x, double y, double width, double height, double scale, int textColor)
    {
        super(FontHelper.createFontRendererFromFont(font), id, (int) x, (int) y, (int) width, (int) height, textColor);
        this.font = font;
        this.xD = x;
        this.yD = y;
        this.widthD = width;
        this.heightD = height;
        this.scale = scale;
        this.color = textColor;
        this.widthIsDefined = true;
        this.heightIsDefined = true;
    }

    @Override
    public void addLine(String line)
    {
        super.addLine(line);
		if (!this.widthIsDefined)
		{
			this.setWidth(Math.max(this.widthD, FontHelper.getStringWidth(this.font, line)));
		}
		if (!this.heightIsDefined)
		{
			this.setHeight(Math.max(this.heightD, FontHelper.getStringHeight(this.font, line, this.scale, this.widthD)));
		}
        this.labels.add(line);
    }

    @Override
    public GuiLabel setCentered()
    {
        this.centeredX = true;
        this.centeredY = true;
        return super.setCentered();
    }

    @Override
    public void drawLabel(Minecraft mc, int mouseX, int mouseY)
    {
        this.xD = this.x == (int) this.xD ? this.xD : this.x;
        this.yD = this.y == (int) this.yD ? this.yD : this.y;
        this.widthD = this.width == (int) this.widthD ? this.widthD : this.width;
        this.heightD = this.height == (int) this.heightD ? this.heightD : this.height;
        if (this.visible)
        {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            this.drawLabelBackground(mc, mouseX, mouseY);
            double height = 0;
            if (this.centeredY)
            {
                height = this.getHeight();
            }
            double offset = 0;
            for (Iterator<String> iterator = this.labels.iterator(); iterator.hasNext(); )
            {
                String text = iterator.next();
                double x = this.xD;
                double y = this.yD;
                if (this.centeredX)
                {
                    if (this.widthIsDefined)
                    {
                        double w = FontHelper.getStringWidth(this.font, text) * this.scale;
                        x = this.xD + (this.widthD - w) * 0.5;
                    }
                    else
                    {
                        double w = FontHelper.getStringWidth(this.font, text) * this.scale;
                        x = this.xD - w * 0.5;
                    }
                }
                if (this.centeredY)
                {
                    if (this.heightIsDefined)
                    {
                        double h = FontHelper.getStringHeight(this.font, text) * this.scale;
                        y = this.yD + (this.heightD - height - h) * 0.5;
                    }
                    else
                    {
                        double h = FontHelper.getStringHeight(this.font, text) * this.scale;
                        y = this.yD - (height - h) * 0.5;
                    }
                }
                FontHelper.drawString(this.font, text, x, y + offset, this.scale, this.color);
                offset += (FontHelper.getStringHeight(this.font, text) + (iterator.hasNext() ? 3 : 0)) * this.scale;
            }
        }
    }

    private double getHeight()
    {
        double height = 0;
        for (Iterator<String> iterator = this.labels.iterator(); iterator.hasNext(); )
        {
            String text = iterator.next();
            height += (FontHelper.getStringHeight(this.font, text) + 3 + (iterator.hasNext() ? 3 : 0)) * this.scale;
        }
        return height;
    }

    public void setWidth(double width)
    {
        this.width = (int) width;
        this.widthD = width;
    }

    public void setHeight(double height)
    {
        this.height = (int) height;
        this.heightD = height;
    }
}
