package com.ormoyo.ormoyoutil.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.awt.image.BufferedImage;
import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.objectweb.asm.Opcodes.*;

public final class Utils
{
    private static final CustomClassLoader LOADER = new CustomClassLoader();
    private static final Map<Method, IInvoker<?>> INVOKERS = Maps.newHashMap();

    private static final Map<Integer, ITick> TICKS = Maps.newHashMap();
    private static final Map<Integer, ITick> TEMP_TICKS = Maps.newHashMap();

    private static final Multimap<Class<? extends Event>, Consumer<Event>> EVENT_CONSUMERS = ArrayListMultimap.create();

    @SideOnly(Side.CLIENT)
    private static ScaledResolution RESOLUTION;
    @SideOnly(Side.CLIENT)
    private static float DELTA_TIME;

    @SuppressWarnings("unchecked")
    public static <T extends Event> void performConsumerOnEvent(Class<T> event, Consumer<T> consumer)
    {
        EVENT_CONSUMERS.put(event, (Consumer<Event>) consumer);
    }

    /**
     * Performs an action after amount of ticks
     *
     * @param action The action to perform
     * @param ticks  The amount of ticks to wait until the action occures
     * @return An id if canceling the operation is needed
     */
    public static int performActionAfterAmountOfTicks(Action action, int ticks)
    {
        int id = TICKS.size();
        TEMP_TICKS.put(id, new ActionPerform(action, ticks));

        return id;
    }

    public static void cancelAction(int id)
    {
        TEMP_TICKS.remove(id);
    }

    /**
     * Performs an action every tick for an amount of ticks
     *
     * @param action The action to perform
     * @param ticks  The amount of ticks to perform the action
     * @return An id if canceling the operation is needed
     * @apiNote You can set the ticks to a negetive value for the operation to never end unless canceled
     */
    public static int performActionForAmountOfTicks(Action action, int ticks)
    {
        int id = TICKS.size();
        TEMP_TICKS.put(id, new ActionPerform(action, ticks, false));

        return id;
    }

    @SideOnly(Side.CLIENT)
    public static void loadImageToTexture(int textureId, BufferedImage image, Filter scaledUpFilter, Filter scaledDownFilter, boolean clamp)
    {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }

        buffer.flip();

