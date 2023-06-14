package com.ormoyo.ormoyoutil.util;

import com.google.common.collect.Sets;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.*;

public class EntityUtils
{
    public static double getYawBetweenEntities(Entity a, Entity b)
    {
        return Math.atan2(a.getPosZ() - b.getPosZ(), a.getPosX() - b.getPosX()) * (180 / Math.PI) + 90;
    }

    public static double getPitchBetweenEntities(Entity a, Entity b)
    {
        double dx = a.getPosX() - b.getPosX();
        double dz = a.getPosZ() - b.getPosZ();

        return Math.atan2((a.getPosY() + a.getEyeHeight()) - (b.getPosY() + (b.getHeight() / 2.0F)), Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI;
    }

    public static Entity raytraceEntityFromEntity(Entity entity, float reachDistance)
    {
        return EntityUtils.raytraceEntityFromEntity(entity, reachDistance, Entity.class);
    }

    public static<T extends Entity> T raytraceEntityFromEntity(Entity entity, float reachDistance, Class<T> entityClass)
    {
        Vector3d vec = EntityUtils.getLookedAtPoint(entity, reachDistance);
        EntityRayResult<T> result = EntityUtils.raytraceEntities(entityClass, entity.world, new Vector3d(entity.getPosX(), entity.getPosY() + entity.getEyeHeight(), entity.getPosZ()), vec);

        if (result.entities.isEmpty())
            return null;

        return result.entities.stream().min(Comparator.comparingDouble((Entity e) -> entity.getDistance(e))).get();
    }

    public static EntityRayResult<?> raytraceEntities(World world, Vector3d from, Vector3d to)
    {
        return EntityUtils.raytraceEntities(Entity.class, world, from, to);
    }

    public static <T extends Entity> EntityRayResult<T> raytraceEntities(Class<T> entityClass, World world, Vector3d from, Vector3d to)
    {
        return EntityUtils.raytraceEntities(entityClass, world, new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, null));
    }

    public static EntityRayResult<?> raytraceEntities(World world, RayTraceContext context)
    {
        return EntityUtils.raytraceEntities(Entity.class, world, context);
    }

    public static <T extends Entity> EntityRayResult<T> raytraceEntities(Class<T> entityClass, World world, RayTraceContext context)
    {
        EntityRayResult<T> result = new EntityRayResult<>();
        result.setBlockHit(world.rayTraceBlocks(context));

        double collidePosX;
        double collidePosY;
        double collidePosZ;

        if (result.getBlockHit() != null)
        {
            collidePosX = result.getBlockHit().getHitVec().getX();
            collidePosY = result.getBlockHit().getHitVec().getY();
            collidePosZ = result.getBlockHit().getHitVec().getZ();
        }
        else
        {
            collidePosX = 30;
            collidePosY = 30;
            collidePosZ = 30;
        }

        double x = context.getStartVec().getX();
        double y = context.getStartVec().getY();
        double z = context.getStartVec().getZ();

        AxisAlignedBB boundingBox = new AxisAlignedBB(
                Math.min(x, collidePosX),
                Math.min(y, collidePosY),
                Math.min(z, collidePosZ),
                Math.max(x, collidePosX),
                Math.max(y, collidePosY),
                Math.max(z, collidePosZ)).grow(1, 1, 1);

        List<T> entities = world.getEntitiesWithinAABB(entityClass, boundingBox);
        for (T entity : entities)
        {
            float pad = entity.getCollisionBorderSize() + 0.5f;
            AxisAlignedBB aabb = entity.getBoundingBox().grow(pad, pad, pad);

            if (aabb.contains(context.getStartVec()))
            {
                result.addEntityHit(entity);
            }
            else if (aabb.intersects(boundingBox))
            {
                result.addEntityHit(entity);
            }
        }

        return result;
    }

    public static Vector3d getLookedAtPoint(Entity looker, float reachDistance)
    {
        Vector3d pos = new Vector3d(looker.getPosX(), looker.getPosY() + looker.getEyeHeight(), looker.getPosZ());
        Vector3d look = looker.getLook(1.0F);

        return pos.add(look.x * reachDistance, look.y * reachDistance, look.z * reachDistance);
    }

    public static class EntityRayResult<T extends Entity>
    {
        private RayTraceResult blockHit;
        private final Set<T> entities = Sets.newHashSet();

        public RayTraceResult getBlockHit()
        {
            return blockHit;
        }

        public void setBlockHit(RayTraceResult blockHit)
        {
            this.blockHit = blockHit;
        }

        public void addEntityHit(T entity)
        {
            entities.add(entity);
        }

        public Collection<T> getEntityHits()
        {
            return Collections.unmodifiableSet(this.entities);
        }
    }
}
