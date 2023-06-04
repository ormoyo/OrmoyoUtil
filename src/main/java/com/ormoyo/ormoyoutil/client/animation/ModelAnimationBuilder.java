package com.ormoyo.ormoyoutil.client.animation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SideOnly(Side.CLIENT)
public final class ModelAnimationBuilder
{
    private final Table<Integer, Integer, AnimationAction> actions = HashBasedTable.create();
    private final Table<Integer, ActionType, Integer> durations = HashBasedTable.create();

    public static ModelAnimationBuilder create()
    {
        return new ModelAnimationBuilder();
    }

    public ModelAnimationBuilder move(int boneId, Vec3d position, int duration)
    {
        Integer a = this.durations.get(boneId, ActionType.POSITION);

        int d = a == null ? 0 : a;
        AnimationAction action = this.actions.get(boneId, d);

        if (action == null)
        {
            action = new AnimationAction();
            this.actions.put(boneId, d, action);
        }

        action.position = position;
        this.durations.put(boneId, ActionType.POSITION, d + duration);

        return this;
    }

    public ModelAnimationBuilder rotate(int boneId, Vec3d rotation, int duration)
    {
        Integer a = this.durations.get(boneId, ActionType.ROTATION);

        int d = a == null ? 0 : a;
        AnimationAction action = this.actions.get(boneId, d);

        if (action == null)
        {
            action = new AnimationAction();
            this.actions.put(boneId, d, action);
        }

        action.rotation = rotation;
        this.durations.put(boneId, ActionType.ROTATION, d + duration);

        return this;
    }

    public ModelAnimationBuilder scale(int boneId, Vec3d scale, int duration)
    {
        Integer a = this.durations.get(boneId, ActionType.SCALE);

        int d = a == null ? 0 : a;
        AnimationAction action = this.actions.get(boneId, d);

        if (action == null)
        {
            action = new AnimationAction();
            this.actions.put(boneId, d, action);
        }

        action.scale = scale;
        this.durations.put(boneId, ActionType.SCALE, d + duration);

        return this;
    }

    public ModelAnimation build()
    {
        List<Keyframe> keyframes = Lists.newArrayList();
        for (int bone : this.actions.rowKeySet())
        {
            Map<Integer, AnimationAction> m = this.actions.row(bone);
            for (Entry<Integer, AnimationAction> entry : m.entrySet())
            {
                int time = entry.getKey();
                AnimationAction action = entry.getValue();

                keyframes.add(new Keyframe(bone, time, action.position, action.rotation, action.scale));
            }

            keyframes.sort(Comparator.comparingInt(Keyframe::getTime));
        }

        return new ModelAnimation(keyframes.toArray(new Keyframe[0]));
    }

    private static class AnimationAction
    {
        private Vec3d position;
        private Vec3d rotation;
        private Vec3d scale;
    }

    private enum ActionType
    {
        POSITION,
        ROTATION,
        SCALE
    }
}
