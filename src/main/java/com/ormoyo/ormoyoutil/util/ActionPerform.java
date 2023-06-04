package com.ormoyo.ormoyoutil.util;

//This will perform a consumer after amount of ticks
public class ActionPerform implements ITick
{
    private final Action action;

    private final int maxTick;
    private int tick;

    private boolean performAtEnd;
    protected boolean remove;

    public ActionPerform(Action action, int tickLength)
    {
        this(action, tickLength, true);
    }

    public ActionPerform(Action action, int tickLength, boolean performAtEnd)
    {
        if (tickLength == 0)
        {
            throw new IllegalArgumentException("tick amount cannot be 0");
        }

        this.action = action;
        this.maxTick = tickLength;

    }

    @Override
    public void update()
    {
        this.tick++;

        if (this.maxTick > 0)
        {
            this.tick %= this.maxTick;
        }

        if (this.tick == 0)
        {
            this.remove = true;
            this.action.execute();

            return;
        }

        if (!this.performAtEnd)
        {
            this.action.execute();
        }
    }

    @Override
    public boolean remove()
    {
        return this.remove;
    }
}