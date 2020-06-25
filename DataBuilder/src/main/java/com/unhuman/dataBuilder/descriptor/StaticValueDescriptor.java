package com.unhuman.dataBuilder.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unhuman.dataBuilder.input.PromptHelper;

public class StaticValueDescriptor extends AbstractEntityTypeDescriptor {
    @JsonProperty
    private String value;

    @JsonProperty
    private Boolean valueIsString;

    public StaticValueDescriptor(String name) {
        super(name);
    }

    // For Jackson
    private StaticValueDescriptor() {
        super();
    }

    @Override
    public void obtainConfiguration() {
        value = PromptHelper.promptForValue("static value");
        String checkYesNo = PromptHelper.promptFilteredValue(
                getName() + " is String value? (Y/n)", "^[YyNn]$", "Y");
        valueIsString = checkYesNo.toLowerCase().equals("y");
    }

    @Override
    public String getNextValue(NullHandler nullHandler) {
        return (valueIsString ? '"' : "") + value + (valueIsString ? '"' : "");
    }
}
