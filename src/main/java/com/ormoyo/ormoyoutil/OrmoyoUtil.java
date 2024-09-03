package com.ormoyo.ormoyoutil;

import com.google.common.base.Preconditions;
import com.ormoyo.ormoyoutil.abilities.StatsAbility;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.AbilityKeybindingBase;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import com.ormoyo.ormoyoutil.ability.AbilityHolderImpl;
import com.ormoyo.ormoyoutil.capability.AbilityHolderStorage;
import com.ormoyo.ormoyoutil.client.OrmoyoResourcePackListener;
import com.ormoyo.ormoyoutil.network.NetworkChannel;
import com.ormoyo.ormoyoutil.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Optional;

@Mod(OrmoyoUtil.MODID)
public class OrmoyoUtil
{
    public static final String MODID = "ormoyoutil";
    public static final Logger LOGGER = LogManager.getLogger("OrmoyoUtil");

    @NetworkChannel
    public static SimpleChannel NETWORK_CHANNEL;

    public OrmoyoUtil()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(StatsAbility::onIMCHandle);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC, "ormoyoutil.toml");
    }

    private void setup(FMLCommonSetupEvent event)
    {
        for (ModInfo modInfo : ModList.get().getMods())
        {
            ModFileScanData data = modInfo.getOwningFile().getFile().getScanResult();
            Optional<? extends ModContainer> container = ModList.get().getModContainerById(modInfo.getModId());

            Preconditions.checkState(container.isPresent());
            NetworkHandler.injectNetworkWrapper(container.get(), data);
        }

        for (ModFileScanData data : ModList.get().getAllScanData())
        {
            NetworkHandler.registerNetworkMessages(data);
        }

        CapabilityManager.INSTANCE.register(AbilityHolder.class, new AbilityHolderStorage(), AbilityHolderImpl::new);
    }

    private void doClientStuff(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            Minecraft mc = event.getMinecraftSupplier().get();
            if (event.getMinecraftSupplier().get().getResourceManager() instanceof IReloadableResourceManager)
            {
                OrmoyoResourcePackListener listener = new OrmoyoResourcePackListener();

                IReloadableResourceManager manager = (IReloadableResourceManager) mc.getResourceManager();
                manager.addReloadListener(listener);

                listener.onResourceManagerReload(manager);
            }

            for (AbilityEntry entry : Ability.getAbilityRegistry().getValues())
            {
                if (AbilityKeybindingBase.class.isAssignableFrom(entry.getAbilityClass()))
                {
                    AbilityKeybindingBase ability = (AbilityKeybindingBase) entry.newInstance(Ability.getAbilityHolder(null));
                    if (ability.getKeyCode() <= 0 || ability.getKeyType() == null)
                        continue;

                    ResourceLocation location = ability.getRegistryName();
                    ClientRegistry.registerKeyBinding(
                            new KeyBinding(
                                    "key." + location.getNamespace() + "." + location.getPath(),
                                    ability.getKeyConflictContext(),
                                    ability.getKeyModifier(),
                                    ability.getKeyType(),
                                    ability.getKeyCode(),
                                    "key." + location.getNamespace() + ".category"));
                }
            }
        });
    }

    public static class Config
    {
        private static final ForgeConfigSpec SPEC;
        public static final LevelSystem LEVEL_SYSTEM;


        public static class LevelSystem
        {
            private LevelSystem(ForgeConfigSpec.Builder builder)
            {
                builder.comment("The Leveling System").push("Leveling");

                MAX_LEVEL = builder.comment("The max level a player can have")
                        .defineInRange("maxLevel", 20, 1, 999);

                USE_MINECRAFT_LEVELING = builder.comment("Use the minecraft leveling system instead of the mod's")
                        .define("useMinecraftLeveling", false);

                {
                    builder.comment("The percentage for getting exp from killing an entity").push("Success Rate");

                    HARD_SUCCESS_RATE = builder.defineInRange("Hard", 0.7d, 0d, 1d);
                    NORMAL_SUCCESS_RATE = builder.defineInRange("Normal", 0.9d, 0d, 1d);
                    EASY_SUCCESS_RATE = builder.defineInRange("Easy", 0.95d, 0d, 1d);
                    PEACEFUL_SUCCESS_RATE = builder.defineInRange("Peaceful", 1d, 0d, 1d);
                    builder.defineInList("Abilities", "afga", Arrays.asList("afga", "lol", "nice"));

                    builder.pop();
                }

                builder.pop();
            }

            public final ForgeConfigSpec.IntValue MAX_LEVEL;
            public final ForgeConfigSpec.BooleanValue USE_MINECRAFT_LEVELING;

            public final ForgeConfigSpec.DoubleValue HARD_SUCCESS_RATE;
            public final ForgeConfigSpec.DoubleValue NORMAL_SUCCESS_RATE;
            public final ForgeConfigSpec.DoubleValue EASY_SUCCESS_RATE;
            public final ForgeConfigSpec.DoubleValue PEACEFUL_SUCCESS_RATE;
        }

        static
        {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push("OrmoyoUtil");

            LEVEL_SYSTEM = new LevelSystem(builder);

            builder.pop();
            SPEC = builder.build();
        }
    }
}
