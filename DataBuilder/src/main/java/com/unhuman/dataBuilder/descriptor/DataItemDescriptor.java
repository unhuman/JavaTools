package com.unhuman.dataBuilder.descriptor;

import java.util.Random;
import java.util.regex.Matcher;

public abstract class DataItemDescriptor {
    public enum NullHandler { AS_NULL, EMPTY }

    private String name;
    private Matcher currentMatcherState;
    private Long randomSeed;

    public DataItemDescriptor(String name) {
        this.name = name;
    }

    protected Long getRandomSeed() {
        return randomSeed;
    }

    public int getNextRandom(int maxExclusive) {
        Random random = (randomSeed != null)
                ? new Random(randomSeed + this.getClass().getSimpleName().hashCode())
                : new Random();
        return random.nextInt(maxExclusive);
    }

    public String getName() {
        return name;
    }

    public abstract void obtainConfiguration();

    public abstract String getNextValue(NullHandler nullHandler);

    public DataItemDescriptor setIterationState(Matcher matcher, Long randomSeed) {
        this.currentMatcherState = matcher;
        this.randomSeed = randomSeed;
        return this;
    }

    protected Matcher getCurrentMatcherState() {
        return currentMatcherState;
    }
}
