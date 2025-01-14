package com.ormoyo.ormoyoutil.ability;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.abilities.StatsAbility;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventEntry;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventListener;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventListenerImpl;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventPredicate;
import com.ormoyo.ormoyoutil.ability.util.ClientAbility;
import com.ormoyo.ormoyoutil.ability.util.ServerAbility;
import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import com.ormoyo.ormoyoutil.capability.AbilityHolderProvider;
import com.ormoyo.ormoyoutil.commands.AbilitiesCommand;
import com.ormoyo.ormoyoutil.commands.AcquireAbilityCommand;
import com.ormoyo.ormoyoutil.event.FontRenderEvent;
import com.ormoyo.ormoyoutil.network.MessageSetAbilities;
import com.ormoyo.ormoyoutil.network.MessageSetAbilityKeys;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IGenericEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.plexus.util.FastMap;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID)
class AbilityEventHandler
{
    @CapabilityInject(AbilityHolder.class)
    public static final Capability<AbilityHolder> ABILITY_HOLDER_CAPABILITY = null;

    private static final FastMap<Class<? extends Event>, AbilityEventList> LISTENERS = new FastMap<>(64);

    static final Set<Class<? extends Ability>> CLIENT_ABILITIES = Sets.newHashSet();
    static final Set<Class<? extends Ability>> SERVER_ABILITIES = Sets.newHashSet();
    static final Set<Class<? extends Ability>> SHARED_ABILITIES = Sets.newHashSet();

    static Map<Class<? extends Ability>, AbilityEntry<?>> CLASSES_TO_ENTRIES;

    static ForgeRegistry<AbilityEntry<?>> ABILITY_REGISTRY;
    static IForgeRegistry<AbilityEventEntry> ABILITY_EVENT_REGISTRY;

