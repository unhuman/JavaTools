package com.unhuman.dataBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unhuman.dataBuilder.descriptor.DataItemDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class SettingsConfig {
    public LinkedHashSet<String> replacementTokens;
    private String regex;
    private List<DataItemDescriptor> settings;

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

    public List<DataItemDescriptor> getSettings() {
        return settings;
    }

    public void addSetting(DataItemDescriptor setting) {
        this.settings.add(setting);
    }

    public DataItemDescriptor getSetting(String name) {
        return settings.stream().filter(item -> name.equals(item.getName())).findFirst().get();
    }

    public void setSettings(List<DataItemDescriptor> settings) {
        this.settings = settings;
    }
}
