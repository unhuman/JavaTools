package com.unhuman.dataBuilder;

import com.unhuman.dataBuilder.descriptor.DataItemDescriptor;

import java.util.ArrayList;
import java.util.List;

public class SettingsConfig {
    private String regex;
    private List<DataItemDescriptor> settings;

    public SettingsConfig() {
        regex = null;
        settings = new ArrayList<>();
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

    public void setSettings(List<DataItemDescriptor> settings) {
        this.settings = settings;
    }
}
