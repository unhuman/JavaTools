package com.unhuman.dataBuilder.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unhuman.dataBuilder.input.PromptHelper;

import java.util.Random;

public class BooleanDescriptor extends DataItemDescriptor {
    private Random random = new Random();

    @JsonProperty
    private int percentTrue = 50;

    public BooleanDescriptor(String name) {
        super(name);
    }

    private BooleanDescriptor() {
        // For Jackson
        super();
    }

    @Override
    public void obtainConfiguration() {
        percentTrue = PromptHelper.promptPercentage(getName() + "=true", 50);
    }

    @JsonIgnore
    @Override
    public String getNextValue(NullHandler nullHandler) {
        int checkPercentage = random.nextInt(100);
        return Boolean.toString(checkPercentage < percentTrue);
    }
}
