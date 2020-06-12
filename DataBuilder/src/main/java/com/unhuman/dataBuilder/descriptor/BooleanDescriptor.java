package com.unhuman.dataBuilder.descriptor;

import com.unhuman.dataBuilder.input.PromptHelper;

import java.util.Random;

public class BooleanDescriptor extends DataItemDescriptor {
    private Random random = new Random();
    private int percentTrue = 50;

    public BooleanDescriptor(String name) {
        super(name);
    }

    @Override
    public void obtainConfiguration() {
        percentTrue = PromptHelper.promptPercentage(getName() + "=true", 50);
    }

    @Override
    public String getNextValue() {
        int checkPercentage = random.nextInt(100);
        return Boolean.toString(checkPercentage < percentTrue);
    }
}
