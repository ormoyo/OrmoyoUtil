package com.ormoyo.ormoyoutil.client;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@SideOnly(Side.CLIENT)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRender
{
    Class<? extends Entity> value();

    @SideOnly(Side.CLIENT)
    class Handler
    {
        @SuppressWarnings({"rawtypes", "unchecked"})
        public static void injectRender(ASMDataTable data)
        {
            ClassLoader classLoader = Loader.instance().getModClassLoader();
            for (ASMData target : data.getAll(InjectRender.class.getName()))
            {
                try
                {
                    Class<?> c = Class.forName(target.getClassName(), true, classLoader);
                    if (!Render.class.isAssignableFrom(c))
                    {
                        OrmoyoUtil.LOGGER.error("Class {} doesn't extend Render", target.getClassName());
                        return;
                    }
                    Class<? extends Render> clazz = (Class<? extends Render>) c;
                    InjectRender annotation = clazz.getAnnotation(InjectRender.class);
                    RenderingRegistry.registerEntityRenderingHandler(annotation.value(), new IRenderFactory()
                    {
                        @Override
                        public Render createRenderFor(RenderManager manager)
                        {
                            try
                            {
                                Constructor<? extends Render> constructor = clazz.getDeclaredConstructor(RenderManager.class);
                                constructor.setAccessible(true);
                                return constructor.newInstance(manager);
                            }
                            catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                                   InvocationTargetException | NoSuchMethodException | SecurityException e)
                            {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    });
                }
                catch (Exception e)
                {
                    OrmoyoUtil.LOGGER.fatal("Failed to inject render class {}", target.getClassName(), e);
                }
            }
        }
    }
}
