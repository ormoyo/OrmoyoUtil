package com.ormoyo.ormoyoutil.client.animation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;
import com.ormoyo.ormoyoutil.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

@SideOnly(Side.CLIENT)
public class ModelAnimation
{
    private final ImmutableMap<Integer, Bone> keyframes;
    private int tick;


    public ModelAnimation(Keyframe... keyframes)
    {
        Builder<Integer, Bone> builder = ImmutableMap.builder();
        Multimap<Integer, Keyframe> bones = ArrayListMultimap.create();

        for (Keyframe keyframe : keyframes)
        {
            bones.put(keyframe.getBodyPartIndex(), keyframe);
        }

        for (Integer key : bones.keySet())
        {
            Collection<Keyframe> collection = bones.get(key);
            builder.put(key, new Bone(collection.toArray(new Keyframe[0])));
        }

        this.keyframes = builder.build();
    }

    public void animate(ModelBase model)
    {
        float tick = this.tick + Minecraft.getMinecraft().getRenderPartialTicks();
        for (int i = 0; i < model.boxList.size(); i++)
        {
            ModelRenderer renderer = model.boxList.get(i);
            Bone bone = this.keyframes.get(i);

            if (bone.currentKeyframe >= bone.keyframes.length - 1)
            {
                continue;
            }

            Keyframe previousKeyframe = bone.keyframes[bone.currentKeyframe];
            Keyframe keyframe = bone.keyframes[bone.currentKeyframe + 1];

            if (keyframe.getTime() >= tick)
            {
                bone.currentKeyframe++;
                if (bone.currentKeyframe >= bone.keyframes.length - 1)
                {
                    continue;
                }

                previousKeyframe = keyframe;
                keyframe = bone.keyframes[bone.currentKeyframe + 1];
            }

            float t = Utils.mapValue(tick, previousKeyframe.getTime(), keyframe.getTime(), 0f, 1f);

            renderer.offsetX = (float) Utils.lerp(previousKeyframe.getPosition().x, keyframe.getPosition().x, t);
            renderer.offsetY = (float) Utils.lerp(previousKeyframe.getPosition().y, keyframe.getPosition().y, t);
            renderer.offsetZ = (float) Utils.lerp(previousKeyframe.getPosition().z, keyframe.getPosition().z, t);

            renderer.rotateAngleX = (float) Utils.lerp(previousKeyframe.getRotation().x, keyframe.getRotation().x, t);
            renderer.rotateAngleY = (float) Utils.lerp(previousKeyframe.getRotation().y, keyframe.getRotation().y, t);
            renderer.rotateAngleZ = (float) Utils.lerp(previousKeyframe.getRotation().z, keyframe.getRotation().z, t);
        }
    }

    public void setTick(int tick)
    {
        this.tick = tick;
    }

    private class Bone
    {
        private final Keyframe[] keyframes;
        private int currentKeyframe;

        private Bone(Keyframe... keyframes)
        {
            this.keyframes = keyframes;
        }
    }
}
