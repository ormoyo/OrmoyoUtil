package com.ormoyo.ormoyoutil.client;

import com.ormoyo.ormoyoutil.util.vec.Vec2i;
import net.minecraft.client.renderer.texture.ITextureObject;

public interface ITextureMethod extends ITextureObject
{
    Vec2i getTextureSize();
}
