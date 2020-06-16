package com.unhuman.dataBuilder;

import com.unhuman.dataBuilder.descriptor.BooleanDescriptor;
import com.unhuman.dataBuilder.descriptor.DataItemDescriptor;
import com.unhuman.dataBuilder.descriptor.EmailDescriptor;
import com.unhuman.dataBuilder.descriptor.EmptyDescriptor;
import com.unhuman.dataBuilder.descriptor.EnumValuesDescriptor;
import com.unhuman.dataBuilder.descriptor.FileContentDescriptor;
import com.unhuman.dataBuilder.descriptor.FirstNameDescriptor;
import com.unhuman.dataBuilder.descriptor.IdDescriptor;
import com.unhuman.dataBuilder.descriptor.IntegerDescriptor;
import com.unhuman.dataBuilder.descriptor.LastNameDescriptor;
import com.unhuman.dataBuilder.input.PromptHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.unhuman.dataBuilder.input.PromptHelper.error;
import static com.unhuman.dataBuilder.input.PromptHelper.output;

public class DataBuilder {
    private enum FileTypes { INPUT, OUTPUT }
    private enum InputTypes { ID, BOOLEAN, INTEGER,
        EMAIL, EMPTY_STRING, ENUM_VALUES, FILE_CONTENT, FIRST_NAME, LAST_NAME }
    private enum SerializationTypes { CSV, JSON }

    protected void process() {
        ArrayList<DataItemDescriptor> items = new ArrayList<>();

        List<Enum> availableInputTypes = new ArrayList<>();
        for (InputTypes inputType: InputTypes.values()) {
            availableInputTypes.add(inputType);
        }

        String inputContent = null;
        while (true) {
            try {
                File inputFile = getFile(FileTypes.INPUT);
                inputContent = Files.readString(inputFile.toPath());
                break;
            } catch (Exception e) {
                error("Problem reading in file: %s\n", inputContent);
            }
        }

        String matchRegex = null;
        while (true) {
            try {
                matchRegex = PromptHelper.promptForValue("matching regex");
                Pattern pattern = Pattern.compile(matchRegex);
                Matcher matcher = pattern.matcher(inputContent);
                if (matcher.find()) {
                    break;
                }
                error("No matches for regex: %s in file\n", matchRegex);
            } catch (Exception e) {
                error("Invalid regex: %s in file\n", matchRegex);
            }
        }

        File outputFile = getFile(FileTypes.OUTPUT);

        while (true) {
            output("\n-- New Field --\n");
            String name = PromptHelper.promptForValue("field name (empty to stop)", "");

            // Empty name = we're done
            if (name.isBlank()) {
                break;
            }

            // Ensure name doesn't already exist
            if (items.stream().anyMatch(item -> item.getName().equals(name))) {
                error("Name: %s already exists\n", name);
                continue;
            }

            InputTypes[] displayInputTypes = new InputTypes[availableInputTypes.size()];
            String selectedType =
                    PromptHelper.promptForEnumValue("data type for " + name,
                            PromptHelper.StartingIndex.ONE,
                            availableInputTypes.toArray(displayInputTypes));

            DataItemDescriptor descriptor = null;
            switch (InputTypes.valueOf(selectedType)) {
                case ID:
                    // only permit one id
                    availableInputTypes.remove(InputTypes.ID);
                    descriptor = new IdDescriptor(name);
                    break;
                case BOOLEAN:
                    descriptor = new BooleanDescriptor(name);
                    break;
                case INTEGER:
                    descriptor = new IntegerDescriptor(name);
                    break;
                case EMAIL:
                    descriptor = new EmailDescriptor(name);
                    break;
                case EMPTY_STRING:
                    descriptor = new EmptyDescriptor(name);
                    break;
                case ENUM_VALUES:
                    descriptor = new EnumValuesDescriptor(name);
                    break;
                case FILE_CONTENT:
                    descriptor = new FileContentDescriptor(name);
                    break;
                case FIRST_NAME:
                    descriptor = new FirstNameDescriptor(name);
                    break;
                case LAST_NAME:
                    descriptor = new LastNameDescriptor(name);
                    break;
                default:
                    // not expected
                    throw new RuntimeException("Invalid type: " + InputTypes.valueOf(selectedType));
            }
            descriptor.obtainConfiguration();
            items.add(descriptor);
        }

        output("\n-- Serialization --\n");

        SerializationTypes serializationType;
        if (outputFile.toString().endsWith(".csv")) {
            serializationType = SerializationTypes.JSON;
        } else if (outputFile.toString().endsWith(".json")) {
            serializationType = SerializationTypes.CSV;
        } else {
            serializationType = SerializationTypes.valueOf(PromptHelper.promptForEnumValue(
                    "serialization desired", PromptHelper.StartingIndex.ONE, SerializationTypes.values()));
        }

        String serializedContent;
        switch (serializationType) {
            case CSV:
                serializedContent = serializeCsv(items, inputContent, matchRegex);
                break;
            case JSON:
                boolean serializeNullValues = PromptHelper.promptYesNo("Do you want to serialize null values?");
                serializedContent = serializeJson(items, inputContent, matchRegex, serializeNullValues);
                break;
            default:
                throw new RuntimeException("Invalid serialization: " + serializationType);
        }

        try {
            Files.writeString(outputFile.toPath(), serializedContent);
            output("File %s successfully written\n ", outputFile.getPath());
        } catch (IOException ioException) {
            error("Problem writing output file %s\n", outputFile.getPath());
        }
    }

