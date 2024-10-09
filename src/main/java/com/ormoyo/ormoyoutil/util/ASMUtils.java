package com.ormoyo.ormoyoutil.util;

import com.google.common.collect.Maps;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import javax.annotation.Nonnull;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("unchecked")
public class ASMUtils
{
    private static final AtomicInteger IDs = new AtomicInteger();
    private static final ASMClassLoader LOADER = new ASMClassLoader();

    private static final Map<Executable, Class<?>> CALLBACKS = Maps.newHashMap();

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    private static final MethodHandles.Lookup privateAccessLookup = ObfuscationReflectionHelper.getPrivateValue(MethodHandles.Lookup.class, null, "IMPL_LOOKUP");

    /**
     * This method is slow but returns a lambda of the chosen interface, that when used invokes the chosen method(even if it's private), almost as fast as calling the method normally
     * <p>
     * If the provided method is non-static then the provided interface method first parameter needs to be an instance of the object whose class holds the method
     *
     * @param <T>             The interface type
     * @param lambdaInterface The interface to put cast the lambda as(needs to be a {@link FunctionalInterface})
     * @param method          The method to be invoked when the lambda is used
     * @return The interface who holds the lambda
     * @throws LambdaConversionException When the provided interface method doesn't match the provided method
     */
    public static <T> T createLambdaFromMethod(Class<T> lambdaInterface, Method method) throws LambdaConversionException
    {
        try
        {
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            if (!method.isAccessible())
                method.setAccessible(true);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
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
        catch (LambdaConversionException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This method is slow but returns a lambda of the chosen interface, that when used invokes the chosen constructor(even if it's private) and returns a new instance of the constructor's class, almost as fast as creating a new instance normally
     * <p>
     * If the provided method is non-static then the provided interface method first parameter needs to be an instance of the object whose class holds the method
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
            if (!constructor.isAccessible())
                constructor.setAccessible(true);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
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
        catch (LambdaConversionException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            e.printStackTrace();
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
                    continue;

                if (func != null)
                {
                    func = null;
                    break;
                }

                func = m;
            }

            if (func != null)
                return func;
        }

        throw new IllegalArgumentException(clazz + " isn't a functional interface");
    }

    /**
     * Creates an instance of the provided interface that when called will call the provided method
     *
     * @param iface    The interface to implement. <br> This has to be a {@link FunctionalInterface} and match parameters and return types with the provided method
     * @param callback The method to call
     * @param <T>      Interface type
     */
    public static <T> T createMethodCallback(Class<T> iface, Method callback)
    {
        Method ifaceMethod = getFunctionalInterfaceMethod(iface);

        if (CALLBACKS.containsKey(callback))
        {
            return (T) CALLBACKS.get(callback);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        GeneratorAdapter mv;

        String name = getUniqueName(callback);
        String desc = name.replace('.', '/');

        String handlerDesc = Type.getInternalName(iface);

        Type instType = Type.getType(callback.getDeclaringClass());
        String handlerFuncDesc = Type.getMethodDescriptor(ifaceMethod);

        Type ifaceMethodType = Type.getType(handlerFuncDesc);

        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{handlerDesc});

        cw.visitSource(".dynamic", null);
        {
            // Constructor
            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null), ACC_PUBLIC, "<init>", "()V");

            // super()
            mv.loadThis();
            mv.invokeConstructor(Type.getType(Object.class), new org.objectweb.asm.commons.Method("<init>", "()V"));

            mv.returnValue();
            mv.endMethod();
        }
        {
            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, ifaceMethod.getName(), handlerFuncDesc, null, null), ACC_PUBLIC, ifaceMethod.getName(), handlerFuncDesc);

            //Invoke callback method
            for (int i = 0; i < ifaceMethodType.getArgumentTypes().length; i++)
            {
                if (i == 0 && !isStatic)
                {
                    mv.loadArg(0);
                    mv.checkCast(instType);

                    continue;
                }

                Class<?> clazz = callback.getParameterTypes()[i];

                mv.loadArg(i);
                mv.checkCast(Type.getType(clazz));
            }

            org.objectweb.asm.commons.Method method = org.objectweb.asm.commons.Method.getMethod(callback);
            if (isStatic)
                mv.invokeStatic(instType, method);
            else
                mv.invokeVirtual(instType, method);

            mv.returnValue();
            mv.endMethod();
        }
        cw.visitEnd();

        Class<?> ret = LOADER.define(name, cw.toByteArray());
        CALLBACKS.put(callback, ret);

        try
        {
            return (T) ret.getConstructor().newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    /**
     * Creates an instance of the provided interface that when called will call the provided method
     *
     * @param iface    The interface to implement. <br> This has to be a {@link FunctionalInterface} and match parameters and return types with the provided method
     * @param callback The method to call
     * @param <T>      Interface type
     */
    public static <T, R> T createConstructorCallback(Class<T> iface, Constructor<R> callback)
    {
        Method ifaceMethod = getFunctionalInterfaceMethod(iface);

        if (CALLBACKS.containsKey(callback))
        {
            return (T) CALLBACKS.get(callback);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        GeneratorAdapter mv;

        String name = getUniqueName(callback);
        String desc = name.replace('.', '/');

        String handlerDesc = Type.getInternalName(iface);

        Type instType = Type.getType(callback.getDeclaringClass());
        String handlerFuncDesc = Type.getMethodDescriptor(ifaceMethod);

        Type ifaceMethodType = Type.getType(handlerFuncDesc);

        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{handlerDesc});
        cw.visitSource(".dynamic", null);
        {
            // Constructor
            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null), ACC_PUBLIC, "<init>", "()V");

            // super()
            mv.loadThis();
            mv.invokeConstructor(Type.getType(Object.class), new org.objectweb.asm.commons.Method("<init>", "()V"));

            mv.returnValue();
            mv.endMethod();
        }
        {
            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, ifaceMethod.getName(), handlerFuncDesc, null, null), ACC_PUBLIC, ifaceMethod.getName(), handlerFuncDesc);

            //Return new instance
            mv.newInstance(instType);
            mv.dup();

            for (int i = 0; i < ifaceMethodType.getArgumentTypes().length; i++)
            {
                Class<?> clazz = callback.getParameterTypes()[i];

                mv.loadArg(i);
                mv.checkCast(Type.getType(clazz));
            }

            mv.invokeConstructor(instType, org.objectweb.asm.commons.Method.getMethod(callback));

            mv.returnValue();
            mv.endMethod();
        }
        cw.visitEnd();

        Class<?> ret = LOADER.define(name, cw.toByteArray());
        CALLBACKS.put(callback, ret);

        try
        {
            return (T) ret.getConstructor().newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    public static String getCallerClassName()
    {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        String callerClassName = null;

        for (int i = 1; i < stElements.length; i++)
        {
            StackTraceElement ste = stElements[i];

            if (!ste.getClassName().equals(ASMUtils.class.getName()) && ste.getClassName().indexOf("java.lang.Thread") != 0)
            {
                if (callerClassName == null)
                    callerClassName = ste.getClassName();
                else if (!ste.getClassName().equals(callerClassName))
                    return ste.getClassName();
            }
        }

        return null;
    }

    private static String getUniqueName(Executable callback)
    {
        return String.format("%s_%d_%s_%s_%s", ASMUtils.class.getName(), IDs.getAndIncrement(),
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
}
