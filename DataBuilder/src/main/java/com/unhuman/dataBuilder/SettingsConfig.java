package com.unhuman.dataBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unhuman.dataBuilder.descriptor.AbstractEntityTypeDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class SettingsConfig {
    public LinkedHashSet<String> replacementTokens;
    private String regex;
    private List<AbstractEntityTypeDescriptor> settings;

    public SettingsConfig() {
        replacementTokens = null;
        regex = null;
        settings = new ArrayList<>();
    }

    public LinkedHashSet<String> getReplacementTokens() {
        return replacementTokens;
    }

    public void setReplacementTokens(LinkedHashSet<String> replacementTokens) {
        this.replacementTokens = replacementTokens;
    }

    @JsonIgnore
    public boolean isTokenBased() {
        return replacementTokens != null;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public List<AbstractEntityTypeDescriptor> getSettings() {
        return settings;
    }

    public void addSetting(AbstractEntityTypeDescriptor setting) {
        this.settings.add(setting);
    }

    public AbstractEntityTypeDescriptor getSetting(String name) {
        return settings.stream().filter(item -> name.equals(item.getName())).findFirst().get();
    }

    public void setSettings(List<AbstractEntityTypeDescriptor> settings) {
        this.settings = settings;
    }
}
