package com.unhuman.dataBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
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
import com.unhuman.dataBuilder.descriptor.StaticValueDescriptor;
import com.unhuman.dataBuilder.input.PromptHelper;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.unhuman.dataBuilder.input.PromptHelper.error;
import static com.unhuman.dataBuilder.input.PromptHelper.output;
import static java.lang.System.exit;

public class DataBuilder {
    private static final int SUCCESS = 0;
    private static final int CONFIG_ERROR = -1;
    private static final int FILE_ERROR = -2;

    // When updating this list, you need to also update promptSettingsConfig() and getInheritanceObjectMapper()
    private enum InputTypes { ID, BOOLEAN, INTEGER,
        EMAIL, EMPTY_STRING, ENUM_VALUES, FILE_CONTENT, FIRST_NAME, LAST_NAME, STATIC_VALUE }
    private enum SerializationTypes { CSV, JSON }

    protected int process(CommandParams commandParams) {
        String inputContent = null;
        try {
            inputContent = Files.readString(commandParams.getDataInputFile().toPath());
        } catch (Exception e) {
            error("Problem reading in data file: %s: %s\n", inputContent, e.getMessage());
            return FILE_ERROR;
        }

        SettingsConfig settingsConfig;
        if (commandParams.getSettingsConfigInputFile() != null) {
            try {
                settingsConfig = getInheritanceObjectMapper()
                        .readValue(commandParams.getSettingsConfigInputFile(), SettingsConfig.class);
            } catch (Exception e) {
                error("Problem reading in settings/config file: %s: %s\n",
                        commandParams.getSettingsConfigInputFile().getPath(), e.getMessage());
                return FILE_ERROR;
            }
        } else {
            settingsConfig = promptSettingsConfig(inputContent);
        }

        output("\n-- Serialization --\n");

        SerializationTypes serializationType;
        if (commandParams.getDataOutputFile().toString().endsWith(".csv")) {
            serializationType = SerializationTypes.CSV;
        } else if (commandParams.getDataOutputFile().toString().endsWith(".json")) {
            serializationType = SerializationTypes.JSON;
        } else {
            serializationType = SerializationTypes.valueOf(PromptHelper.promptForEnumValue(
                    "serialization desired", PromptHelper.StartingIndex.ONE, SerializationTypes.values()));
        }

        String serializedContent;
        switch (serializationType) {
            case CSV:
                serializedContent = serializeCsv(inputContent, settingsConfig);
                break;
            case JSON:
                boolean serializeNullValues = PromptHelper.promptYesNo("Do you want to serialize null values?");
                serializedContent = serializeJson(inputContent, settingsConfig, serializeNullValues);
                break;
            default:
                throw new RuntimeException("Invalid serialization: " + serializationType);
        }

        int status = SUCCESS;

        try {
            Files.writeString(commandParams.getDataOutputFile().toPath(), serializedContent);
            output("Data file %s successfully written\n", commandParams.getDataOutputFile().getPath());
        } catch (Exception e) {
            error("Problem writing data file %s: %s\n", commandParams.getDataOutputFile().getPath(), e.getMessage());
            status = FILE_ERROR;
        }

        if (commandParams.getSettingsConfigOutputFile() != null) {
            try {
                ObjectMapper objectMapper = getInheritanceObjectMapper();
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(commandParams.getSettingsConfigOutputFile(), settingsConfig);
                output("Settings/config file %s successfully written\n",
                        commandParams.getSettingsConfigOutputFile().getPath());
            } catch (Exception e) {
                error("Problem writing settings/config file: %s: %s\n",
                        commandParams.getSettingsConfigOutputFile().getPath(), e.getMessage());
                status = FILE_ERROR;
            }
        }

        return status;
    }

