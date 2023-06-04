package com.ormoyo.ormoyoutil.util;

import java.util.Random;

public class MathUtils
{
    private static final Random RANDOM = new Random();

    public static int lerp(int min, int max, float t)
    {
        return round(min + (max - min) * t);
    }

    public static float lerp(float min, float max, float t)
    {
        return min + (max - min) * t;
    }

    public static double lerp(double min, double max, double t)
    {
        return min + (max - min) * t;
    }

    public static int round(float num)
    {
        return (int) (num + 0.5f);
    }

    public static int round(double num)
    {
        return (int) (num + 0.5);
    }

    public static int randomInt(int min, int max)
    {
        return lerp(min, max, RANDOM.nextFloat());
    }

    public static float randomFloat(float min, float max)
    {
        return lerp(min, max, RANDOM.nextFloat());
    }

    public static double randomDouble(double min, double max)
    {
        return lerp(min, max, RANDOM.nextDouble());
    }

    public static int randomInt(Random random, int min, int max)
    {
        return lerp(min, max, random.nextFloat());
    }

    public static float randomFloat(Random random, float min, float max)
    {
        return lerp(min, max, random.nextFloat());
    }

    public static double randomDouble(Random random, double min, double max)
    {
        return lerp(min, max, random.nextDouble());
    }
}
