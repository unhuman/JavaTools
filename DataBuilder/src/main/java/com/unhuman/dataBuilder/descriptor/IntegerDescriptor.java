package com.unhuman.dataBuilder.descriptor;

import com.unhuman.dataBuilder.input.PromptHelper;

import java.io.Console;
import java.util.Random;

import static com.unhuman.dataBuilder.input.PromptHelper.error;

public class IntegerDescriptor extends DataItemDescriptor {
    private Random random = new Random();

    private int minValue;
    private int maxValue;

    public IntegerDescriptor(String name) {
        super(name);
    }

    @Override
    public void obtainConfiguration() {
        Console console = System.console();

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
    public String getNextValue() {
        int nextValue = minValue + random.nextInt(maxValue - minValue);
        return Integer.toString(nextValue);
    }
}