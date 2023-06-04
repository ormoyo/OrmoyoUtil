package com.ormoyo.ormoyoutil.util;

import com.google.common.collect.Maps;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import javax.annotation.Nonnull;
import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("unchecked")
public class ASMUtils
{
    private static final AtomicInteger IDs = new AtomicInteger();
    private static final ASMClassLoader LOADER = new ASMClassLoader();

    private static final HashMap<Method, Class<?>> CALLERS = Maps.newHashMap();
    private static final Map<Method, IInvoker<?>> INVOKERS = Maps.newHashMap();

    @Nonnull
    private static final MethodHandles.Lookup privateAccessLookup = Objects.requireNonNull(ObfuscationReflectionHelper.getPrivateValue(MethodHandles.Lookup.class, null, "IMPL_LOOKUP"));

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

            MethodHandles.Lookup lookup = privateAccessLookup.in(method.getDeclaringClass());
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
            MethodHandles.Lookup lookup = privateAccessLookup.in(constructor.getDeclaringClass());
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
            {
                return func;
            }
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

        if (CALLERS.containsKey(callback))
        {
            return (T) CALLERS.get(callback);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        GeneratorAdapter mv;

        String name = getUniqueName(callback);
        String desc = name.replace('.', '/');

        String handlerDesc = Type.getInternalName(iface);

        Type instType = Type.getType(callback.getDeclaringClass());
        Type ifaceMethodType = Type.getType(ifaceMethod);

        boolean isStatic = Modifier.isStatic(callback.getModifiers());

        String handlerFuncDesc = Type.getMethodDescriptor(ifaceMethodType.getReturnType(), ifaceMethodType.getArgumentTypes());

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

            mv.invokeStatic(instType, new org.objectweb.asm.commons.Method(callback.getName(), Type.getMethodDescriptor(callback)));

            mv.returnValue();
            mv.endMethod();
        }
        cw.visitEnd();

        Class<?> ret = LOADER.define(name, cw.toByteArray());
        CALLERS.put(callback, ret);

        try
        {
            return (T) ret.getConstructor().newInstance();
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            return null;
        }
    }

    /**
     * Creates an {@link IInvoker} that invokes the callback method when called
     *
     * @param callback The method needs to be public(a public method inside a private class still works)
     * @return The invoker to be used to invoke the method
     */
    public static <T> IInvoker<T> createMethodInvoker(Method callback)
    {
        if (INVOKERS.containsKey(callback))
            return (IInvoker<T>) INVOKERS.get(callback);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        GeneratorAdapter mv;

        String descriptor;

        boolean isMethodStatic = Modifier.isStatic(callback.getModifiers());
        boolean isInterface = callback.getDeclaringClass().isInterface();

        String parameters = Arrays.toString(callback.getParameterTypes());
        String name = String.format("%s_%s(%s)", callback.getDeclaringClass().getSimpleName(),
                callback.getName(), parameters.substring(1, parameters.length() - 1));

        String desc = name.replace('.', '/');
        String instType = Type.getInternalName(callback.getDeclaringClass());

        Type owner = Type.getObjectType(desc);
        Type objectType = Type.getType(Object.class);

        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, objectType.getInternalName(), new String[]{Type.getInternalName(IInvoker.class)});
        cw.visitSource(".dynamic", null);
        {
            descriptor = Type.getMethodDescriptor(Type.VOID_TYPE);

            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null), ACC_PUBLIC, "<init>", descriptor);
            mv.loadThis();

            mv.invokeConstructor(objectType, new org.objectweb.asm.commons.Method("<init>", "()V"));
            mv.returnValue();

            mv.endMethod();
        }
        {
            Class<?>[] params = callback.getParameterTypes();

            Label exception = new Label();
            Label end = new Label();

            descriptor = Type.getMethodDescriptor(objectType, objectType, Type.getType(Object[].class));

            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", descriptor, null, null), ACC_PUBLIC | ACC_VARARGS, "invoke", descriptor);
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
                mv.visitInsn(ACONST_NULL);

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

    private static String getUniqueName(Method callback)
    {
        return String.format("%s_%d_%s_%s_%s", ASMUtils.class.getName(), IDs.getAndIncrement(),
                callback.getDeclaringClass().getSimpleName(),
                callback.getName(),
                callback.getParameterTypes()[0].getSimpleName());
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
