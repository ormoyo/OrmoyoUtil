package com.ormoyo.ormoyoutil.network.datasync;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Basically {@link EntityDataManager} but for abilities
 */
public class AbilitySyncManager
{
    private static final Map<Class<? extends Ability>, Integer> dataValuesCount = Maps.newHashMap();
    private final Map<Integer, AbilityDataEntry<?>> entries = Maps.newHashMap();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Ability ability;
    private boolean dirty;

    public AbilitySyncManager(Ability ability)
    {
        this.ability = ability;
        if (ability.getOwner() == null) return;
        EventHandler.managers.add(this);
    }

    public static <T> AbilityDataParameter<T> createKey(Class<? extends Ability> clazz, IDataSerializer<T> serializer)
    {
        int count;
        if (dataValuesCount.containsKey(clazz))
        {
            count = dataValuesCount.get(clazz) + 1;
        }
        else
        {
            int i = 0;
            Class<?> superClazz = clazz;

            while (superClazz != Ability.class)
            {
                superClazz = superClazz.getSuperclass();
                if (dataValuesCount.containsKey(superClazz))
                {
                    i = dataValuesCount.get(superClazz) + 1;
                    break;
                }
            }

            count = i;
        }

        if (count > 254)
            throw new IllegalArgumentException("Ability data value id is too big with " + count + "! (Max is " + 254 + ")");

        dataValuesCount.put(clazz, count);
        return convertParameter(serializer.createKey(count));
    }

    public <T> void register(AbilityDataParameter<T> key, T value)
    {
        int i = key.getId();

        if (i > 254)
            throw new IllegalArgumentException("Ability data value id is too big with " + i + "! (Max is " + 254 + ")");
        if (this.entries.containsKey(i))
            throw new IllegalArgumentException("Duplicate id value for " + i + "!");
        if (DataSerializers.getSerializerId(key.getSerializer()) < 0)
            throw new IllegalArgumentException("Unregistered serializer " + key.getSerializer() + " for " + i + "!");

        this.setEntry(key, value);
    }

    private <T> void setEntry(AbilityDataParameter<T> key, T value)
    {
        AbilityDataEntry<T> dataentry = new AbilityDataEntry<T>(key, value);
        this.lock.writeLock().lock();
        this.entries.put(key.getId(), dataentry);
        this.lock.writeLock().unlock();
    }

    @SuppressWarnings("unchecked")
    private <T> AbilityDataEntry<T> getEntry(AbilityDataParameter<T> key)
    {
        this.lock.readLock().lock();
        AbilityDataEntry<T> dataentry;
        try
        {
            dataentry = (AbilityDataEntry<T>) this.entries.get(key.getId());
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting synched ability data");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Synched ability data");

            crashreportcategory.addDetail("Data ID", key);

            throw new ReportedException(crashreport);
        }
        this.lock.readLock().unlock();
        return dataentry;
    }

    public <T> void set(AbilityDataParameter<T> key, T value)
    {
        AbilityDataEntry<T> dataentry = this.getEntry(key);
        if (ObjectUtils.notEqual(value, dataentry.getValue()))
        {
            dataentry.setValue(value);
            dataentry.setDirty(true);

            this.ability.notifySyncManagerChange(key);
            this.dirty = true;
        }
    }

    public <T> T get(AbilityDataParameter<T> key)
    {
        return this.getEntry(key).getValue();
    }

    public <T> void setDirty(AbilityDataParameter<T> key)
    {
        this.getEntry(key).setDirty(true);
        this.dirty = true;
    }

    public boolean isDirty()
    {
        return this.dirty;
    }

    void setEntryValues(List<AbilityDataEntry<?>> entriesIn)
    {
        this.lock.writeLock().lock();

        for (AbilityDataEntry<?> dataEntry : entriesIn)
        {
            AbilityDataEntry<?> otherDataEntry = this.entries.get(dataEntry.getKey().getId());
            if (otherDataEntry != null)
            {
                this.setEntryValue(otherDataEntry, dataEntry);
                this.ability.notifySyncManagerChange(dataEntry.getKey());
            }
        }
        this.lock.writeLock().unlock();
        this.dirty = false;
    }

    @SuppressWarnings("unchecked")
    private <T> void setEntryValue(AbilityDataEntry<T> target, AbilityDataEntry<?> source)
    {
        target.setValue((T) source.getValue());
    }

    static <T> AbilityDataParameter<T> convertParameter(DataParameter<T> parameter)
    {
        return new AbilityDataParameter<T>(parameter.getId(), parameter.getSerializer());
    }

    @EventBusSubscriber(modid = OrmoyoUtil.MODID)
    private static class EventHandler
    {
        private static final List<AbilitySyncManager> managers = Lists.newArrayList();

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event)
        {
            if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) return;
            for (AbilitySyncManager manager : managers)
            {
                if (manager.dirty && !manager.ability.getOwner().getEntityWorld().isRemote && manager.ability.getOwner().equals(event.player))
                {
                    List<AbilityDataEntry<?>> list = null;
                    manager.lock.readLock().lock();

                    for (AbilityDataEntry<?> value : manager.entries.values())
                    {
                        if (value.isDirty())
                        {
                            if (list == null)
                                list = Lists.newArrayList();

                            value.setDirty(false);
                            list.add(value.copy());
                        }
                    }

                    if (list != null)
                        OrmoyoUtil.NETWORK_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) manager.ability.getOwner()), new MessageUpdateDataParameters(manager.ability.getEntry(), list));

                    manager.lock.readLock().unlock();
                    manager.dirty = false;
                }
            }
        }
    }

    static class AbilityDataEntry<T>
    {
        private final AbilityDataParameter<T> key;
        private T value;
        private boolean dirty;

        public AbilityDataEntry(AbilityDataParameter<T> key, T value)
        {
            this.key = key;
            this.value = value;
            this.dirty = true;
        }

        public AbilityDataParameter<T> getKey()
        {
            return this.key;
        }

        public void setValue(T valueIn)
        {
            this.value = valueIn;
        }

        public T getValue()
        {
            return this.value;
        }

        public boolean isDirty()
        {
            return this.dirty;
        }

        public void setDirty(boolean dirtyIn)
        {
            this.dirty = dirtyIn;
        }

        public AbilityDataEntry<T> copy()
        {
            return new AbilityDataEntry<T>(this.key, this.key.getSerializer().copyValue(this.value));
        }
    }
}
