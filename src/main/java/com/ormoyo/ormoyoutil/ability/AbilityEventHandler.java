package com.ormoyo.ormoyoutil.ability;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.abilities.StatsAbility;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventEntry;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventListenerImpl;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventListener;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventPredicate;
import com.ormoyo.ormoyoutil.ability.util.ClientAbility;
import com.ormoyo.ormoyoutil.ability.util.ServerAbility;
import com.ormoyo.ormoyoutil.capability.AbilityHolderProvider;
import com.ormoyo.ormoyoutil.commands.AbilitiesCommand;
import com.ormoyo.ormoyoutil.commands.AcquireAbilityCommand;
import com.ormoyo.ormoyoutil.event.FontRenderEvent;
import com.ormoyo.ormoyoutil.network.MessageSetAbilities;
import com.ormoyo.ormoyoutil.network.MessageSetAbilityKeys;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IGenericEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID)
class AbilityEventHandler
{
    @CapabilityInject(AbilityHolder.class)
    public static final Capability<AbilityHolder> ABILITY_HOLDER_CAPABILITY;

    private static final Table<AbilityEntry, Class<? extends Event>, AbilityEventListener> LISTENERS;
    private static final BiConsumer<Event, PlayerEntity> EVENT_ACTION;

    static final Collection<Class<? extends Ability>> CLIENT_ABILITIES = Sets.newHashSet();
    static final Collection<Class<? extends Ability>> SERVER_ABILITIES = Sets.newHashSet();

    static final Collection<Class<? extends Ability>> SHARED_ABILITIES = Sets.newHashSet();

    static Map<AbilityEntry, ITextComponent> ABILITY_DISPLAY_NAMES;

    static IForgeRegistry<AbilityEntry> ABILITY_REGISTRY;
    static IForgeRegistry<AbilityEventEntry> ABILITY_EVENT_REGISTRY;