        GlStateManager.bindTexture(textureId);

        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, clamp ? GL_CLAMP : GL_REPEAT);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, clamp ? GL_CLAMP : GL_REPEAT);

        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scaledDownFilter.capability);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scaledUpFilter.capability);

        GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
		if (scaledDownFilter == Filter.LINEAR_MIPMAP_LINEAR ||
				scaledDownFilter == Filter.LINEAR_MIPMAP_NEAREST ||
				scaledDownFilter == Filter.NEAREST_MIPMAP_NEAREST)
		{
			GL30.glGenerateMipmap(GL_TEXTURE_2D);
		}
    }

    @SideOnly(Side.CLIENT)
    public static int interpolateInt(int value, int prevValue, float partialTicks)
    {
        return Utils.round(lerp(prevValue, value, partialTicks));
    }

    @SideOnly(Side.CLIENT)
    public static double interpolateDouble(double value, double prevValue, float partialTicks)
    {
        return lerp(prevValue, value, partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public static float interpolateFloat(float value, float prevValue, float partialTicks)
    {
        return lerp(prevValue, value, partialTicks);
    }

    @SideOnly(Side.CLIENT)
    public static Vec3d interpolateVec3d(Vec3d value, Vec3d prevValue, float partialTicks)
    {
        return new Vec3d(interpolateDouble(value.x, prevValue.x, partialTicks),
                interpolateDouble(value.x, prevValue.x, partialTicks),
                interpolateDouble(value.z, prevValue.z, partialTicks));
    }

    @SideOnly(Side.CLIENT)
    public static ScaledResolution getResolution()
    {
        return RESOLUTION;
    }

    @SideOnly(Side.CLIENT)
    public static float getDeltaTime()
    {
        return DELTA_TIME;
    }

    public static double lerp(double a, double b, double t)
    {
        return a + (b - a) * t;
    }

    public static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }

    public static int lerp(int a, int b, double t)
    {
        return Utils.round(a + (b - a) * t);
    }

    public static int lerp(int a, int b, float t)
    {
        return Utils.round(a + (b - a) * t);
    }

    public static Vec3d lerpVec3d(Vec3d a, Vec3d b, double t)
    {
        double x = lerp(a.x, b.x, t);
        double y = lerp(a.y, b.y, t);
        double z = lerp(a.z, b.z, t);
        return new Vec3d(x, y, z);
    }

    public static double mapValue(double value, double min1, double max1, double min2, double max2)
    {
        return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
    }

    public static float mapValue(float value, float min1, float max1, float min2, float max2)
    {
        return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
    }

    public static int mapValue(int value, int min1, int max1, int min2, int max2)
    {
        return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
    }

    public static void copyDamageSource(DamageSource source, DamageSource target)
    {
        if (source != null)
        {
            target.damageType = source.damageType;
            if (source.isDifficultyScaled())
            {
                target.setDifficultyScaled();
            }
            if (source.isFireDamage())
            {
                target.setFireDamage();
            }
            if (source.isUnblockable())
            {
                target.setDamageBypassesArmor();
            }
            if (source.isMagicDamage())
            {
                target.setMagicDamage();
            }
            if (source.isExplosion())
            {
                target.setExplosion();
            }
            if (source.isProjectile())
            {
                target.setProjectile();
            }
            if (source.isDamageAbsolute())
            {
                target.setDamageIsAbsolute();
            }
            if (source.canHarmInCreative())
            {
                target.setDamageAllowedInCreativeMode();
            }
        }
    }

    public static Entity getEntityEntityLookingAt(EntityLivingBase entity, float reachDistance)
    {
        Vec3d vec = getPosEntityLookingAt(entity, reachDistance);
        EntityRayResult<Entity> result = raytraceEntities(entity.world, new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), vec, false, false, true);
        if (!result.entities.isEmpty())
        {
            Map<Double, Entity> distances = Maps.newHashMap();
            for (Entity hit : result.entities)
            {
				if (hit == entity)
				{
					continue;
				}
                distances.put((double) entity.getDistance(hit), hit);
            }
            if (!distances.isEmpty())
            {
                return distances.get(Collections.min(distances.keySet()));
            }
        }
        return null;
    }

    public static <T extends Entity> T getEntityEntityLookingAt(EntityLivingBase entity, float reachDistance, Class<T> entityClass)
    {
        Vec3d vec = getPosEntityLookingAt(entity, reachDistance);
        EntityRayResult<T> result = raytraceEntities(entityClass, entity.world, new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), vec, false, false, true);
        if (!result.entities.isEmpty())
        {
            Map<Double, T> distances = Maps.newHashMap();
            for (T hit : result.entities)
            {
				if (hit == entity)
				{
					continue;
				}
                distances.put((double) entity.getDistance(hit), hit);
            }
            if (!distances.isEmpty())
            {
                return distances.get(Collections.min(distances.keySet()));
            }
        }
        return null;
    }

    public static Vec3d getPosEntityLookingAt(EntityLivingBase entity, float reachDistance)
    {
        Vec3d pos = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        Vec3d look = entity.getLook(1.0F);
        Vec3d vec = pos.add(look.x * reachDistance, look.y * reachDistance, look.z * reachDistance);
        return vec;
    }

    public static double getYawBetweenEntities(Entity first, Entity second)
    {
        return Math.atan2(first.posZ - second.posZ, first.posX - second.posX) * (180 / Math.PI) + 90;
    }

    public static double getPitchBetweenEntities(Entity first, Entity second)
    {
        double dx = first.posX - second.posX;
        double dz = first.posZ - second.posZ;
        return Math.atan2((first.posY + first.getEyeHeight()) - (second.posY + (second.height / 2.0F)), Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI;
    }

    public static double getYawBetweenVec(Vec3d first, Vec3d second)
    {
        return MathHelper.atan2(first.z - second.z, first.x - second.x) * 180 / Math.PI + 90;
    }

    public double getPitchBetweenVec(Vec3d first, Vec3d second)
    {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.atan2(first.y - second.y, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI;
    }

    public static double getYawBetweenEntityAndVec(Entity entity, Vec3d vec)
    {
        return MathHelper.atan2(entity.posZ - vec.z, entity.posX - vec.x) * 180 / Math.PI + 90;
    }

    public static double getPitchBetweenEntityAndVec(Entity entity, Vec3d vec)
    {
        double dx = entity.posX - vec.x;
        double dz = entity.posZ - vec.z;
        return Math.atan2((entity.posY + entity.getEyeHeight()) - vec.y, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI;
    }

    public static int round(double a)
    {
		if (Double.isNaN(a))
		{
			return 0;
		}
        return (int) (a + 0.5);
    }

    public static float getYawFromFacing(EnumFacing facing)
    {
        switch (facing)
        {
            case NORTH:
                return 0;
            case EAST:
                return 90;
            case SOUTH:
                return 180;
            case WEST:
                return 270;
            default:
                throw new IllegalStateException("Unable to get yaw from facing " + facing);
        }
    }

    public static float getPitchFromFacing(EnumFacing facing)
    {
        switch (facing)
        {
            case UP:
                return -90;
            case DOWN:
                return 90;
            default:
                throw new IllegalStateException("Unable to get pitch from facing " + facing);
        }
    }

    public static boolean doesBlockHaveCollison(World world, BlockPos pos)
    {
        return world.getBlockState(pos).getCollisionBoundingBox(world, pos) != Block.NULL_AABB;
    }

    public static PacketBuffer getPacketBuffer(ByteBuf buf)
    {
        return buf instanceof PacketBuffer ? (PacketBuffer) buf : new PacketBuffer(buf);
    }

    public static EntityRayResult<Entity> raytraceEntities(World world, Vec3d from, Vec3d to, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        EntityRayResult<Entity> result = new EntityRayResult<Entity>();
        result.setBlockHit(world.rayTraceBlocks(new Vec3d(from.x, from.y, from.z), to, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock));

        double collidePosX;
        double collidePosY;
        double collidePosZ;

        if (result.getBlockHit() != null)
        {
            collidePosX = result.getBlockHit().hitVec.x;
            collidePosY = result.getBlockHit().hitVec.y;
            collidePosZ = result.getBlockHit().hitVec.z;
        }
        else
        {
            collidePosX = 30;
            collidePosY = 30;
            collidePosZ = 30;
        }

        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(Math.min(from.x, collidePosX), Math.min(from.y, collidePosY), Math.min(from.z, collidePosZ), Math.max(from.x, collidePosX), Math.max(from.y, collidePosY), Math.max(from.z, collidePosZ)).grow(1, 1, 1));
        for (Entity entity : entities)
        {
            float pad = entity.getCollisionBorderSize() + 0.5f;
            AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(pad, pad, pad);
            RayTraceResult hit = aabb.calculateIntercept(from, to);
            if (aabb.contains(from))
            {
                result.addEntityHit(entity);
            }
            else if (hit != null)
            {
                result.addEntityHit(entity);
            }
        }

        return result;
    }

    public static <T extends Entity> EntityRayResult<T> raytraceEntities(Class<T> entityClass, World world, Vec3d from, Vec3d to, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        EntityRayResult<T> result = new EntityRayResult<T>();
        result.setBlockHit(world.rayTraceBlocks(new Vec3d(from.x, from.y, from.z), to, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock));

        double collidePosX;
        double collidePosY;
        double collidePosZ;

        if (result.getBlockHit() != null)
        {
            collidePosX = result.getBlockHit().hitVec.x;
            collidePosY = result.getBlockHit().hitVec.y;
            collidePosZ = result.getBlockHit().hitVec.z;
        }
        else
        {
            collidePosX = 30;
            collidePosY = 30;
            collidePosZ = 30;
        }

        List<T> entities = world.getEntitiesWithinAABB(entityClass, new AxisAlignedBB(Math.min(from.x, collidePosX), Math.min(from.y, collidePosY), Math.min(from.z, collidePosZ), Math.max(from.x, collidePosX), Math.max(from.y, collidePosY), Math.max(from.z, collidePosZ)).grow(1, 1, 1));
        for (T entity : entities)
        {
            float pad = entity.getCollisionBorderSize() + 0.5f;
            AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(pad, pad, pad);
            RayTraceResult hit = aabb.calculateIntercept(from, to);
            if (aabb.contains(from))
            {
                result.addEntityHit(entity);
            }
            else if (hit != null)
            {
                result.addEntityHit(entity);
            }
        }

        return result;
    }

    /**
     * This method is slow but returns a lambda of the chosen interface, that when used invokes the chosen method, almost as fast as calling the method normally
     * <p>
     * If the provided method is non static then the provided interface method first parameter needs to be an instance of the object whose class holds the method
     *
     * @param <T>             The interface type
     * @param lambdaInterface The interface to put cast the lambda as
     * @param method          The method to be invoked when the lambda is used
     * @return The interface who holds the lambda
     * @throws LambdaConversionException When the provided interface method doesn't match the provided method
     */
    public static <T> T createLambdaFromMethod(Class<T> lambdaInterface, Method method) throws LambdaConversionException
    {
        try
        {
            boolean isStatic = Modifier.isStatic(method.getModifiers());

            Lookup lookup = EventHandler.privateAccessLookup.in(method.getDeclaringClass());
            Method interfaceMethod = getFunctionalInterfaceMethod(lambdaInterface);

            MethodType interfaceType = MethodType.methodType(interfaceMethod.getReturnType(), interfaceMethod.getParameterTypes());
            MethodType methodType = isStatic ?
                    MethodType.methodType(method.getReturnType(), method.getParameterTypes()) :
                    MethodType.methodType(method.getReturnType(), method.getDeclaringClass(), method.getParameterTypes());

            MethodHandle lambdaImplementation = lookup.unreflect(method);
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    interfaceMethod.getName(),
                    MethodType.methodType(lambdaInterface),
                    interfaceType,
                    lambdaImplementation,
                    methodType);

            return (T) site.getTarget().invoke();
        }
        catch (Throwable e)
        {
            if (e instanceof LambdaConversionException)
            {
                throw (LambdaConversionException) e;
            }
            else
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * This method is slow but returns a lambda of the chosen interface, that when used invokes the chosen constructor and returns a new instance of the constructor's class, almost as fast as creating a new instance normally
     * <p>
     * If the provided method is non static then the provided interface method first parameter needs to be an instance of the object whose class holds the method
     *
     * @param <T>             The interface type
     * @param lambdaInterface The interface to put to lambda into
     * @param constructor     The constructor to be invoked when the lambda is used
     * @return The interface who holds the lambda
     * @throws LambdaConversionException When the provided interface method doesn't match the provided constructor
     */
    public static <T> T createLambdaFromConstructor(Class<T> lambdaInterface, Constructor<?> constructor) throws LambdaConversionException
    {
        try
        {
            Lookup lookup = EventHandler.privateAccessLookup.in(constructor.getDeclaringClass());
            Method interfaceMethod = getFunctionalInterfaceMethod(lambdaInterface);

            MethodType interfaceType = MethodType.methodType(interfaceMethod.getReturnType(), interfaceMethod.getParameterTypes());
            MethodType methodType = MethodType.methodType(constructor.getDeclaringClass(), constructor.getParameterTypes());

            MethodHandle lambdaImplementation = lookup.unreflectConstructor(constructor);
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    interfaceMethod.getName(),
                    MethodType.methodType(lambdaInterface),
                    interfaceType,
                    lambdaImplementation,
                    methodType);

            return (T) site.getTarget().invoke();
        }
        catch (Throwable e)
        {
            if (e instanceof LambdaConversionException)
            {
                throw (LambdaConversionException) e;
            }
            else
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @return The functional interface method
     * @throws IllegalArgumentException If the provided class isn't a functional interface
     */
    public static Method getFunctionalInterfaceMethod(Class<?> clazz)
    {
        if (clazz.isInterface())
        {
            Method func = null;
            for (Method m : clazz.getMethods())
            {
				if (Modifier.isStatic(m.getModifiers()) || !Modifier.isAbstract(m.getModifiers()))
				{
					continue;
				}

                if (func != null)
                {
                    func = null;
                    break;
                }

                func = m;
            }

            if (func != null)
            {
                return func;
            }
        }

        throw new IllegalArgumentException(clazz + " isn't a functional interface");
    }

    /**
     * Creates an {@link IInvoker} that invokes the provided method when called
     *
     * @param callback The method needs to be public(a public method inside a private class still works)
     * @return The invoker to be used to invoke the method
     */
    @SuppressWarnings("unchecked")
    public static <T> IInvoker<T> createWrapper(Method callback)
    {
		if (INVOKERS.containsKey(callback))
		{
			return (IInvoker<T>) INVOKERS.get(callback);
		}

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        GeneratorAdapter mv;

        boolean isMethodStatic = Modifier.isStatic(callback.getModifiers());
        boolean isInterface = callback.getDeclaringClass().isInterface();

        String parameters = Arrays.toString(callback.getParameterTypes());
        String name = String.format("%s_%s(%s)", callback.getDeclaringClass().getSimpleName(),
                callback.getName(), parameters.subSequence(1, parameters.length() - 1));

        String desc = name.replace('.', '/');
        String instType = Type.getInternalName(callback.getDeclaringClass());
        Type owner = Type.getObjectType(desc);
        Type objectType = Type.getType(Object.class);

        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, objectType.getInternalName(), new String[]{Type.getInternalName(IInvoker.class)});
        cw.visitSource(".dynamic", null);
        {
            String d = Type.getMethodDescriptor(Type.VOID_TYPE);

            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", d, null, null), ACC_PUBLIC, "<init>", d);
            mv.loadThis();

            mv.invokeConstructor(objectType, new org.objectweb.asm.commons.Method("<init>", "()V"));
            mv.returnValue();

            mv.endMethod();
        }
        {
            Class<?>[] params = callback.getParameterTypes();

            Label exception = new Label();
            Label end = new Label();

            String d = Type.getMethodDescriptor(objectType, objectType, Type.getType(Object[].class));

            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", d, null, null), ACC_PUBLIC | ACC_VARARGS, "invoke", d);
            mv.loadArg(1);

            mv.arrayLength();
            mv.push(params.length);

            mv.ifICmp(IFNE, exception);
            if (!isMethodStatic)
            {
                mv.loadArg(0);
                mv.checkCast(Type.getObjectType(instType));
            }

            for (int i = 0; i < params.length; i++)
            {
                Class<?> parameter = params[i];
                Type parameterType = Type.getType(parameter);

                if (parameter.isPrimitive())
                {
                    if (parameter == boolean.class)
                    {
                        mv.loadArg(1);
                        mv.push(i);

                        mv.arrayLoad(objectType);
                        mv.checkCast(Type.getType(Boolean.class));

                        mv.invokeVirtual(Type.getType(Boolean.class), new org.objectweb.asm.commons.Method("booleanValue", "()Z"));
                    }
                    else if (parameter == char.class)
                    {
                        mv.loadArg(1);
                        mv.push(i);

                        mv.arrayLoad(objectType);
                        mv.checkCast(Type.getType(Character.class));

                        mv.invokeVirtual(Type.getType(Character.class), new org.objectweb.asm.commons.Method("charValue", "()C"));
                    }
                    else
                    {
                        mv.loadArg(1);
                        mv.push(i);

                        mv.arrayLoad(objectType);
                        mv.checkCast(Type.getType(Number.class));

                        mv.invokeVirtual(Type.getType(Number.class), new org.objectweb.asm.commons.Method(parameter.getName() + "Value", Type.getMethodDescriptor(parameterType)));
                    }
                }
                else
                {
                    mv.loadArg(1);
                    mv.push(i);

                    mv.arrayLoad(objectType);
                    mv.checkCast(parameterType);
                }
            }

            mv.visitMethodInsn(isMethodStatic ? INVOKESTATIC : isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL, instType, callback.getName(), Type.getMethodDescriptor(callback), isInterface);

			if (callback.getReturnType() == void.class)
			{
				mv.visitInsn(ACONST_NULL);
			}

            mv.goTo(end);

            mv.visitLabel(exception);
            mv.newInstance(Type.getType(IllegalArgumentException.class));

            mv.dup();

            mv.push("Given arguments with length %s don't match method arguments with length %s");
            mv.push(2);

            mv.newArray(objectType);
            mv.dup();

            mv.push(0);
            mv.loadArg(1);

            mv.arrayLength();
            mv.invokeStatic(Type.getType(Integer.class), new org.objectweb.asm.commons.Method("valueOf", Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE)));

            mv.arrayStore(objectType);
            mv.dup();

            mv.push(1);
            mv.loadThis();

            mv.getField(owner, "paramCount", Type.INT_TYPE);
            mv.invokeStatic(Type.getType(Integer.class), new org.objectweb.asm.commons.Method("valueOf", Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE)));

            mv.arrayStore(objectType);
            mv.invokeStatic(Type.getType(String.class), new org.objectweb.asm.commons.Method("format", Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class), Type.getType(Object[].class))));

            mv.invokeConstructor(Type.getType(IllegalArgumentException.class), new org.objectweb.asm.commons.Method("<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class))));
            mv.throwException();

            mv.visitLabel(end);
            mv.returnValue();

            mv.endMethod();
        }
        cw.visitEnd();

        IInvoker<T> invoker = null;
        try
        {
            invoker = (IInvoker<T>) LOADER.define(name, cw.toByteArray()).newInstance();
            INVOKERS.put(callback, invoker);
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }

        return invoker;
    }

    public interface IInvoker<T>
    {
        /**
         * @param instance May be null if static
         * @param args     The method arguments
         * @return What the provided method returns or null if it returns void
         */
        T invoke(Object instance, Object... args);
    }

    public static class EntityRayResult<T extends Entity>
    {
        private RayTraceResult blockHit;

        private final List<T> entities = new ArrayList<>();

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

        public List<T> getEntityHits()
        {
            return Collections.unmodifiableList(this.entities);
        }
    }

    public enum Filter
    {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR),
        NEAREST_MIPMAP_NEAREST(GL_NEAREST_MIPMAP_NEAREST),
        NEAREST_MIPMAP_LINEAR(GL_NEAREST_MIPMAP_LINEAR),
        LINEAR_MIPMAP_LINEAR(GL_LINEAR_MIPMAP_LINEAR),
        LINEAR_MIPMAP_NEAREST(GL_LINEAR_MIPMAP_NEAREST);

        public final int capability;
        private static final Map<Integer, Filter> capabilityToFilter = Maps.newHashMap();

        Filter(int capability)
        {
            this.capability = capability;
        }

        public static Filter valueOf(int capability)
        {
            return capabilityToFilter.get(capability);
        }

        static
        {
            for (Filter filter : Filter.values())
            {
                capabilityToFilter.put(filter.capability, filter);
            }
        }
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID)
    private static class EventHandler
    {
        private static final Lookup privateAccessLookup = ObfuscationReflectionHelper.getPrivateValue(Lookup.class, null, "IMPL_LOOKUP");

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onServerTick(TickEvent.ServerTickEvent event)
        {
            if (event.phase == Phase.END)
            {
                if (TICKS.size() != TEMP_TICKS.size() || !TICKS.values().containsAll(TICKS.values()))
                {
                    TICKS.clear();
                    TICKS.putAll(TEMP_TICKS);
                }

                for (Iterator<ITick> iterator = TICKS.values().iterator(); iterator.hasNext(); )
                {
                    ITick tickable = iterator.next();
                    if (tickable.remove())
                    {
                        iterator.remove();
                    }
                    else
                    {
                        tickable.update();
                    }
                }
            }
        }

        @SideOnly(Side.CLIENT)
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
			if (Minecraft.getMinecraft().isGamePaused())
			{
				return;
			}
            if (event.phase == Phase.END)
            {
                if (TICKS.size() != TEMP_TICKS.size() || !TICKS.values().containsAll(TICKS.values()))
                {
                    TICKS.clear();
                    TICKS.putAll(TEMP_TICKS);
                }

                for (Iterator<ITick> iterator = TICKS.values().iterator(); iterator.hasNext(); )
                {
                    ITick tickable = iterator.next();
                    if (tickable.remove())
                    {
                        iterator.remove();
                    }
                    else
                    {
                        tickable.update();
                    }
                }
            }
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void onDisconnectionFromServer(ClientDisconnectionFromServerEvent event)
        {
            TICKS.clear();
        }

        static long newTime;

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        @SideOnly(Side.CLIENT)
        public static void onRenderTick(RenderTickEvent event)
        {
            if (event.phase == Phase.START)
            {
                long oldTime = newTime;
                newTime = Minecraft.getSystemTime();
                Utils.DELTA_TIME = (newTime - oldTime) / 1000f;
                Utils.RESOLUTION = new ScaledResolution(Minecraft.getMinecraft());
            }
        }

        @SubscribeEvent
        public static void onEvent(Event event)
        {
            Collection<Consumer<Event>> consumers = EVENT_CONSUMERS.get(event.getClass());
            for (Iterator<Consumer<Event>> iterator = consumers.iterator(); iterator.hasNext(); )
            {
                Consumer<Event> consumer = iterator.next();
                consumer.accept(event);

                iterator.remove();
            }
        }
    }

    private static class CustomClassLoader extends ClassLoader
    {
        private CustomClassLoader()
        {
            super(CustomClassLoader.class.getClassLoader());
        }

        public Class<?> define(String name, byte[] data)
        {
            return defineClass(name, data, 0, data.length);
        }
    }
}
