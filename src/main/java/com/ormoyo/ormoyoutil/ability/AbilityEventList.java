package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.ability.event.AbilityEventListener;
import com.ormoyo.ormoyoutil.ability.event.AbilityEventPredicate;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("rawtypes")
class AbilityEventList
{
    private boolean rebuild = true;
    private final AtomicReference<Map<AbilityEntry, Collection<AbilityEventListener>>> listeners = new AtomicReference<>();
    private final List<List<AbilityEventListener>> priorities;
    private AbilityEventList parent;
    private List<AbilityEventList> children;
    private Semaphore writeLock = new Semaphore(1, true);


    AbilityEventList()
    {
        int count = EventPriority.values().length;
        priorities = new ArrayList<>(count);

        for (int x = 0; x < count; x++)
            priorities.add(new ArrayList<>());
    }

    AbilityEventList(AbilityEventList parent)
    {
        this();

        this.parent = parent;
        this.parent.addChild(this);
    }

    private void addChild(AbilityEventList child)
    {
        if (this.children == null)
            this.children = Collections.synchronizedList(new ArrayList<>(2));
        this.children.add(child);
    }

    protected boolean shouldRebuild()
    {
        return rebuild;// || (parent != null && parent.shouldRebuild());
    }

    protected void forceRebuild()
    {
        this.rebuild = true;
        if (this.children != null) {
            synchronized (this.children) {
                for (AbilityEventList child : this.children)
                    child.forceRebuild();
            }
        }
    }

    public Collection<AbilityEventListener> getListeners(AbilityEntry ability)
    {
        if (this.shouldRebuild())
            this.buildCache();

        Collection<AbilityEventListener> listeners = this.listeners.get().get(ability);
        return listeners == null ? Collections.emptyList() : listeners;
    }

    public ArrayList<AbilityEventListener> getListeners(EventPriority priority)
    {
        writeLock.acquireUninterruptibly();
        ArrayList<AbilityEventListener> ret = new ArrayList<>(priorities.get(priority.ordinal()));
        writeLock.release();

        if (parent != null)
            ret.addAll(parent.getListeners(priority));

        return ret;
    }

    private void buildCache()
    {
        if (parent != null && parent.shouldRebuild())
            parent.buildCache();

        int keyCount = Math.max(Ability.getAbilityRegistry().getValues().size(), 8);
        Map<AbilityEntry, Collection<AbilityEventListener>> ret = new HashMap<>(keyCount);

        for (EventPriority priority : EventPriority.values())
        {
            List<AbilityEventListener> listeners = this.getListeners(priority);
            for (AbilityEventListener listener : listeners)
            {
                Collection<AbilityEventListener> collection = ret.computeIfAbsent(listener.getAbilityEntry(), k -> new ArrayList<>());
                collection.add(listener);
            }
        }

        listeners.set(ret);
        rebuild = false;
    }

    public void register(EventPriority priority, AbilityEventListener listener)
    {
        writeLock.acquireUninterruptibly();
        priorities.get(priority.ordinal()).add(listener);
        writeLock.release();

        this.forceRebuild();
    }

    public void unregister(AbilityEventListener listener)
    {
        writeLock.acquireUninterruptibly();

        for (List<AbilityEventListener> listeners : priorities)
            if (listeners.remove(listener))
                this.forceRebuild();

        writeLock.release();
    }
    
    public static class AbilityEventPriority implements AbilityEventListener<Event>
    {
        private final EventPriority priority;

        private AbilityEventPriority(EventPriority priority)
        {
            this.priority = priority;
        }


        @Override
        public void invoke(Ability ability, Event event)
        {
            int phase = event.getPhase() == null ? -1 : event.getPhase().ordinal();
            if (this.priority.ordinal() <= phase)
                return;

            event.setPhase(this.priority);
        }

        @Override
        public AbilityEventPredicate<Event> getEventPredicate()
        {
            return (ability, event) -> true;
        }

        @Override
        public Class<Event> getEventClass()
        {
            return null;
        }

        @Override
        public AbilityEntry getAbilityEntry()
        {
            return null;
        }

        @Override
        public Method getMethod()
        {
            return null;
        }
    }
}
