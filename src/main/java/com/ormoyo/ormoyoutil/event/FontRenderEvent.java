package com.ormoyo.ormoyoutil.event;

import com.ormoyo.ormoyoutil.client.font.Font;
import com.ormoyo.ormoyoutil.client.font.FontHelper;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Called when a font is being rendered at {@link FontHelper}
 */
public class FontRenderEvent extends Event
{
    protected String text;
    protected Font font;

    @Cancelable
    public static class Pre extends FontRenderEvent
    {

        public Pre(String text, Font font)
        {
            this.text = text;
            this.font = font;
        }

        public void setFont(Font font)
        {
            this.font = font;
        }

        public void setText(String text)
        {
            this.text = text;
        }
    }

    public static class Post extends FontRenderEvent
    {
        public Post(String text, Font font)
        {
            this.text = text;
            this.font = font;
        }
    }

    public String getText()
    {
        return this.text;
    }

    public Font getFont()
    {
        return this.font;
    }
}
