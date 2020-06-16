package com.unhuman.dataBuilder.descriptor;

import com.unhuman.dataBuilder.input.PromptHelper;

import java.util.UUID;

public class IdDescriptor extends DataItemDescriptor {
    private enum IdType { INCREMENTING, GUID };

    private IdType idType;

    private int incrementingCurrentId = 0;
    private boolean incrementingCurrentIdIsString = false;

    public IdDescriptor(String name) {
        super(name);
    }

    public void obtainConfiguration() {
        String checkIdType =
                PromptHelper.promptForEnumValue("data type for id", PromptHelper.StartingIndex.ONE,
                        IdType.values());

        idType = IdType.valueOf(checkIdType);

        if (IdType.INCREMENTING.equals(idType)) {
            while (true) {
                try {
                    String checkStartingId = PromptHelper.promptForValue("Starting Id", "1");
                    incrementingCurrentId = Integer.parseInt(checkStartingId);
                    break;
                } catch (Exception e) {
                    // keep trying
                }
            }

            incrementingCurrentIdIsString = PromptHelper.promptYesNo(getName() + " is String value?");
        }
    }

    public String getNextValue(NullHandler nullHandler) {
        switch (idType) {
            case INCREMENTING:
                return incrementingCurrentIdIsString
                        ? '"' + String.valueOf(incrementingCurrentId++) + '"'
                        : String.valueOf(incrementingCurrentId++);
            case GUID:
                return '"' + UUID.randomUUID().toString() + '"';
            default:
                throw new RuntimeException("Unknown idType: " + idType.name());
        }
    }
}
