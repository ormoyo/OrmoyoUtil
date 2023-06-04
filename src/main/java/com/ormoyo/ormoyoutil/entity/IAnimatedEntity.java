package com.ormoyo.ormoyoutil.entity;

import com.ormoyo.ormoyoutil.client.animation.ModelAnimation;

public interface IAnimatedEntity
{
    void onAnimationChange(ModelAnimation animation);

    void onAnimationStart(ModelAnimation animation);

    void onAnimationUpdate(ModelAnimation animation);

    void onAnimationEnd(ModelAnimation animation);
}
