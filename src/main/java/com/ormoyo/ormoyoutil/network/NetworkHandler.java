package com.ormoyo.ormoyoutil.network;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class NetworkHandler
{
    private static final Map<String, SimpleChannel> channels = Maps.newHashMap();
    private static final Map<SimpleChannel, Integer> idMap = Maps.newHashMap();

    private static final Map<Class<? extends AbstractMessage>, Function<PacketBuffer, AbstractMessage>> decoders = Maps.newHashMap();

    @ParametersAreNonnullByDefault
    public static void injectNetworkWrapper(ModContainer mod, ModFileScanData scanData)
    {
        for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations())
        {
            Class<?> targetClass = mod.getMod().getClass();
            if (!Type.getType(NetworkChannel.class).equals(annotation.getAnnotationType()))
                continue;
            if (!Type.getType(targetClass).equals(annotation.getClassType()))
                continue;

            try
            {
                Field field = targetClass.getDeclaredField(annotation.getMemberName());
                if (!Modifier.isStatic(field.getModifiers()))
                    continue;

                field.setAccessible(true);

                String version = mod.getModInfo().getVersion().toString();
                SimpleChannel channel = NetworkRegistry.ChannelBuilder
                        .named(new ResourceLocation(mod.getModId(), annotation.getMemberName().toLowerCase()))
                        .clientAcceptedVersions(v -> v.equals(version))
                        .serverAcceptedVersions(v -> v.equals(version))
                        .networkProtocolVersion(() -> version)
                        .simpleChannel();

                field.set(null, channel);
                channels.putIfAbsent(mod.getModId(), channel);
            }
            catch (Exception e)
            {
                OrmoyoUtil.LOGGER.error("Failed to inject network wrapper for mod container {}", mod, e);
            }
        }
    }

    @ParametersAreNonnullByDefault
    public static <T extends AbstractMessage<T>> void registerNetworkMessages(ModFileScanData scanData)
    {
        Stream<ModFileScanData.AnnotationData> messages = scanData.getAnnotations().stream()
                .filter(annotationData -> Type.getType(NetworkMessage.class).equals(annotationData.getAnnotationType()))
                .sorted(Comparator.comparing(a -> a.getClassType().getInternalName()));

        for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations())
        {
            Type type = annotation.getAnnotationType();
            if (!Type.getType(NetworkDecoder.class).equals(type))
                continue;

            List<Type> classes = (List<Type>) annotation.getAnnotationData().get("value");
            String className = annotation.getClassType().getClassName();

            String methodName = annotation.getMemberName().substring(0, annotation.getMemberName().indexOf('('));

            try
            {
                Class clazz = Class.forName(className);
                Method method = clazz.getMethod(methodName, PacketBuffer.class);

                classes.forEach(c ->
                {
                    try
                    {
                        Class<?> cla = Class.forName(c.getClassName());

                        NetworkHandler.decoders.put((Class<? extends AbstractMessage>) cla,
                                ASMUtils.createMethodCallback(Function.class, method));
                    }
                    catch (ClassNotFoundException e)
                    {
                        OrmoyoUtil.LOGGER.error("Couldn't find the class {}", c.getClassName());
                    }
                });
            }
            catch (ReflectiveOperationException e)
            {
                throw new RuntimeException(e);
            }
        }

        messages.forEach(annotation -> {
            String modid = (String) annotation.getAnnotationData().get("modid");
            String className = annotation.getClassType().getClassName();

            if (!channels.containsKey(modid))
            {
                OrmoyoUtil.LOGGER.error("Couldn't register network message {} because the mod {} doesn't exist or doesn't have a channel", className, modid);
                return;
            }

            try
            {
                Class<?> clazz = Class.forName(className);
                if (!AbstractMessage.class.isAssignableFrom(clazz))
                {
                    OrmoyoUtil.LOGGER.error("Network message {} doesn't extend AbstractMessage", className);
                    return;
                }

                Class<T> message = (Class<T>) clazz;
                SimpleChannel channel = channels.get(modid);

                List<ModAnnotation.EnumHolder> holders = (List<ModAnnotation.EnumHolder>) annotation.getAnnotationData().get("direction");

                if (holders == null)
                {
                    NetworkHandler.registerMessage(channel, message);
                    return;
                }

                List<NetworkDirection> directions = holders.stream().map(h -> NetworkDirection.valueOf(h.getValue())).collect(Collectors.toList());
                for (NetworkDirection direction : directions)
                {
                    NetworkHandler.registerMessage(channel, message, direction);
                }
            }
            catch (ClassNotFoundException e)
            {
                OrmoyoUtil.LOGGER.error("Failed to find network message class {}", className);
            }
        });
    }

    private static <T extends AbstractMessage<T>> void registerMessage(SimpleChannel channel, Class<T> clazz)
    {
        NetworkHandler.registerMessage(channel, clazz, null);
    }

    private static <T extends AbstractMessage<T>> void registerMessage(SimpleChannel channel, Class<T> clazz, NetworkDirection direction)
    {
        int id = 0;

        if (idMap.containsKey(channel))
            id = idMap.get(channel);

        if (!decoders.containsKey(clazz))
        {
            OrmoyoUtil.LOGGER.error("Couldn't find network decoder for message {} or it doesn't exist", clazz.getName());
            return;
        }

        channel.registerMessage(id,
                clazz,
                AbstractMessage::encode,
                buf -> (T) decoders.get(clazz).apply(buf), AbstractMessage::onMessage,
                direction == null ? Optional.empty() : Optional.of(direction));

        idMap.put(channel, id + 1);
    }
}
