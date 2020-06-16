package com.unhuman.dataBuilder.descriptor;

import java.util.regex.Matcher;

public abstract class DataItemDescriptor {
    public enum NullHandler { AS_NULL, EMPTY }

    private String name;
    private Matcher currentMatcherState;

    public DataItemDescriptor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void obtainConfiguration();

    public abstract String getNextValue(NullHandler nullHandler);

    public DataItemDescriptor setMatcher(Matcher matcher) {
        this.currentMatcherState = matcher;
        return this;
    }

    protected Matcher getCurrentMatcherState() {
        return currentMatcherState;
    }
}
