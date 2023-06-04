package com.ormoyo.ormoyoutil.config;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = OrmoyoUtil.MODID)
public class ConfigHandler
{
    private static final String PREFIX = "config.ormoyoutil.";
    @Config.LangKey(PREFIX + "leveling_system")
    public static LevelingSystem LEVELING_SYSTEM = new LevelingSystem();

    public static class LevelingSystem
    {
        @Config.Name("Max Level")
        @Config.LangKey(PREFIX + "max_level")
        @Config.RangeInt(min = 1, max = 999)
        public short maxLevel = 20;

        @Config.Name("Entity EXP Count")
        @Config.LangKey(PREFIX + "entity_exp_count")
        @Config.Comment("The amount of exp a player will get if he kills the entity")
        public String[] entityExpCount = {"minecraft:ender_dragon=100"};
    }

    @Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID)
    private static class EventHandler
    {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
        {
            if (event.getModID().equals(OrmoyoUtil.MODID))
            {
                ConfigManager.sync(OrmoyoUtil.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
