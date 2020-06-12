package com.unhuman.dataBuilder.descriptor;

import java.util.regex.Matcher;

public abstract class DataItemDescriptor {
    private String name;
    private Matcher currentMatcherState;

    public DataItemDescriptor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void obtainConfiguration();

    public abstract String getNextValue();

    public void setMatchedContent(Matcher matcher) {
        this.currentMatcherState = matcher;
    }

    protected Matcher getCurrentMatcherState() {
        return currentMatcherState;
    }
}
