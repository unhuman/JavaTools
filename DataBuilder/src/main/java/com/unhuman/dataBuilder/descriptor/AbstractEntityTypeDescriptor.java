package com.unhuman.dataBuilder.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Random;
import java.util.regex.Matcher;

public abstract class AbstractEntityTypeDescriptor {
    public enum NullHandler { AS_NULL, EMPTY }

    private Random random = new Random();

    @JsonProperty
    private String name;

    private Matcher currentMatcherState;

    public AbstractEntityTypeDescriptor(String name) {
        this.name = name;
    }

    // For Jackson
    protected AbstractEntityTypeDescriptor() {
    }


    public int getNextRandom(int maxExclusive) {
        return random.nextInt(maxExclusive);
    }

    public String getName() {
        return name;
    }

    public abstract void obtainConfiguration();

    public abstract String getNextValue(NullHandler nullHandler);

    public AbstractEntityTypeDescriptor setIterationState(Matcher matcher) {
        this.currentMatcherState = matcher;
        return this;
    }

    protected Matcher getCurrentMatcherState() {
        return currentMatcherState;
    }
}
