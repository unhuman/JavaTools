package com.unhuman.dataBuilder.descriptor;

import com.unhuman.dataBuilder.input.PromptHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static com.unhuman.dataBuilder.input.PromptHelper.error;

public class EnumValuesDescriptor extends DataItemDescriptor {
    private Random random = new Random();
    private Map<String, Integer> dataProbability = new LinkedHashMap<>();

    public EnumValuesDescriptor(String name) {
        super(name);
    }

    @Override
    public void obtainConfiguration() {
        int totalPercentage = 0;
        while (totalPercentage != 100) {
            String item = PromptHelper.promptForValue("enum item (empty to stop / null remainder)", "");
            if (item.isBlank()) {
                break;
            }
            int percentage = PromptHelper.promptIntegerValue("percentage for '" + item + "'");
            if (percentage <= 0 || percentage > 100) {
                error("Percentage (%d) invalid\n", percentage);
                continue;
            }
            totalPercentage += percentage;
            if (totalPercentage > 100) {
                error("Total Percentage (%d) > 100\n", totalPercentage);
                error("Ignoring value %s\n", getName());
                totalPercentage = 0;
                dataProbability.clear();
                continue;
            }
            dataProbability.put(item, totalPercentage);
        }
    }

    @Override
    public String getNextValue(NullHandler nullHandler) {
        int itemRandom = random.nextInt(100);
        for (String value: dataProbability.keySet()) {
            if (itemRandom < dataProbability.get(value)) {
                return '"' + value + '"';
            }
        }
        return NullHandler.AS_NULL.equals(nullHandler) ? null : "";
    }
}
