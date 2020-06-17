package com.unhuman.dataBuilder.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unhuman.dataBuilder.input.PromptHelper;

import java.util.Random;

import static com.unhuman.dataBuilder.input.PromptHelper.error;

public class IntegerDescriptor extends DataItemDescriptor {
    private Random random = new Random();

    @JsonProperty
    private int minValue;

    @JsonProperty
    private int maxValue;

    public IntegerDescriptor(String name) {
        super(name);
    }

    private IntegerDescriptor() {
        // For Jackson
        super();
    }

    @Override
    public void obtainConfiguration() {
        while (true) {
            minValue = PromptHelper.promptIntegerValue("Minimum Value");
            maxValue = PromptHelper.promptIntegerValue("Maximum Value");
            if (minValue <= maxValue) {
                return;
            }
            error("Minimum Value (%d) must be <= Maximum Value (%d)\n", minValue, maxValue);
        }
    }

    @Override
    public String getNextValue(NullHandler nullHandler) {
        int nextValue = minValue + random.nextInt(maxValue - minValue);
        return Integer.toString(nextValue);
    }
}
