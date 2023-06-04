package com.ormoyo.ormoyoutil.client.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ormoyo.ormoyoutil.animation.AnimationHandler;
import com.ormoyo.ormoyoutil.animation.AnimationHandler.Animation;
import com.ormoyo.ormoyoutil.client.animation.ModelAnimation;
import com.ormoyo.ormoyoutil.entity.IAnimatedEntity;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class AnimatedModelBase extends ModelBase
{
    public static final ModelAnimation IDLE = new ModelAnimation();

    protected final BiMap<String, ModelAnimation> animations = HashBiMap.create();
    protected ModelAnimation currentAnimation = IDLE;

    public void playAnimation(String name)
    {
        if (!this.animations.containsKey(name))
        {
            return;
        }
        this.currentAnimation = this.animations.get(name);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        if (entity instanceof IAnimatedEntity)
        {
            Animation animation = AnimationHandler.INSTANCE.getEntityAnimation((IAnimatedEntity) entity);
            String name = this.animations.inverse().get(this.currentAnimation);

            if (!animation.getAnimationName().equals(name))
            {
                this.currentAnimation = this.animations.getOrDefault(animation.getAnimationName(), IDLE);
            }

            this.currentAnimation.animate(this);
            this.currentAnimation.setTick(entity.ticksExisted - animation.getTick());
        }

        super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }
}
