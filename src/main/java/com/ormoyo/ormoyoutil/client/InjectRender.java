package com.ormoyo.ormoyoutil.client;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRender
{
    String value();

    @OnlyIn(Dist.CLIENT)
    class Handler
    {
        @SuppressWarnings({"rawtypes", "unchecked"})
        public static void injectRender(ModFileScanData data)
        {
            Stream<ModFileScanData.AnnotationData> renderers = data.getAnnotations().stream()
                    .filter(annotationData -> Type.getType(InjectRender.class).equals(annotationData.getAnnotationType()));

            renderers.forEach(target -> {
                try
                {
                    Class<?> c = Class.forName(target.getClassType().getClassName());
                    if (!EntityRenderer.class.isAssignableFrom(c))
                    {
                        OrmoyoUtil.LOGGER.error("Class {} doesn't extend Render", target.getClassType().getClassName());
                        return;
                    }

                    Class<? extends EntityRenderer> clazz = (Class<? extends EntityRenderer>) c;
                    InjectRender annotation = clazz.getAnnotation(InjectRender.class);

                    Optional<EntityType<?>> entityType = EntityType.byKey(annotation.value());
                    if (!entityType.isPresent())
                    {
                        OrmoyoUtil.LOGGER.error("EntityType {} doesn't exist", annotation.value());
                        return;
                    }

                    RenderingRegistry.registerEntityRenderingHandler(entityType.get(), manager -> {
                        try
                        {
                            Constructor<? extends EntityRenderer> constructor = clazz.getConstructor(EntityRendererManager.class);
                            return constructor.newInstance(manager);
                        }
                        catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                               InvocationTargetException | NoSuchMethodException | SecurityException e)
                        {
                            throw new RuntimeException(e);
                        }
                    });
                }
                catch (Exception e)
                {
                    OrmoyoUtil.LOGGER.error("Failed to inject entity renderer class {}", target.getClassType().getClassName(), e);
                }
            });
        }
    }
}
