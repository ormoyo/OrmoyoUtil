package com.ormoyo.ormoyoutil;

import com.ormoyo.ormoyoutil.commands.CommandUnlockAbility;
import com.ormoyo.ormoyoutil.network.NetworkHandler;
import com.ormoyo.ormoyoutil.network.NetworkWrapper;
import com.ormoyo.ormoyoutil.proxy.CommonProxy;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = OrmoyoUtil.MODID,
        name = OrmoyoUtil.NAME,
        version = OrmoyoUtil.VERSION,
        acceptedMinecraftVersions = OrmoyoUtil.MINECRAFT_VERSION,
        useMetadata = true
)
public class OrmoyoUtil
{
    public static final String MODID = "ormoyoutil";
    public static final String NAME = "Ormoyo Util";
    public static final String VERSION = "1.0";
    public static final String MINECRAFT_VERSION = "[1.12.2]";

    @Instance
    public static OrmoyoUtil instance;

    public static final Logger LOGGER = LogManager.getLogger(OrmoyoUtil.MODID);

    @SidedProxy(serverSide = "com.ormoyo.ormoyoutil.proxy.CommonProxy", clientSide = "com.ormoyo.ormoyoutil.proxy.ClientProxy")
    public static CommonProxy PROXY;

    @NetworkWrapper
    public static SimpleNetworkWrapper NETWORK_WRAPPER;

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event)
    {
        for (ModContainer mod : Loader.instance().getModList())
        {
            NetworkHandler.INSTANCE.injectNetworkWrapper(mod, event.getAsmData());
        }

        NetworkHandler.INSTANCE.registerNetworkMessages(event.getAsmData());
        PROXY.preInit(event);
    }

    @EventHandler
    public static void init(FMLInitializationEvent event)
    {
        PROXY.init(event);
    }

    @EventHandler
    public static void postInit(FMLPostInitializationEvent event)
    {
        PROXY.postInit(event);
    }

    @EventHandler
    public static void ServerStart(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandUnlockAbility());
    }
}
