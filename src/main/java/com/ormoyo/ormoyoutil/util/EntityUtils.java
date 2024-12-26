package com.ormoyo.ormoyoutil.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
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

    public static List<Entity> getEntitiesNearbyEntity(Entity entity, float radius)
    {
        return EntityUtils.getEntitiesNearbyEntity(Entity.class, entity, radius);
    }

    public static <T extends Entity> List<T> getEntitiesNearbyEntity(Class<T> entitiesType, Entity entity, float radius)
    {
        AxisAlignedBB bb = radius == 0 ? entity.getBoundingBox() : entity.getBoundingBox().grow(radius, radius, radius);
        return entity.getEntityWorld().getEntitiesWithinAABB(entitiesType, bb, e -> e != entity);
    }

    public static Entity raytraceEntityFromEntity(Entity entity, float reachDistance)
    {
        return EntityUtils.raytraceEntityFromEntity(Entity.class, entity, reachDistance);
    }

    public static <T extends Entity> T raytraceEntityFromEntity(Class<T> entityClass, Entity entity, float reachDistance)
    {
        Vector3d vec = EntityUtils.getLookedAtPoint(entity, reachDistance);
        EntityRayResult<T> result = EntityUtils.raytraceEntities(entityClass, entity.getEntityWorld(), new Vector3d(entity.getPosX(), entity.getPosY() + entity.getEyeHeight(), entity.getPosZ()), vec);

        if (result.entities.isEmpty())
            return null;

        Optional<T> optional = result.entities
                .stream()
                .filter(e -> e != entity)
                .min(Comparator.comparingDouble((Entity e) -> entity.getDistance(e)));

        return optional.orElse(null);
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
        BlockRayTraceResult blockHit = world.rayTraceBlocks(context);

        double collidePosX;
        double collidePosY;
        double collidePosZ;

        if (blockHit.getType() != RayTraceResult.Type.MISS)
        {
            collidePosX = blockHit.getHitVec().getX();
            collidePosY = blockHit.getHitVec().getY();
            collidePosZ = blockHit.getHitVec().getZ();
        }
        else
        {
            collidePosX = Math.abs(context.getEndVec().getX());
            collidePosY = Math.abs(context.getEndVec().getY());
            collidePosZ = Math.abs(context.getEndVec().getZ());
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
        EntityRayResult<T> result = new EntityRayResult<>(entities.size());

        for (T entity : entities)
        {
            float pad = entity.getCollisionBorderSize() + 0.5f;
            AxisAlignedBB aabb = entity.getBoundingBox().grow(pad, pad, pad);

            if (aabb.contains(context.getStartVec()))
                result.addEntityHit(entity);
            else if (aabb.intersects(boundingBox))
                result.addEntityHit(entity);
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
        private final List<T> entities;

        public EntityRayResult(int capacity)
        {
            this.entities = new ArrayList<>(capacity);
        }

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
            return Collections.unmodifiableCollection(this.entities);
        }
    }
}
