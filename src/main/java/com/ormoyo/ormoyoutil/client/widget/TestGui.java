package com.ormoyo.ormoyoutil.client.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.ormoyo.ormoyoutil.client.RenderHelper;
import com.ormoyo.ormoyoutil.client.font.Font;
import com.ormoyo.ormoyoutil.client.font.FontHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

import java.awt.*;

public class TestGui extends Screen
{
    public TestGui()
    {
        super(ITextComponent.getTextComponentOrEmpty("Test"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderBackground(matrixStack);

        int i = (this.width - 160) / 2;
        int j = (this.height - 160) / 2;

        RenderHelper.drawTexturedRect(RenderHelper.WHITE, i, j, 0, 0, 16, 16, 10, Color.BLUE);
        FontHelper.drawString(Font.ARIEL, "afafasgdgthr", i, j + 40, 1, Color.WHITE, 200, FontHelper.TextAlignment.CENTER);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
