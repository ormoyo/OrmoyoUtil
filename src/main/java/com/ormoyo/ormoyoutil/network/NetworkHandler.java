package com.ormoyo.ormoyoutil.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation.EnumHolder;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum NetworkHandler
{
    INSTANCE;

    private final Map<String, SimpleNetworkWrapper> wrappers = Maps.newHashMap();
    private final Map<SimpleNetworkWrapper, Integer> idMap = Maps.newHashMap();

    public void injectNetworkWrapper(ModContainer mod, ASMDataTable data)
    {
        SetMultimap<String, ASMDataTable.ASMData> annotations = data.getAnnotationsFor(mod);

        if (annotations == null)
        {
            return;
        }

        Set<ASMDataTable.ASMData> targetList = annotations.get(NetworkWrapper.class.getName());
        ClassLoader classLoader = Loader.instance().getModClassLoader();
        for (ASMDataTable.ASMData target : targetList)
        {
            try
            {
                Class<?> targetClass = Class.forName(target.getClassName(), true, classLoader);
                Field field = targetClass.getDeclaredField(target.getObjectName());
                field.setAccessible(true);
                SimpleNetworkWrapper networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(mod.getModId());
                field.set(null, networkWrapper);
                this.wrappers.putIfAbsent(mod.getModId(), networkWrapper);
            }
            catch (Exception e)
            {
                OrmoyoUtil.LOGGER.fatal("Failed to inject network wrapper for mod container {}", mod, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage<T>> void registerNetworkMessages(ASMDataTable data)
    {
        List<ASMData> datas = Lists.newArrayList(data.getAll(NetworkMessage.class.getName()));
        datas.sort((a, b) -> a.getClassName().compareTo(b.getClassName()));
//		if(i == 0) {
//			List<EnumHolder> aholders = (List<EnumHolder>) a.getAnnotationInfo().get("side");
//			List<EnumHolder> bholders = (List<EnumHolder>) b.getAnnotationInfo().get("side");
//			int length = Math.min(aholders.size(), bholders.size());
//			for(int j = 0; j < length; j++) {
//				Side aside = Side.valueOf(aholders.get(j).getValue());
//				Side bside = Side.valueOf(bholders.get(j).getValue());
//				if(aside != bside) {
//					return aside.compareTo(bside);
//				}
//			}
//			return aholders.size() - bholders.size();
//		}
        for (ASMData target : datas)
        {
            String modid = (String) target.getAnnotationInfo().get("modid");
            if (!this.wrappers.containsKey(modid))
            {
                OrmoyoUtil.LOGGER.fatal("Couldn't register network message {} because the mod {} doesn't exist or doesn't have a wrapper", target.getClassName(), modid);
                return;
            }

            try
            {
                Class<?> clazz = Class.forName(target.getClassName());
                if (!AbstractMessage.class.isAssignableFrom(clazz))
                {
                    OrmoyoUtil.LOGGER.fatal("Network message {} doesn't extend AbstractMessage", target.getClassName());
                    return;
                }

                Class<T> message = (Class<T>) clazz;
                SimpleNetworkWrapper wrapper = this.wrappers.get(modid);
                List<EnumHolder> holders = (List<EnumHolder>) target.getAnnotationInfo().get("side");

                Side[] sides = holders == null ? Side.values() : holders.stream().map(h -> Side.valueOf(h.getValue())).collect(Collectors.toList()).toArray(new Side[holders.size()]);
                for (Side side : sides)
                {
                    registerMessage(wrapper, message, side);
                }
            }
            catch (ClassNotFoundException e)
            {
                OrmoyoUtil.LOGGER.fatal("Failed to find network message {} class", target.getClassName());
            }
        }
    }

    public <T extends AbstractMessage<T> & IMessageHandler<T, IMessage>> void registerMessage(SimpleNetworkWrapper networkWrapper, Class<T> clazz)
    {
        try
        {
            NetworkMessage meta = clazz.getDeclaredAnnotation(NetworkMessage.class);
            for (Side side : meta.side())
            {
                registerMessage(networkWrapper, clazz, side);
            }
        }
        catch (NullPointerException e)
        {
            OrmoyoUtil.LOGGER.fatal("Network message {} doesn't have a NetworkMessage annotation", clazz.getName());
        }
    }

    @Deprecated
    public <T extends AbstractMessage<T> & IMessageHandler<T, IMessage>> void registerMessage(SimpleNetworkWrapper networkWrapper, Class<T> clazz, Side side)
    {
        int id = 0;
        if (this.idMap.containsKey(networkWrapper))
        {
            id = this.idMap.get(networkWrapper);
        }

        networkWrapper.registerMessage(clazz, clazz, id, side);
        this.idMap.put(networkWrapper, id + 1);
    }
}
