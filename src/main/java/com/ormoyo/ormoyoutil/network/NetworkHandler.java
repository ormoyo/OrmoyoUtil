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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
public class NetworkHandler
{
    private static final Map<String, SimpleChannel> channels = Maps.newHashMap();
    private static final Map<SimpleChannel, Integer> idMap = Maps.newHashMap();

    private static final Map<Class<? extends AbstractMessage>, Function<PacketBuffer, AbstractMessage>> decoders = Maps.newHashMap();

    public static void injectNetworkWrapper(ModContainer mod, ModFileScanData scanData)
    {
        if (scanData == null) return;
        List<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations().stream()
                .filter(annotationData -> Type.getType(NetworkChannel.class).equals(annotationData.getAnnotationType()) &&
                        Type.getType(mod.getMod().getClass()).equals(annotationData.getClassType()))
                .collect(Collectors.toList());

        annotations.forEach(annotation ->
        {
            try
            {
                Class<?> targetClass = mod.getMod().getClass();
                Field field = targetClass.getDeclaredField(annotation.getMemberName());

                field.setAccessible(true);

                SimpleChannel channel = NetworkRegistry.ChannelBuilder
                        .named(new ResourceLocation(mod.getModId(), "main"))
                        .clientAcceptedVersions(version -> version.equals(mod.getModInfo().getVersion().toString()))
                        .serverAcceptedVersions(version -> version.equals(mod.getModInfo().getVersion().toString()))
                        .networkProtocolVersion(() -> mod.getModInfo().getVersion().toString())
                        .simpleChannel();

                field.set(null, channel);
                channels.putIfAbsent(mod.getModId(), channel);
            }
            catch (Exception e)
            {
                OrmoyoUtil.LOGGER.fatal("Failed to inject network wrapper for mod container {}", mod, e);
            }
        });
    }


    public static <T extends AbstractMessage<T>> void registerNetworkMessages(ModFileScanData scanData)
    {
        if (scanData == null) return;
        List<ModFileScanData.AnnotationData> messages = scanData.getAnnotations().stream()
                .filter(annotationData -> Type.getType(NetworkMessage.class).equals(annotationData.getAnnotationType()))
                .sorted(Comparator.comparing(a -> a.getClassType().getInternalName()))
                .collect(Collectors.toList());

        List<ModFileScanData.AnnotationData> networkDecoders = scanData.getAnnotations().stream()
                .filter(annotationData -> Type.getType(NetworkDecoder.class).equals(annotationData.getAnnotationType()))
                .collect(Collectors.toList());

        networkDecoders.forEach(annotation ->
        {
            List<Type> classes = (List<Type>) annotation.getAnnotationData().get("value");
            String className = annotation.getClassType().getInternalName().replace('/', '.');

            String methodName = annotation.getMemberName().substring(0, annotation.getMemberName().indexOf('('));

            try
            {
                Class clazz = Class.forName(className);
                Method method = clazz.getMethod(methodName, PacketBuffer.class);

                classes.forEach(c ->
                {
                    try
                    {
                        Class<?> cla = Class.forName(c.getInternalName().replace('/', '.'));
                        NetworkHandler.decoders.put((Class<? extends AbstractMessage>) cla,
                                ASMUtils.createMethodCallback(Function.class, method));
                    }
                    catch (ClassNotFoundException e)
                    {
                        throw new RuntimeException(e);
                    }
                });
            }
            catch (ReflectiveOperationException e)
            {
                throw new RuntimeException(e);
            }

        });
        messages.forEach(annotation ->
        {
            String modid = (String) annotation.getAnnotationData().get("modid");
            String className = annotation.getClassType().getInternalName().replace('/', '.');

            if (!channels.containsKey(modid))
            {
                OrmoyoUtil.LOGGER.fatal("Couldn't register network message {} because the mod {} doesn't exist or doesn't have a channel", className, modid);
                return;
            }

            try
            {
                Class<?> clazz = Class.forName(className);
                if (!AbstractMessage.class.isAssignableFrom(clazz))
                {
                    OrmoyoUtil.LOGGER.fatal("Network message {} doesn't extend AbstractMessage", className);
                    return;
                }

                Class<T> message = (Class<T>) clazz;
                SimpleChannel channel = channels.get(modid);

                List<ModAnnotation.EnumHolder> holders = (List<ModAnnotation.EnumHolder>) annotation.getAnnotationData().get("direction");

                if (holders == null)
                {
                    registerMessage(channel, message);
                    return;
                }

                List<NetworkDirection> directions = holders.stream().map(h -> NetworkDirection.valueOf(h.getValue())).collect(Collectors.toList());
                for (NetworkDirection direction : directions)
                {
                    registerMessage(channel, message, direction);
                }
            }
            catch (ClassNotFoundException e)
            {
                OrmoyoUtil.LOGGER.fatal("Failed to find network message {} class", className);
            }
        });
    }

    private static <T extends AbstractMessage<T>> void registerMessage(SimpleChannel channel, Class<T> clazz)
    {
        registerMessage(channel, clazz, null);
    }

    private static <T extends AbstractMessage<T>> void registerMessage(SimpleChannel channel, Class<T> clazz, NetworkDirection direction)
    {
        int id = 0;

        if (idMap.containsKey(channel))
            id = idMap.get(channel);

        if (!decoders.containsKey(clazz))
        {
            OrmoyoUtil.LOGGER.fatal("Couldn't find network decoder for message {} or it doesn't exist", clazz.getName());
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
