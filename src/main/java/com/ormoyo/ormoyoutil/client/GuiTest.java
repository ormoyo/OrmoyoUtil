package com.ormoyo.ormoyoutil.client;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.client.button.GuiBetterButton;
import com.ormoyo.ormoyoutil.client.button.GuiFontButton;
import com.ormoyo.ormoyoutil.client.font.Font;
import com.ormoyo.ormoyoutil.client.font.FontHelper;
import com.ormoyo.ormoyoutil.client.font.FontHelper.TextAlignment;
import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import com.ormoyo.ormoyoutil.network.MessageTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.IOException;

public class GuiTest extends GuiScreen
{
    private static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation("textures/gui/widgets.png");
    String string = "";
    int t = 1;
    int maxT = 10;
    boolean b;
    int offX;
    int offY;

    @Override
    public void initGui()
    {
        super.initGui();
        int i = (this.width - 160) / 2;
        int j = (this.height - 160) / 2;
        this.buttonList.add(new GuiFontButton(Font.ARIEL, 0, i + 160 / 2 - FontHelper.getStringWidth(Font.ARIEL, "The quick brown fox jumps over the lazy dog.") / 2, j, "The quick brown fox jumps over the lazy dog.", 1));
        this.buttonList.add(new GuiBetterButton(1, i, j + 60, 100, 40, "bla"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        int i = (this.width - 160) / 2;
        int j = (this.height - 160) / 2;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.drawTexturedRect(RenderHelper.WHITE, i, j, 0, 0, 16, 16, 10, Color.BLUE);
        FontHelper.drawString(Font.ARIEL, this.string, i, j + 40, 1, Color.WHITE, 200, TextAlignment.CENTER);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        super.actionPerformed(button);
        if (button.id == 1)
        {
            OrmoyoUtil.NETWORK_WRAPPER.sendToServer(new MessageTest(Minecraft.getMinecraft().player));
        }
    }

    @Override
    public void updateScreen()
    {
        /**
         super.updateScreen();
         int a = (this.b ? this.t-- : this.t++);
         double t = (double) 1/this.maxT * a;
         int i = Utils.lerp(0, 100, t);
         this.buttonList.get(1).setWidth(i);
         if(this.t >= this.maxT || this.t <= 1) {
         this.b = !this.b;
         }
         */
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);
        if (typedChar == '\b')
        {
            this.string = this.string.substring(0, Math.max(this.string.length() - 1, 0));
            return;
        }
        System.out.println("u+" + Integer.toHexString(typedChar | 0x10000).substring(1));
    }
}