    private SettingsConfig promptSettingsConfig(String inputContent) {
        SettingsConfig settingsConfig = new SettingsConfig();
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
                error("Invalid regex: %s in file: %s\n", matchRegex, e.getMessage());
            }
        }
        settingsConfig.setRegex(matchRegex);

        List<Enum> availableInputTypes = new ArrayList<>();
        for (InputTypes inputType: InputTypes.values()) {
            availableInputTypes.add(inputType);
        }

        while (true) {
            output("\n-- New Field --\n");
            String name = PromptHelper.promptForValue("field name (empty to stop)", "");

            // Empty name = we're done
            if (name.isBlank()) {
                break;
            }

            // Ensure name doesn't already exist
            if (settingsConfig.getSettings().stream().anyMatch(item -> item.getName().equals(name))) {
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
                case STATIC_VALUE:
                    descriptor = new StaticValueDescriptor(name);
                    break;
                default:
                    // not expected
                    throw new RuntimeException("Invalid type: " + InputTypes.valueOf(selectedType));
            }
            descriptor.obtainConfiguration();
            settingsConfig.addSetting(descriptor);
        }

        return settingsConfig;
    }

    private String serializeJson(String inputContent, SettingsConfig settingsConfig,
                                 boolean serializeNullValues) {
        StringBuilder builder = new StringBuilder(2048);
        Pattern pattern = Pattern.compile(settingsConfig.getRegex());
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
            for (DataItemDescriptor descriptor: settingsConfig.getSettings()) {
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

    private String serializeCsv(String inputContent, SettingsConfig settingsConfig) {
        StringBuilder builder = new StringBuilder(2048);
        Pattern pattern = Pattern.compile(settingsConfig.getRegex());
        Matcher matcher = pattern.matcher(inputContent);

        builder.append(settingsConfig.getSettings().stream().map(item ->
                item.getName()).collect(Collectors.joining(",")));

        Random random = new Random();
        while (matcher.find()) {
            // Create a seed for each round
            long randomSeed = random.nextLong();

            builder.append("\n");
            builder.append(settingsConfig.getSettings().stream().map(item ->
                    item.setIterationState(matcher, randomSeed)
                            .getNextValue(DataItemDescriptor.NullHandler.EMPTY))
                    .collect(Collectors.joining(",")));
        }
        return builder.toString();
    }

    public static void main(String[] args) {
        CommandParams commandParams = new CommandParams();
        CommandLine commandLine = new CommandLine(commandParams);
        int response = commandLine.execute(args);
        if (response != 0) {
            commandLine.usage(System.out);
            exit(response);
        }

        DataBuilder dataBuilder = new DataBuilder();
        exit(dataBuilder.process(commandParams));
    }

    private ObjectMapper getInheritanceObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Enable polymorphism
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator());
        // Register types of polymorphism
        objectMapper.registerSubtypes(new NamedType(BooleanDescriptor.class, BooleanDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(EmailDescriptor.class, EmailDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(EmptyDescriptor.class, EmptyDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(EnumValuesDescriptor.class, EnumValuesDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(FileContentDescriptor.class, FileContentDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(FirstNameDescriptor.class, FirstNameDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(IdDescriptor.class, IdDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(IntegerDescriptor.class, IntegerDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(LastNameDescriptor.class, LastNameDescriptor.class.getSimpleName()));
        objectMapper.registerSubtypes(new NamedType(StaticValueDescriptor.class, StaticValueDescriptor.class.getSimpleName()));

        return objectMapper;
    }

    private static class CommandParams implements Runnable, CommandLine.IExitCodeGenerator {
        @picocli.CommandLine.Option(names = {"-h", "--help", "-?"}, usageHelp = true,
                description = "display a help message")
        private boolean helpRequested = false;

        @picocli.CommandLine.Option(names = {"-i", "--input"}, paramLabel = "INPUT_SETTINGS_FILE",
                description = "settings/config input file (not the data input file)")
        private File settingsConfigInputFile;

        @picocli.CommandLine.Option(names = {"-o", "--output"}, paramLabel = "OUTPUT_SETTINGS_FILE",
                description = "settings/config output file (not the data output file)")
        private File settingsConfigOutputFile;

        @CommandLine.Parameters(index = "0", paramLabel="DATA_INPUT_FILE")
        private File dataInputFile;

        @CommandLine.Parameters(index = "1", paramLabel="DATA_OUTPUT_FILE")
        private File dataOutputFile;


        public File getSettingsConfigInputFile() {
            return settingsConfigInputFile;
        }

        public File getSettingsConfigOutputFile() {
            return settingsConfigOutputFile;
        }

        public File getDataInputFile() {
            return dataInputFile;
        }

        public File getDataOutputFile() {
            return dataOutputFile;
        }

        @Override
        public void run() {
            // Nothing
        }

        @Override public int getExitCode() {
            int exitCode = 0;

            // Validate configSettings
            if (getSettingsConfigInputFile() != null && !getSettingsConfigInputFile().exists()) {
                error("Settings/Config input file must exist: %s\n", getSettingsConfigInputFile().getPath());
                exitCode = CONFIG_ERROR;
            }
            if (getSettingsConfigOutputFile() != null && getSettingsConfigOutputFile().exists()) {
                error("Settings/Config output file must not exist: %s\n", getSettingsConfigOutputFile().getPath());
                exitCode = CONFIG_ERROR;
            }

            // Validate input / output files
            if (getDataInputFile() == null || getDataInputFile() == null) {
                error("Data input and output files must be specified\n");
                exitCode = CONFIG_ERROR;
            } else {
                if (!getDataInputFile().exists()) {
                    error("Data input file must exist: %s\n", getDataInputFile().getPath());
                    exitCode = CONFIG_ERROR;
                }
                if (getDataOutputFile().exists()) {
                    error("Data output file must not exist: %s\n", getDataOutputFile());
                    exitCode = CONFIG_ERROR;
                }
            }

            return exitCode;
        }
    }
}