    @SuppressWarnings("ConstantConditions")
    public static void onInit()
    {
        try
        {
            ABILITY_REGISTRY.freeze();
            CLASSES_TO_ENTRIES = new HashMap<>(ABILITY_REGISTRY.getValues().size());

            for (AbilityEntry<?> entry : Ability.getAbilityRegistry())
            {
                CLASSES_TO_ENTRIES.put(entry.getAbilityClass(), entry);

                Constructor<? extends Ability> constructor = entry.getAbilityClass().getConstructor(AbilityHolder.class);
                entry.abilityConstructor = ASMUtils.createConstructorCallback(Function.class, constructor);

                for (Method method : entry.getAbilityClass().getMethods())
                {
                    if (!method.isAnnotationPresent(SubscribeEvent.class))
                        continue;

                    Class<? extends Event> eventType = AbilityEventHandler.getEventParameter(method);
                    AbilityEventList list = AbilityEventHandler.getListenerList(eventType);

                    for (AbilityEventEntry eventEntry : Ability.getAbilityEventRegistry().getValues())
                    {
                        if (!eventType.isAssignableFrom(eventEntry.getEventClass()))
                            continue;
                        if (!eventEntry.getEventClass().isAssignableFrom(eventType))
                            continue;

                        AbilityEventListenerImpl listener = null;
                        try
                        {
                            AbilityEventListenerImpl listener = new AbilityEventListenerImpl(entry, method, eventType, eventEntry.getEventPredicate(), IGenericEvent.class.isAssignableFrom(eventType));
                            list.register(listener.getPriority(), listener);
                        }
                    }
                }
                            listener = new AbilityEventListenerImpl(entry, method, eventType, eventEntry.getEventPredicate(), IGenericEvent.class.isAssignableFrom(eventType));
                        } catch (ReflectiveOperationException e)
                        {
                            throw new RuntimeException(e);
                        }
                        list.register(listener.getPriority(), listener);
                    }
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            OrmoyoUtil.LOGGER.error("A critical error has occurred by reflection on init");
        }
    }

    private static Class<? extends Event> getEventParameter(Method method)
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

        return (Class<? extends Event>) eventT;
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

        Collection<AbilityEntry<?>> entries = Ability.getAbilityRegistry().getValues()
                .stream()
                .filter(entry -> entry.getLevel() <= 1)
                .filter(entry -> entry.getCondition() == null || entry.getConditionCheckingEvents().length == 0)
                .filter(entry -> AbilityEventHandler.CLIENT_ABILITIES.contains(entry.getAbilityClass()))
                .collect(Collectors.toList());

        abilityHolder.setAbilities(entries
                .stream()
                .map(entry -> entry.newInstance(abilityHolder))
                .collect(Collectors.toList()));

        OrmoyoUtil.NETWORK_CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                new MessageSetAbilities(abilityHolder, entries));
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
                    new MessageSetAbilities(abilityHolder, abilityHolder.getAbilities().stream()
                            .filter(Ability::isClientAbility)
                            .filter(Ability::isSharedByClients)
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
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        AcquireAbilityCommand.register(event.getDispatcher());
        AbilitiesCommand.register(event.getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID, value = Dist.DEDICATED_SERVER)
    private static class ServerEventHandler
    {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onEvent(Event event)
        {
            if (ServerLifecycleHooks.getCurrentServer() == null)
                return;

            List<ServerPlayerEntity> players = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
            for (PlayerEntity player : players)
                AbilityEventHandler.handleEvent(event, player);
        }
    }

    @Mod.EventBusSubscriber(modid = OrmoyoUtil.MODID, value = Dist.CLIENT)
    static class ClientEventHandler
    {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onEvent(Event event)
        {
            if (EffectiveSide.get().isServer())
            {
                if (ServerLifecycleHooks.getCurrentServer() == null)
                    return;

                List<ServerPlayerEntity> players = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
                for (PlayerEntity player : players)
                    AbilityEventHandler.handleEvent(event, player);

                return;
            }

            if (Minecraft.getInstance().world == null)
                return;

            List<AbstractClientPlayerEntity> players = Minecraft.getInstance().world.getPlayers();
            for (PlayerEntity player : players)
            {
                AbilityEventHandler.handleEvent(event, player);
            }
        }

        public static void clientSetup(FMLClientSetupEvent event)
        {
            Minecraft mc = event.getMinecraftSupplier().get();
            for (AbilityEntry<?> abilityEntry : Ability.getAbilityRegistry().getValues())
            {
                if (abilityEntry.keyBindings.isEmpty())
                    continue;

                String namespace = Objects.requireNonNull(abilityEntry.getRegistryName()).getNamespace();
                List<AbilityEntry.KeyBinding> keys = abilityEntry.keyBindings;

                for (int i = 0; i < keys.size(); i++)
                {
                    AbilityEntry.KeyBinding.ClientKeyBinding key = (AbilityEntry.KeyBinding.ClientKeyBinding) keys.get(i);
                    AbilityEntryBuilder.AbilityKeybinding keybinding = key.keybinding;

                    String desc = "key." + namespace + "." + keybinding.name;
                    ClientRegistry.registerKeyBinding(new KeyBinding(
                            desc,
                            keybinding.context,
                            keybinding.modifier,
                            InputMappings.Type.valueOf(keybinding.type.desc),
                            keybinding.code,
                            "key." + namespace + ".category"));

                    AbilityEntry.KeyBinding entryKey = abilityEntry.keyBindings.get(i);
                    int index = mc.gameSettings.keyBindings.length - 1;

                    abilityEntry.keyBindings.set(i, new AbilityEntry.KeyBinding(index, entryKey.getCooldown(), desc));
                }
            }
        }

        public static void onKeybindBaseConstruct(AbilityKeybindingBase ability)
        {
            if (Minecraft.getInstance().getConnection() == null)
                return;
            if (EffectiveSide.get().isServer())
                return;

            for (KeyBinding keybind : ability.getKeyBindings())
            {
                if (keybind == null)
                {
                    OrmoyoUtil.LOGGER.warn("Ability {} has null keybindings", ability.getRegistryName());
                    continue;
                }

                KeyBinding key = ability.getKeybinding();
                if (key != null && Objects.equals(key.getKeyDescription(), keybind.getKeyDescription()))
                {
                    ability.hasBeenPressed.put(null, new MutableBoolean());
                    continue;
                }

                ability.hasBeenPressed.put(keybind.getKeyDescription(), new MutableBoolean());
                AbilityKeybindingBase.KEYBIND_IDS.putIfAbsent(keybind.getKeyDescription(), AbilityKeybindingBase.KEYBIND_IDS.size() + 1);
            }

            if (AbilityKeybindingBase.KEYBIND_IDS.isEmpty())
                return;

            OrmoyoUtil.NETWORK_CHANNEL.sendToServer(new MessageSetAbilityKeys(AbilityKeybindingBase.KEYBIND_IDS));
        }

        public static void registerAbilityEventPredicatesOnClient(RegistryEvent.Register<AbilityEventEntry> event)
        {
            ModBusEventHandler.register(event, FontRenderEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, TickEvent.ClientTickEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, TickEvent.RenderTickEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, RenderGameOverlayEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, EntityViewRenderEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, InputEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, GuiScreenEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, GuiOpenEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, RenderHandEvent.class, ModBusEventHandler.ClientEventPredicates.defaultClientPredicate());
            ModBusEventHandler.register(event, RenderLivingEvent.class, ModBusEventHandler.ClientEventPredicates.RENDER_LIVING_EVENT);
            ModBusEventHandler.register(event, RenderArmEvent.class, ModBusEventHandler.ClientEventPredicates.RENDER_ARM_EVENT);
            ModBusEventHandler.register(event, ClientPlayerNetworkEvent.class, ModBusEventHandler.ClientEventPredicates.CLIENT_PLAYER_NETWORK_EVENT);
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
                ModBusEventHandler.registerAbilitiesOnSide(scanData);
            }
        }

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event)
        {
            event.enqueueWork(AbilityEventHandler::onInit);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> ClientEventHandler.clientSetup(event));
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
                    OrmoyoUtil.LOGGER.fatal("Failed to load ability {}", annotationData.getClassType().getClassName());
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
                    Boolean share = (Boolean) annotationData.getAnnotationData().get("share");

                    if (share != null && share)
                    {
                        SHARED_ABILITIES.add(clazz);
                        SERVER_ABILITIES.add(clazz);
                        CLIENT_ABILITIES.add(clazz);

                        continue;
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
                    continue;
                }

                if (holders.stream().noneMatch(side -> Dist.valueOf(side.getValue()) == FMLEnvironment.dist))
                    continue;

                SERVER_ABILITIES.add(clazz);
            }
        }

        @SubscribeEvent
        public static void onNewRegistry(RegistryEvent.NewRegistry event)
        {
            ABILITY_REGISTRY = (ForgeRegistry<AbilityEntry<?>>) new RegistryBuilder<AbilityEntry<?>>()
                    .setName(new ResourceLocation(OrmoyoUtil.MODID, "ability"))
                    .setType(ASMUtils.castRegistry(AbilityEntry.class))
                    .setIDRange(0, 2048)
                    .allowModification()
                    .add((IForgeRegistry.AddCallback<AbilityEntry<?>>) (owner, stage, id, entry, oldEntry) ->
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

        @SuppressWarnings("unchecked") //Ugly hack to let us pass in a typed Class object. Remove when we remove type specific references.
        private static <T> Class<T> c(Class<?> cls)
        {
            return (Class<T>)cls;
        }

        @SubscribeEvent
        public static void registerAbilities(RegistryEvent.Register<AbilityEntry<?>> event)
        {
            event.getRegistry().register(AbilityEntryBuilder.<StatsAbility>create()
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

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEventHandler.registerAbilityEventPredicatesOnClient(event));
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

    private static void handleEvent(Event event, PlayerEntity player)
    {
        AbilityHolder abilityHolder = Ability.getAbilityHolder(player);
        if (abilityHolder == null)
            return;

        AbilityEventList listenerList = AbilityEventHandler.getListenerList(event.getClass());
        for (Ability ability : abilityHolder.getAbilities())
        {
            if (!ability.isEnabled())
                break;

             Collection<AbilityEventListener> listeners = listenerList.getListeners(ability.getEntry());
             for (AbilityEventListener listener : listeners)
             {
                 if (!listener.getEventPredicate().test(ability, event))
                     continue;

                 listener.invoke(ability, event);
             }
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
    }

    private static final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static AbilityEventList getListenerList(Class<? extends Event> eventClass)
    {
        Lock readLock = lock.readLock();

        readLock.lock();
        AbilityEventList listenerList = LISTENERS.get(eventClass);
        readLock.unlock();

        if (listenerList == null)
        {
            listenerList = computeEventList(eventClass);
            Lock writeLock = lock.writeLock();

            writeLock.lock();
            readLock.lock();

            LISTENERS.putIfAbsent(eventClass, listenerList);

            listenerList = LISTENERS.get(eventClass);

            readLock.unlock();
            writeLock.unlock();
        }
        return listenerList;
    }

    private static AbilityEventList computeEventList(Class<? extends Event> eventClass)
    {
        if (eventClass == Event.class)
            return new AbilityEventList();

        Class<? extends Event> superclass = (Class<? extends Event>) eventClass.getSuperclass();
        AbilityEventList parentList = AbilityEventHandler.getListenerList(superclass);

        return new AbilityEventList(parentList);
    }
}
