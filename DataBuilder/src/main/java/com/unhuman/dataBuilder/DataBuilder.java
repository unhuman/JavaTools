package com.unhuman.dataBuilder;

import com.unhuman.dataBuilder.descriptor.BooleanDescriptor;
import com.unhuman.dataBuilder.descriptor.DataItemDescriptor;
import com.unhuman.dataBuilder.descriptor.EnumValuesDescriptor;
import com.unhuman.dataBuilder.descriptor.FileContentDescriptor;
import com.unhuman.dataBuilder.descriptor.IdDescriptor;
import com.unhuman.dataBuilder.descriptor.IntegerDescriptor;
import com.unhuman.dataBuilder.input.PromptHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.unhuman.dataBuilder.input.PromptHelper.error;
import static com.unhuman.dataBuilder.input.PromptHelper.output;

public class DataBuilder {
    private enum FileTypes { INPUT, OUTPUT }
    private enum InputTypes { ID, FILE_CONTENT, ENUM_VALUES, BOOLEAN, INTEGER }

    protected void process() {
        ArrayList<DataItemDescriptor> items = new ArrayList<>();

        List<String> dataKinds = new ArrayList<>();
        for (InputTypes inputType: InputTypes.values()) {
            dataKinds.add(inputType.name());
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

            String selectedType =
                    PromptHelper.promptForEnumValue("data type for " + name,
                            dataKinds.toArray(new String[dataKinds.size()]));

            DataItemDescriptor descriptor = null;
            switch (InputTypes.valueOf(selectedType)) {
                case ID:
                    // only permit one id
                    dataKinds.remove(InputTypes.ID.name());
                    descriptor = new IdDescriptor(name);
                    break;
                case FILE_CONTENT:
                    descriptor = new FileContentDescriptor(name);
                    break;
                case ENUM_VALUES:
                    descriptor = new EnumValuesDescriptor(name);
                    break;
                case BOOLEAN:
                    descriptor = new BooleanDescriptor(name);
                    break;
                case INTEGER:
                    descriptor = new IntegerDescriptor(name);
                    break;
                default:
                    // not expected
                    throw new RuntimeException("Invalid type: " + InputTypes.valueOf(selectedType));
            }
            descriptor.obtainConfiguration();
            items.add(descriptor);
        }

        output("\n-- Serialization --\n");
        boolean serializeNullValues = PromptHelper.promptYesNo("Do you want to serialize null values?");

        // now process the data
        // Probably better if we used Jackson to build this...
        StringBuilder builder = new StringBuilder(2048);
        Pattern pattern = Pattern.compile(matchRegex);
        Matcher matcher = pattern.matcher(inputContent);
        builder.append("[");
        boolean firstMatch = true;
        while (matcher.find()) {
            if (!firstMatch) {
                builder.append(",");
            }
            firstMatch = false;
            builder.append("\n{");

            // process all the descriptors
            boolean firstDescriptor = true;
            for (DataItemDescriptor descriptor: items) {
                descriptor.setMatchedContent(matcher);
                String value = descriptor.getNextValue();
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
        builder.append("]");

        try {
            Files.writeString(outputFile.toPath(), builder.toString());
            output("File %s successfully written\n ", outputFile.getPath());
        } catch (IOException ioException) {
            error("Problem writing output file %s\n", outputFile.getPath());
        }
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
