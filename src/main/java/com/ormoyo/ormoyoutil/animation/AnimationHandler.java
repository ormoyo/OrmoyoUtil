package com.ormoyo.ormoyoutil.animation;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.entity.IAnimatedEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.Objects;

public enum AnimationHandler
{
    INSTANCE;

    private final Map<IAnimatedEntity, Animation> playingAnimations = Maps.newHashMap();

    public <T extends Entity & IAnimatedEntity> void setEntityAnimation(T entity, String animation)
    {
        this.setEntityAnimation(entity, animation, true);
    }

    public <T extends Entity & IAnimatedEntity> void setEntityAnimation(T entity, String animation, boolean updateClients)
    {
        Animation anim = new Animation(entity, animation);
        this.playingAnimations.put(entity, anim);
    }

    @SideOnly(Side.CLIENT)
    public void setEntityAnimation(IAnimatedEntity entity, String animation)
    {
        Animation anim = new Animation(entity, animation);
        this.playingAnimations.put(entity, anim);
    }

    public <T extends Entity & IAnimatedEntity> Animation getEntityAnimation(T entity)
    {
        return this.playingAnimations.get(entity);
    }

    public Animation getEntityAnimation(IAnimatedEntity entity)
    {
        return this.playingAnimations.get(entity);
    }

    public class Animation
    {
        private final int entityId;

        private final boolean isClientSide;
        private final int worldId;

        private final String name;
        private final int tick;

        private Animation(IAnimatedEntity entity, String name)
        {
            Entity e = (Entity) entity;
            this.entityId = e.getEntityId();

            this.isClientSide = e.getEntityWorld().isRemote;
            this.name = name;

            int worldCount = -1;
            World[] worlds = FMLCommonHandler.instance().getMinecraftServerInstance().worlds;
            for (int i = 0; i < worlds.length; i++)
            {
                World world = worlds[i];
                if (world == e.getEntityWorld())
                {
                    worldCount = i;
                    break;
                }
            }

            this.worldId = this.isClientSide ? -1 : worldCount;
            this.tick = e.ticksExisted;
        }

        public String getAnimationName()
        {
            return this.name;
        }

        public int getAnimationTick()
        {
            if (!this.isClientSide && this.worldId < 0)
            {
                throw new NullPointerException("Entity doesn't exist anymore");
            }

            Entity entity = this.isClientSide ? OrmoyoUtil.PROXY.getClientPlayer().getEntityWorld().getEntityByID(this.entityId) : FMLCommonHandler.instance().getMinecraftServerInstance().worlds[this.worldId].getEntityByID(this.entityId);

            Objects.requireNonNull(entity, "Entity doesn't exist anymore");
            return entity.ticksExisted - this.tick;
        }

        @SideOnly(Side.CLIENT)
        public int getTick()
        {
            return this.tick;
        }
    }
}