package com.ormoyo.ormoyoutil.client.animation;

import net.minecraft.util.math.Vec3d;

import javax.annotation.concurrent.Immutable;

@Immutable
public class Keyframe
{
    private final int time;
    private final int bodyPartIndex;

    private final Vec3d position;
    private final Vec3d rotation;
    private final Vec3d scale;

    public Keyframe(int bodyPartIndex, int time, Vec3d position, Vec3d rotation, Vec3d scale)
    {
        this.time = time;
        this.bodyPartIndex = bodyPartIndex;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public int getTime()
    {
        return this.time;
    }

    public Vec3d getPosition()
    {
        return this.position;
    }

    public Vec3d getRotation()
    {
        return this.rotation;
    }

    public Vec3d getScale()
    {
        return this.scale;
    }

    public int getBodyPartIndex()
    {
        return this.bodyPartIndex;
    }
}
