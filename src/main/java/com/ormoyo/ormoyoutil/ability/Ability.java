package com.ormoyo.ormoyoutil.ability;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.abilities.StatsAbility;
import com.ormoyo.ormoyoutil.network.MessageSetAbilities;
import com.ormoyo.ormoyoutil.network.datasync.AbilityDataParameter;
import com.ormoyo.ormoyoutil.network.datasync.AbilitySyncManager;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
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
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.lang.invoke.LambdaConversionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Ability
{
    public Ability(IAbilityHolder owner)
    {
        this.owner = (PlayerEntity) owner;
        this.entry = Ability.getAbilityClassEntry(this.getClass());

        this.syncManager = new AbilitySyncManager(this);
        ABILITY_DISPLAY_NAMES.put(this.getClass(), this.getTranslatedName());

        this.abilityInit();
    }

    private static final AbilityDataParameter<Boolean> IS_ENABLED = AbilitySyncManager.createKey(Ability.class, DataSerializers.BOOLEAN);

    private final AbilityEntry entry;
    protected final AbilitySyncManager syncManager;

    protected final PlayerEntity owner;

    protected boolean isEnabled = true;

    /**
     * Called every tick
     */
    public void tick()
    {
    }

    /**
     * Called for registering data parameters
     */
    public void abilityInit()
    {
        this.syncManager.register(IS_ENABLED, true);
    }

    /**
     * Called when a player unlocks this ability
     */
    public void onUnlock()
    {
    }

    public void writeToNBT(CompoundNBT compound)
    {
    }

    public void readFromNBT(CompoundNBT compound)
    {
    }

    public void setIsEnabled(boolean isEnabled)
    {
        this.syncManager.set(IS_ENABLED, isEnabled);
    }

    /**
     * If the ability is disabled all it's methods wouldn't get called
     */
    public boolean isEnabled()
    {
        return this.syncManager.get(IS_ENABLED);
    }

    public void onAbilityEnabled()
    {
    }

    public void onAbilityDisabled()
    {
    }

    public void notifySyncManagerChange(AbilityDataParameter<?> parameter)
    {
    }

    public PlayerEntity getOwner()
    {
        return this.owner;
    }

    public String getName()
    {
        String registryName = String.valueOf(this.entry);
        return StringUtils.capitalize(registryName.substring(registryName.lastIndexOf('.')));
    }

    public ITextComponent getTranslatedName()
    {
        return new TranslationTextComponent("ability." + this.getRegistryName().getNamespace() + "." + this.getRegistryName().getPath() + ".name");
    }

    public final ResourceLocation getRegistryName()
    {
        return this.entry.getRegistryName();
    }

    public final AbilityEntry getEntry()
    {
        return this.entry;
    }

    public AbilitySyncManager getSyncManager()
    {
        return this.syncManager;
    }

    protected final void sendMessageToAbility(Ability ability, AbilityMessage message)
    {
        ability.getMessageFromAbility(this, message);
    }

    protected void getMessageFromAbility(Ability sender, AbilityMessage message)
    {
    }

    @Override
    public String toString()
    {
        return this.getName();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj instanceof Ability)
        {
            Ability ability = (Ability) obj;
            return this.entry.equals(ability.entry);
        }
        return false;
    }

    public static AbilityEntry getAbilityClassEntry(Class<? extends Ability> clazz)
    {
        for (AbilityEntry entry : Ability.getAbilityRegistry().getValues())
        {
            if (entry.getAbilityClass() == clazz)
            {
                return entry;
            }
        }
        return null;
    }

    private static IForgeRegistry<AbilityEntry> ABILITY_REGISTRY;
    public static IForgeRegistry<AbilityEntry> getAbilityRegistry()
    {
        return ABILITY_REGISTRY;
    }

    private static IForgeRegistry<AbilityEventEntry> ABILITY_EVENT_REGISTRY;
    public static IForgeRegistry<AbilityEventEntry> getAbilityEventRegistry()
    {
        return ABILITY_EVENT_REGISTRY;
    }

    private static final Map<Class<? extends Ability>, ITextComponent> ABILITY_DISPLAY_NAMES = Maps.newHashMap();
    public static ITextComponent getAbilityDisplayName(Class<? extends Ability> clazz)
    {
        return ABILITY_DISPLAY_NAMES.get(clazz);
    }

    @SuppressWarnings("unchecked")
    @EventBusSubscriber(modid = OrmoyoUtil.MODID)
    private static class CommonEventHandler
    {
        private static final Multimap<ResourceLocation, IAbilityEventListener> listeners = ArrayListMultimap.create();
        private static final Consumer<Event> serverEventAction;


        @SuppressWarnings("unused")
        public static void onInit()
        {
            try
            {
                for (AbilityEntry entry : Ability.getAbilityRegistry())
                {
                    // || Constructor ||
                    Constructor<? extends Ability> constructor = entry.getAbilityClass().getConstructor(IAbilityHolder.class);
                    entry.abilityConstructor = ASMUtils.createLambdaFromConstructor(Function.class, constructor);

                    // || Methods ||
                    Collection<Method> eventMethods = Stream.of(entry.getAbilityClass().getMethods()).filter(method -> method.isAnnotationPresent(SubscribeEvent.class)).collect(Collectors.toSet());

                    for (Method method : eventMethods)
                    {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length != 1)
                        {
                            throw new IllegalArgumentException(
                                    "Ability method " + method + " has @SubscribeEvent annotation, but requires " + parameterTypes.length +
                                            " arguments.  Event handler methods must require a single argument."
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
                                listeners.entries().removeIf(cell -> cell.getValue().getEventClass().isAssignableFrom(eventType) && cell.getValue().getMethod().equals(method));
                                listeners.put(entry.getRegistryName(), new AbilityEventListener(entry, method, eventType, eventEntry.getEventPredicate(), IGenericEvent.class.isAssignableFrom(eventType)));
                            }
                        }
                    }
                }
            }
            catch (ReflectiveOperationException | LambdaConversionException e)
            {
                e.printStackTrace();
            }
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
        {
            IAbilityHolder abilityHolder = (IAbilityHolder) event.getPlayer();
            Collection<AbilityEntry> abilities = abilityHolder.getAbilities()
                    .stream()
                    .map(Ability::getEntry)
                    .collect(Collectors.toSet());

            Collection<AbilityEntry> entries = Ability.getAbilityRegistry().getValues()
                    .stream()
                    .filter(entry -> abilities.contains(entry) || (entry.getLevel() <= 1 &&
                            (entry.getCondition() == null || entry.getConditionCheckingEvents().length == 0)))
                    .collect(Collectors.toSet());

            OrmoyoUtil.NETWORK_CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                    new MessageSetAbilities(event.getPlayer(), entries));
        }

        static
        {
            serverEventAction = event ->
            {
                if (ServerLifecycleHooks.getCurrentServer() == null)
                    return;

                PlayerList list = ServerLifecycleHooks.getCurrentServer().getPlayerList();
                for (ServerPlayerEntity player : list.getPlayers())
                {
                    IAbilityHolder abilityHolder = (IAbilityHolder) player;

                    for (Ability ability : abilityHolder.getAbilities())
                    {
                        if (!ability.isEnabled())
                            continue;

                        Collection<IAbilityEventListener> listeners = CommonEventHandler.listeners.get(ability.getRegistryName());
                        for (IAbilityEventListener listener : listeners)
                        {
                            if (listener.getEventClass() != event.getClass())
                                continue;

                            if (!listener.getEventPredicate().test(ability, event))
                                continue;

                            listener.invoke(ability, event);
                        }
                    }

                    if (!(event instanceof PlayerEvent))
                        continue;

                    for (AbilityEntry entry : Ability.getAbilityRegistry().getValues())
                    {
                        for (Class<? extends Event> clazz : entry.getConditionCheckingEvents())
                        {
                            if (clazz == event.getClass() && entry.getCondition().test(player))
                            {
                                abilityHolder.unlockAbility(entry);
                            }
                        }
                    }
                }
            };
        }
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID, value = Dist.CLIENT)
    static class ClientEventHandler
    {
        @SuppressWarnings("unchecked")
        @SubscribeEvent
        public static void onEvent(Event event)
        {
            if (EffectiveSide.get().isServer())
            {
                CommonEventHandler.serverEventAction.accept(event);
                return;
            }

            if (Minecraft.getInstance().player == null)
                return;

            IAbilityHolder abilityHolder = (IAbilityHolder) Minecraft.getInstance().player;
            Collection<Ability> abilities = abilityHolder.getAbilities();

            for (Ability ability : abilities)
            {
                if (!ability.isEnabled())
                    continue;

                Collection<IAbilityEventListener> listeners = CommonEventHandler.listeners.get(ability.getRegistryName());
                for (IAbilityEventListener listener : listeners)
                {
                    if (listener.getEventClass() != event.getClass())
                        continue;

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
                    if (clazz == event.getClass() && entry.getCondition().test(abilityHolder.getPlayer()))
                    {
                        abilityHolder.unlockAbility(entry);
                    }
                }
            }
        }

        static AbilityKeybindingBase currentConstruct;
        public static void onKeybindBaseConstruct()
        {
            if (currentConstruct == null)
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
            }

            currentConstruct = null;
        }
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID, value = Dist.DEDICATED_SERVER)
    private static class ServerEventHandler
    {
        @SubscribeEvent
        public static void onEvent(Event event)
        {
            CommonEventHandler.serverEventAction.accept(event);
        }
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID, bus = EventBusSubscriber.Bus.MOD)
    private static class RegistryEventHandler
    {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event)
        {
            CommonEventHandler.onInit();
        }

        @SubscribeEvent
        public static void onNewRegistry(RegistryEvent.NewRegistry event)
        {
            ABILITY_REGISTRY = new RegistryBuilder<AbilityEntry>().setName(new ResourceLocation(OrmoyoUtil.MODID, "ability")).setType(AbilityEntry.class).setIDRange(0, 2048).create();
            ABILITY_EVENT_REGISTRY = new RegistryBuilder<AbilityEventEntry>().setName(new ResourceLocation(OrmoyoUtil.MODID, "ability_event")).setType(AbilityEventEntry.class).setIDRange(0, 2048).create();
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
            register(event, PlayerEvent.class, EventPredicates.PLAYER_EVENT);
            register(event, LivingAttackEvent.class, EventPredicates.LIVING_ATTACK_EVENT);
            register(event, LivingDeathEvent.class, EventPredicates.LIVING_DEATH_EVENT);
            register(event, ProjectileImpactEvent.class, EventPredicates.PROJECTILE_IMPACT_EVENT);
            register(event, TickEvent.ClientTickEvent.class, EventPredicates.CLIENT_TICK_EVENT);
            register(event, TickEvent.RenderTickEvent.class, EventPredicates.RENDER_TICK_EVENT);
            register(event, RenderGameOverlayEvent.class, EventPredicates.RENDER_GAME_OVERLAY_EVENT);
            register(event, RenderLivingEvent.class, EventPredicates.RENDER_LIVING_EVENT);
            register(event, EntityViewRenderEvent.class, EventPredicates.ENTITY_VIEW_RENDER_EVENT);
            register(event, InputEvent.class, EventPredicates.INPUT_EVENT);
            register(event, RenderHandEvent.class, EventPredicates.RENDER_HAND_EVENT);
            register(event, RenderArmEvent.class, EventPredicates.RENDER_ARM_EVENT);
            register(event, ClientPlayerNetworkEvent.class, EventPredicates.CLIENT_PLAYER_NETWORK_EVENT);
            register(event, GuiOpenEvent.class, EventPredicates.GUI_OPEN_EVENT);
        }

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void registerAbilityEventPredicatesOnClient(RegistryEvent.Register<AbilityEventEntry> event)
        {
            register(event, GuiScreenEvent.class, (ability, player) -> true);
        }

        private static <T extends Event> void register(RegistryEvent.Register<AbilityEventEntry> event, Class<T> clazz, IAbilityEventPredicate<T> predicate)
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
            public static final IAbilityEventPredicate<EntityEvent>
                    ENTITY_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getEntity());

            public static final IAbilityEventPredicate<LivingAttackEvent>
                    LIVING_ATTACK_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getEntityLiving()) ||
                                    ability.owner.equals(event.getSource().getTrueSource());

            public static final IAbilityEventPredicate<LivingDeathEvent>
                    LIVING_DEATH_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getEntityLiving()) ||
                                    ability.owner.equals(event.getSource().getTrueSource());

            public static final IAbilityEventPredicate<ProjectileImpactEvent>
                    PROJECTILE_IMPACT_EVENT =
                    (ability, event) ->
                            ability.owner.equals(((ProjectileEntity) event.getEntity()).getShooter()) ||
                                    ability.owner.equals(event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY ?
                                            ((EntityRayTraceResult) event.getRayTraceResult()).getEntity() : null);

            public static final IAbilityEventPredicate<PlayerEvent>
                    PLAYER_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getPlayer());

            public static final IAbilityEventPredicate<TickEvent.ClientTickEvent>
                    CLIENT_TICK_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<TickEvent.RenderTickEvent>
                    RENDER_TICK_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<RenderGameOverlayEvent>
                    RENDER_GAME_OVERLAY_EVENT =
                    (ability, event) ->
                            true;

            @SuppressWarnings("rawtypes")
            public static final IAbilityEventPredicate<RenderLivingEvent>
                    RENDER_LIVING_EVENT =
                    (ability, event) ->
                            !ability.owner.equals(event.getEntity());

            public static final IAbilityEventPredicate<EntityViewRenderEvent>
                    ENTITY_VIEW_RENDER_EVENT =
                    (ability, event) ->
                            true;
            public static final IAbilityEventPredicate<InputEvent>
                    INPUT_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<RenderHandEvent>
                    RENDER_HAND_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<RenderArmEvent>
                    RENDER_ARM_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<ClientPlayerNetworkEvent>
                    CLIENT_PLAYER_NETWORK_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<GuiOpenEvent>
                    GUI_OPEN_EVENT =
                    (ability, event) ->
                            true;
        }
    }
}