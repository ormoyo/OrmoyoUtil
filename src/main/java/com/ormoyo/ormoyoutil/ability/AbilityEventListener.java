package com.ormoyo.ormoyoutil.ability;

import com.google.common.collect.Maps;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IGenericEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class AbilityEventListener implements IAbilityEventListener
{
    private static final AtomicInteger IDs = new AtomicInteger();
    private static final String HANDLER_DESC = Type.getInternalName(IAbilityEventListener.class);
    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Ability.class), Type.getType(Event.class));
    private static final ASMClassLoader LOADER = new ASMClassLoader();
    private static final Map<Method, Class<?>> cache = Maps.newHashMap();

    private final Method method;
    private final Class<? extends Event> eventClass;
    private final IAbilityEventPredicate<? extends Event> predicate;

    private final IAbilityEventListener handler;
    private final SubscribeEvent subInfo;
    private final String readable;
    private java.lang.reflect.Type filter = null;

    public AbilityEventListener(AbilityEntry target, Method method, Class<? extends Event> eventClass, IAbilityEventPredicate<? extends Event> predicate, boolean isGeneric) throws ReflectiveOperationException
    {
        this.handler = (IAbilityEventListener) createWrapper(method).getConstructor(ResourceLocation.class).newInstance(target.getRegistryName());
        this.subInfo = method.getAnnotation(SubscribeEvent.class);

        this.readable = "ASM: " + target + " " + method.getName() + Type.getMethodDescriptor(method);
        this.eventClass = eventClass;

        this.predicate = predicate;

        if (isGeneric)
        {
            java.lang.reflect.Type type = method.getGenericParameterTypes()[0];
            if (type instanceof ParameterizedType)
            {
                this.filter = ((ParameterizedType) type).getActualTypeArguments()[0];
            }
        }

        this.method = method;
    }

    @Override
    public Method getMethod()
    {
        return this.method;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void invoke(Ability ability, Event event)
    {
        if (this.handler != null && ability != null)
        {
            if (!event.isCancelable() || !event.isCanceled() || subInfo.receiveCanceled())
            {
                if (this.filter == null || this.filter == ((IGenericEvent) event).getGenericType())
                {
                    this.handler.invoke(ability, event);
                }
            }
        }
    }

    @Override
    public Class<? extends Event> getEventClass()
    {
        return this.eventClass;
    }

    @Override
    public IAbilityEventPredicate<? extends Event> getEventPredicate()
    {
        return this.predicate;
    }

    public Class<?> createWrapper(Method callback)
    {
        if (cache.containsKey(callback))
        {
            return cache.get(callback);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        GeneratorAdapter mv;

        String name = getUniqueName(callback);
        String desc = name.replace('.', '/');

        Type owner = Type.getObjectType(desc);
        Type instType = Type.getType(callback.getDeclaringClass());
        Type resourceLocationType = Type.getType(ResourceLocation.class);
        Type eventType = Type.getType(callback.getParameterTypes()[0]);
        Type abilityType = Type.getType(Ability.class);
        Type exceptionType = Type.getType(IllegalArgumentException.class);

        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{HANDLER_DESC});

        cw.visitSource(".dynamic", null);
        {
            cw.visitField(ACC_PUBLIC, "location", "Lnet/minecraft/util/ResourceLocation;", null, null).visitEnd();
        }
        {
            // Constructor
            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", "(Lnet/minecraft/util/ResourceLocation;)V", null, null), ACC_PUBLIC, "<init>", "(Lnet/minecraft/util/ResourceLocation;)V");

            // super()
            mv.loadThis();
            mv.invokeConstructor(Type.getType(Object.class), new org.objectweb.asm.commons.Method("<init>", "()V"));

            // this.location = location
            mv.loadThis();
            mv.loadArg(0);
            mv.putField(owner, "location", resourceLocationType);

            mv.returnValue();
            mv.endMethod();
        }
        {
            Label exception = new Label();
            Label end = new Label();

            // public invoke(Ability ability, Event event)
            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null), ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC);

            // if (this.location.equals(ability.getRegistryName()))
            mv.loadThis();
            mv.getField(owner, "location", resourceLocationType);
            mv.loadArg(0);
            mv.invokeVirtual(abilityType, new org.objectweb.asm.commons.Method("getRegistryName", "()Lnet/minecraft/util/ResourceLocation;"));
            mv.invokeVirtual(resourceLocationType, new org.objectweb.asm.commons.Method("equals", "(Ljava/lang/Object;)Z"));
            mv.ifZCmp(IFNE, end);

            // If not equals
            mv.visitLabel(exception);
            mv.throwException(exceptionType, "Provided ability doesn't match entry");

            //If equals - Calls callback method
            mv.visitLabel(end);
            mv.loadArg(0);
            mv.checkCast(instType);
            mv.loadArg(1);
            mv.checkCast(eventType);
            mv.invokeVirtual(instType, new org.objectweb.asm.commons.Method(callback.getName(), Type.getMethodDescriptor(callback)));

            mv.returnValue();
            mv.endMethod();
        }
        cw.visitEnd();

        Class<?> ret = LOADER.define(name, cw.toByteArray());
        cache.put(callback, ret);

        return ret;
    }

    private String getUniqueName(Method callback)
    {
        return String.format("%s_%d_%s_%s_%s", getClass().getName(), IDs.getAndIncrement(),
                callback.getDeclaringClass().getSimpleName(),
                callback.getName(),
                callback.getParameterTypes()[0].getSimpleName());
    }

    private static class ASMClassLoader extends ClassLoader
    {
        private ASMClassLoader()
        {
            super(ASMClassLoader.class.getClassLoader());
        }

        public Class<?> define(String name, byte[] data)
        {
            return defineClass(name, data, 0, data.length);
        }
    }

    public String toString()
    {
        return this.readable;
    }
}