    private String serializeJson(ArrayList<DataItemDescriptor> items, String inputContent, String matchRegex,
                                 boolean serializeNullValues) {
        StringBuilder builder = new StringBuilder(2048);
        Pattern pattern = Pattern.compile(matchRegex);
        Matcher matcher = pattern.matcher(inputContent);
        builder.append("[");
        boolean firstMatch = true;
        Random random = new Random();
        while (matcher.find()) {
            // Create a seed for each round
            long randomSeed = random.nextLong();

            if (!firstMatch) {
                builder.append(",");
            }
            firstMatch = false;
            builder.append("\n{");

            // process all the descriptors
            boolean firstDescriptor = true;
            for (DataItemDescriptor descriptor: items) {
                descriptor.setIterationState(matcher, randomSeed);
                String value = descriptor.getNextValue(DataItemDescriptor.NullHandler.AS_NULL);
                if (value != null || serializeNullValues) {
                    if (!firstDescriptor) {
                        builder.append(",");
                    }
                    firstDescriptor = false;
                    builder.append('"').append(descriptor.getName()).append('"').append(":");
                    builder.append(value);
                }
            }
            builder.append("}");
        }
        builder.append("\n]");
        return builder.toString();
    }

    private String serializeCsv(ArrayList<DataItemDescriptor> items, String inputContent, String matchRegex) {
        StringBuilder builder = new StringBuilder(2048);
        Pattern pattern = Pattern.compile(matchRegex);
        Matcher matcher = pattern.matcher(inputContent);

        builder.append(items.stream().map(item -> item.getName()).collect(Collectors.joining(",")));

        Random random = new Random();
        while (matcher.find()) {
            // Create a seed for each round
            long randomSeed = random.nextLong();

            builder.append("\n");
            builder.append(items.stream().map(item ->
                    item.setIterationState(matcher, randomSeed)
                            .getNextValue(DataItemDescriptor.NullHandler.EMPTY))
                    .collect(Collectors.joining(",")));
        }
        return builder.toString();
    }

    private File getFile(FileTypes type) {
        String filename = null;
        while (true) {
            try {
                filename = PromptHelper.promptForValue(type.name().toLowerCase() + " filename");
                File file = new File(filename);
                if ((FileTypes.INPUT.equals(type) && file.exists())
                    || FileTypes.OUTPUT.equals(type) && !file.exists()) {
                    return file;
                }
            } catch (Exception e) {
                // Do nothing
            }
            error("Problem opening %s file: %s\n", type.name().toLowerCase(), filename);
        }
    }

    public static void main(String[] args) {
        DataBuilder dataBuilder = new DataBuilder();
        dataBuilder.process();
    }
}