    @SuppressWarnings("ConstantConditions")
    public static void onInit()
    {
        try
        {
            for (AbilityEntry entry : Ability.getAbilityRegistry())
            {
                // || Constructor ||
                Constructor<? extends Ability> constructor = entry.getAbilityClass().getConstructor(AbilityHolder.class);
                entry.abilityConstructor = ASMUtils.createConstructorCallback(Function.class, constructor);

                // || Methods ||
                Collection<Method> eventMethods = Stream.of(entry.getAbilityClass().getMethods()).filter(method -> method.isAnnotationPresent(SubscribeEvent.class)).collect(Collectors.toSet());

                for (Method method : eventMethods)
                {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != 1)
                    {
                        throw new IllegalArgumentException(
                                "Ability method " + method + " has @SubscribeEvent annotation, but requires " + parameterTypes.length +
                                        " arguments. Event handler methods must require a single argument."
                        );
                    }

                    Class<?> eventT = parameterTypes[0];

                    if (!Event.class.isAssignableFrom(eventT))
                        throw new IllegalArgumentException("Ability method " + method + " has @SubscribeEvent annotation, but takes a argument that is not an Event " + eventT);

                    Class<? extends Event> eventType = (Class<? extends Event>) eventT;
                    for (AbilityEventEntry eventEntry : Ability.getAbilityEventRegistry().getValues())
                    {
                        if (eventType.isAssignableFrom(eventEntry.getEventClass()) || eventEntry.getEventClass().isAssignableFrom(eventType))
                        {
                            LISTENERS.cellSet().removeIf(cell -> cell.getColumnKey().isAssignableFrom(eventType) && cell.getValue().getMethod().equals(method));
                            LISTENERS.put(entry, eventType, new AbilityEventListenerImpl(entry, method, eventType, eventEntry.getEventPredicate(), IGenericEvent.class.isAssignableFrom(eventType)));
                        }
                    }
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        AbilityHolder abilityHolder = Ability.getAbilityHolder(event.player);
        if (abilityHolder == null)
            return;

        for (Ability ability : abilityHolder.getAbilities())
        {
            if (!ability.isEnabled())
                return;

            ability.tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        AbilityHolder abilityHolder = Ability.getAbilityHolder(event.getPlayer());

        if (abilityHolder == null)
            return;

        Collection<AbilityEntry> abilities = abilityHolder.getAbilities()
                .stream()
                .map(Ability::getEntry)
                .collect(Collectors.toList());

        Collection<AbilityEntry> entries = Ability.getAbilityRegistry().getValues()
                .stream()
                .filter(entry -> (abilities.contains(entry) ||
                        (entry.getLevel() <= 1 && (entry.getCondition() == null || entry.getConditionCheckingEvents().length == 0))) &&
                        AbilityEventHandler.CLIENT_ABILITIES.contains(entry.getAbilityClass()))
                .collect(Collectors.toList());

        abilityHolder.setAbilities(entries.stream()
                .map(entry -> entry.newInstance(abilityHolder))
                .collect(Collectors.toList()));

        OrmoyoUtil.NETWORK_CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                new MessageSetAbilities(event.getPlayer(), entries));
    }

    @SubscribeEvent
    public static void onStartTrack(PlayerEvent.StartTracking event)
    {
        if (event.getTarget() instanceof PlayerEntity)
        {
            PlayerEntity targetedPlayer = (PlayerEntity) event.getTarget();
            AbilityHolder abilityHolder = Ability.getAbilityHolder(targetedPlayer);

            if (abilityHolder == null)
                return;

            OrmoyoUtil.NETWORK_CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                    new MessageSetAbilities(targetedPlayer, abilityHolder.getAbilities().stream()
                            .filter(Ability::isClientAbility)
                            .filter(ability -> SHARED_ABILITIES.contains(ability.getClass()))
                            .map(Ability::getEntry)
                            .collect(Collectors.toList())));
        }
    }

    @SubscribeEvent
    public static void onClonePlayer(PlayerEvent.Clone event)
    {
        AbilityHolder original = Ability.getAbilityHolder(event.getOriginal());
        AbilityHolder abilityHolder = Ability.getAbilityHolder(event.getPlayer());

        if (original != null && abilityHolder != null)
        {
            abilityHolder.setAbilities(original.getAbilities());
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) event.getObject();
            AbilityHolderProvider provider = new AbilityHolderProvider(player);

            event.addCapability(new ResourceLocation(OrmoyoUtil.MODID, "ability_holder_capability"), provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        AcquireAbilityCommand.register(event.getDispatcher());
        AbilitiesCommand.register(event.getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID, value = Dist.DEDICATED_SERVER)
    private static class ServerEventHandler
    {
        @SubscribeEvent
        public static void onEvent(Event event)
        {
            if (ServerLifecycleHooks.getCurrentServer() == null)
                return;

            PlayerList list = ServerLifecycleHooks.getCurrentServer().getPlayerList();
            for (ServerPlayerEntity player : list.getPlayers())
            {
                EVENT_ACTION.accept(event, player);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID, value = Dist.CLIENT)
    static class ClientEventHandler
    {
        @SubscribeEvent
        public static void onEvent(Event event)
        {
            if (EffectiveSide.get().isServer())
            {
                if (ServerLifecycleHooks.getCurrentServer() == null)
                    return;

                PlayerList list = ServerLifecycleHooks.getCurrentServer().getPlayerList();
                for (ServerPlayerEntity player : list.getPlayers())
                    EVENT_ACTION.accept(event, player);

                return;
            }

            if (Minecraft.getInstance().world == null)
                return;

            List<? extends PlayerEntity> players = Minecraft.getInstance().world.getPlayers();
            for (PlayerEntity player : players)
            {
                EVENT_ACTION.accept(event, player);
            }
        }

        static AbilityKeybindingBase currentConstruct;

        public static void onKeybindBaseConstruct()
        {
            if (currentConstruct == null)
                return;

            if (Minecraft.getInstance().getConnection() == null)
                return;

            if (EffectiveSide.get().isServer())
                return;

            for (KeyBinding keybind : currentConstruct.getKeybinds())
            {
                KeyBinding key = currentConstruct.getKeybind();
                if (key != null && Objects.equals(key.getKeyDescription(), keybind.getKeyDescription()))
                {
                    currentConstruct.hasBeenPressed.put(null, new MutableBoolean());
                    continue;
                }

                currentConstruct.hasBeenPressed.put(keybind.getKeyDescription(), new MutableBoolean());
                AbilityKeybindingBase.KEYBIND_IDS.put(keybind.getKeyDescription(), AbilityKeybindingBase.KEYBIND_IDS.size() + 1);
            }

            OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageSetAbilityKeys(AbilityKeybindingBase.KEYBIND_IDS.inverse()));
            currentConstruct = null;
        }
    }

    @Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    private static class ModBusEventHandler
    {
        @SubscribeEvent
        public static void onConstruct(FMLConstructModEvent event)
        {
            for (ModFileScanData scanData : ModList.get().getAllScanData())
            {
                registerAbilitiesOnSide(scanData);
            }
        }

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event)
        {
            event.enqueueWork(() ->
            {
                AbilityEventHandler.onInit();
                ABILITY_DISPLAY_NAMES = new HashMap<>(ABILITY_REGISTRY.getEntries().size());
            });
        }

        private static void registerAbilitiesOnSide(ModFileScanData scanData)
        {
            Function<ModFileScanData.AnnotationData, Class<? extends Ability>> CONVERT_TO_CLASS = annotationData ->
            {
                try
                {
                    return (Class<? extends Ability>) Class.forName(annotationData.getClassType().getClassName());
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            };

            for (ModFileScanData.AnnotationData annotationData : scanData.getAnnotations())
            {
                boolean isClientAbility = Type.getType(ClientAbility.class).equals(annotationData.getAnnotationType());
                boolean isServerAbility = Type.getType(ServerAbility.class).equals(annotationData.getAnnotationType());

                if (isClientAbility)
                {
                    Class<? extends Ability> clazz = CONVERT_TO_CLASS.apply(annotationData);
                    boolean share = (boolean) annotationData.getAnnotationData().get("share");

                    if (share)
                    {
                        SHARED_ABILITIES.add(clazz);
                        SERVER_ABILITIES.add(clazz);
                        CLIENT_ABILITIES.add(clazz);

                        return;
                    }

                    CLIENT_ABILITIES.add(clazz);
                    continue;
                }

                if (!isServerAbility)
                    continue;

                Class<? extends Ability> clazz = CONVERT_TO_CLASS.apply(annotationData);
                List<ModAnnotation.EnumHolder> holders = (List<ModAnnotation.EnumHolder>) annotationData.getAnnotationData().get("value");

                if (holders == null)
                {
                    SERVER_ABILITIES.add(clazz);
                    return;
                }

                if (holders.stream().noneMatch(side -> Dist.valueOf(side.getValue()) == FMLEnvironment.dist))
                    return;

                SERVER_ABILITIES.add(clazz);
            }
        }

        @SubscribeEvent
        public static void onNewRegistry(RegistryEvent.NewRegistry event)
        {
            ABILITY_REGISTRY = new RegistryBuilder<AbilityEntry>()
                    .setName(new ResourceLocation(OrmoyoUtil.MODID, "ability"))
                    .setType(AbilityEntry.class)
                    .setIDRange(0, 2048)
                    .allowModification()
                    .add((IForgeRegistry.AddCallback<AbilityEntry>) (owner, stage, id, entry, oldEntry) ->
                    {
                        IForgeRegistryModifiable<?> registry = (IForgeRegistryModifiable<?>) owner;

                        boolean isClientAbility = AbilityEventHandler.CLIENT_ABILITIES.contains(entry.getAbilityClass());
                        boolean isServerAbility = AbilityEventHandler.SERVER_ABILITIES.contains(entry.getAbilityClass());

                        if (!isClientAbility && !isServerAbility)
                        {
                            AbilityEventHandler.CLIENT_ABILITIES.add(entry.getAbilityClass());
                            AbilityEventHandler.SERVER_ABILITIES.add(entry.getAbilityClass());

                            return;
                        }

                        LogicalSide side = EffectiveSide.get();
                        switch (side)
                        {
                            case CLIENT:
                                if (isClientAbility)
                                    return;

                                registry.remove(entry.getRegistryName());
                                break;
                            case SERVER:
                                if (isServerAbility)
                                    return;

                                registry.remove(entry.getRegistryName());
                                break;
                        }
                    })
                    .create();

            ABILITY_EVENT_REGISTRY = new RegistryBuilder<AbilityEventEntry>()
                    .setName(new ResourceLocation(OrmoyoUtil.MODID, "ability_event"))
                    .setType(AbilityEventEntry.class)
                    .setIDRange(0, 2048)
                    .create();
        }

        @SubscribeEvent
        public static void registerAbilities(RegistryEvent.Register<AbilityEntry> event)
        {
            event.getRegistry().register(AbilityEntryBuilder.create()
                    .ability(StatsAbility.class)
                    .id(new ResourceLocation(OrmoyoUtil.MODID, "stats"))
                    .build());
        }

        @SubscribeEvent
        public static void registerAbilityEventPredicates(RegistryEvent.Register<AbilityEventEntry> event)
        {
            register(event, EntityEvent.class, EventPredicates.ENTITY_EVENT);
            register(event, LivingAttackEvent.class, EventPredicates.LIVING_ATTACK_EVENT);
            register(event, LivingDeathEvent.class, EventPredicates.LIVING_DEATH_EVENT);
            register(event, ProjectileImpactEvent.class, EventPredicates.PROJECTILE_IMPACT_EVENT);
        }

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void registerAbilityEventPredicatesOnClient(RegistryEvent.Register<AbilityEventEntry> event)
        {
            register(event, FontRenderEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, TickEvent.ClientTickEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, TickEvent.RenderTickEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, RenderGameOverlayEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, EntityViewRenderEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, InputEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, GuiScreenEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, GuiOpenEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, RenderHandEvent.class, ClientEventPredicates.defaultClientPredicate());
            register(event, RenderLivingEvent.class, ClientEventPredicates.RENDER_LIVING_EVENT);
            register(event, RenderArmEvent.class, ClientEventPredicates.RENDER_ARM_EVENT);
            register(event, ClientPlayerNetworkEvent.class, ClientEventPredicates.CLIENT_PLAYER_NETWORK_EVENT);
        }

        private static <T extends Event> void register(RegistryEvent.Register<AbilityEventEntry> event, Class<T> clazz, AbilityEventPredicate<T> predicate)
        {
            StringBuilder name = new StringBuilder();
            char[] chars = clazz.getSimpleName().toCharArray();

            for (int i = 0; i < chars.length; i++)
            {
                if (i == 0)
                {
                    name.append(Character.toLowerCase(chars[i]));
                    continue;
                }

                char c = chars[i];
                if (Character.isUpperCase(c))
                {
                    name.append("_").append(Character.toLowerCase(c));
                }
            }

            event.getRegistry().register(new AbilityEventEntry(new ResourceLocation(OrmoyoUtil.MODID, name.toString()), clazz, predicate));
        }

        public static class EventPredicates
        {
            public static final AbilityEventPredicate<EntityEvent>
                    ENTITY_EVENT =
                    (ability, event) ->
                            ability.getOwner().equals(event.getEntity());

            public static final AbilityEventPredicate<LivingAttackEvent>
                    LIVING_ATTACK_EVENT =
                    (ability, event) ->
                            ability.getOwner().equals(event.getEntityLiving()) ||
                                    ability.getOwner().equals(event.getSource().getTrueSource());

            public static final AbilityEventPredicate<LivingDeathEvent>
                    LIVING_DEATH_EVENT =
                    (ability, event) ->
                            ability.getOwner().equals(event.getEntityLiving()) ||
                                    ability.getOwner().equals(event.getSource().getTrueSource());

            public static final AbilityEventPredicate<ProjectileImpactEvent>
                    PROJECTILE_IMPACT_EVENT =
                    (ability, event) ->
                            ability.getOwner().equals(((ProjectileEntity) event.getEntity()).getShooter()) ||
                                    ability.getOwner().equals(event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY ?
                                            ((EntityRayTraceResult) event.getRayTraceResult()).getEntity() : null);
        }

        @OnlyIn(Dist.CLIENT)
        public static class ClientEventPredicates
        {

            private static <T extends Event> AbilityEventPredicate<T> defaultClientPredicate()
            {
                return (ability, event) ->
                        ability.getOwner().equals(Minecraft.getInstance().player);
            }

            @SuppressWarnings("rawtypes")
            public static final AbilityEventPredicate<RenderLivingEvent>
                    RENDER_LIVING_EVENT =
                    (ability, event) ->
                            !ability.getOwner().equals(event.getEntity());


            public static final AbilityEventPredicate<RenderArmEvent>
                    RENDER_ARM_EVENT =
                    (ability, event) ->
                            ability.getOwner().equals(event.getPlayer());

            public static final AbilityEventPredicate<ClientPlayerNetworkEvent>
                    CLIENT_PLAYER_NETWORK_EVENT =
                    (ability, event) ->
                            ability.getOwner().equals(event.getPlayer());
        }
    }

    static
    {
        ABILITY_HOLDER_CAPABILITY = null;

        LISTENERS = HashBasedTable.create();
        EVENT_ACTION = (event, player) ->
        {
            AbilityHolder abilityHolder = Ability.getAbilityHolder(player);

            if (abilityHolder == null)
                return;

            for (Ability ability : abilityHolder.getAbilities())
            {
                if (!ability.isEnabled())
                    continue;

                AbilityEventListener listener = LISTENERS.get(ability.getEntry(), event.getClass());

                if (listener == null)
                    continue;

                if (!listener.getEventPredicate().test(ability, event))
                    continue;

                listener.invoke(ability, event);
            }

            if (!(event instanceof PlayerEvent))
                return;

            for (AbilityEntry entry : Ability.getAbilityRegistry().getValues())
            {
                for (Class<? extends Event> clazz : entry.getConditionCheckingEvents())
                {
                    if (clazz == event.getClass() && entry.getCondition().test(abilityHolder))
                    {
                        abilityHolder.unlockAbility(entry);
                    }
                }
            }
        };
    }
}
