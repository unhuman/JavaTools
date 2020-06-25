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
import com.unhuman.dataBuilder.descriptor.TextContentDescriptor;
import com.unhuman.dataBuilder.input.PromptHelper;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private enum ContentTypes { ID, BOOLEAN, INTEGER,
        EMAIL, EMPTY_STRING, ENUM_VALUES, FILE_CONTENT, FIRST_NAME, LAST_NAME, STATIC_VALUE, TEXT }
    private enum SerializationTypes { CSV, JSON, TOKEN_REPLACEMENT}

    private enum InputFileType { EXTRACTION, TOKEN_BASED }
    // $[name] tokens to treat inputfile as replacements
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\$\\[(.*?)\\]");

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

        SerializationTypes serializationType;
        if (settingsConfig.isTokenBased()) {
            serializationType = SerializationTypes.TOKEN_REPLACEMENT;
        } else {
            output("\n-- Serialization --\n");

            if (commandParams.getDataOutputFile().toString().endsWith(".csv")) {
                serializationType = SerializationTypes.CSV;
            } else if (commandParams.getDataOutputFile().toString().endsWith(".json")) {
                serializationType = SerializationTypes.JSON;
            } else {
                serializationType = SerializationTypes.valueOf(PromptHelper.promptForEnumValue(
                        "serialization desired", PromptHelper.StartingIndex.ONE, SerializationTypes.values()));
            }
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
            case TOKEN_REPLACEMENT:
                serializedContent = serializeDirect(inputContent, settingsConfig);
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

        List<Enum> availableContentTypes = new ArrayList<>();
        for (ContentTypes contentType: ContentTypes.values()) {
            availableContentTypes.add(contentType);
        }

        // behave differently if the inputContent is an existing file with replacements
        // versus content to be extracted.

        Matcher tokenPatternMatcher = TOKEN_PATTERN.matcher(inputContent);
        LinkedHashSet<String> tokenNames = null;
        if (tokenPatternMatcher.find()) {
            // We don't allow FILE_CONTENT data because we're already in the file
            availableContentTypes.remove(ContentTypes.FILE_CONTENT);
            tokenNames = new LinkedHashSet<>();
            do {
                tokenNames.add(tokenPatternMatcher.group(1));
            } while (tokenPatternMatcher.find());
            settingsConfig.setReplacementTokens(tokenNames);
        } else {
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
        }

        int currentTokenName = 0;
        while (true) {
            String name;
            if (tokenNames != null) {
                if (currentTokenName >= tokenNames.size()) {
                    break;
                }

                name = tokenNames.toArray(new String[tokenNames.size()])[currentTokenName];
                ++currentTokenName;
            } else {
                output("\n-- New Field --\n");
                name = PromptHelper.promptForValue("field name (empty to stop)", "");

                // Empty name = we're done
                if (name.isBlank()) {
                    break;
                }

                // Ensure name doesn't already exist
                if (settingsConfig.getSettings().stream().anyMatch(item -> item.getName().equals(name))) {
                    error("Name: %s already exists\n", name);
                    continue;
                }
            }

            ContentTypes[] displayContentTypes = new ContentTypes[availableContentTypes.size()];
            String selectedType =
                    PromptHelper.promptForEnumValue("data type for " + name,
                            PromptHelper.StartingIndex.ONE,
                            availableContentTypes.toArray(displayContentTypes));

            DataItemDescriptor descriptor = null;
            switch (ContentTypes.valueOf(selectedType)) {
                case ID:
                    // only permit one id
                    availableContentTypes.remove(ContentTypes.ID);
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
                case TEXT:
                    descriptor = new TextContentDescriptor(name);
                    break;
                default:
                    // not expected
                    throw new RuntimeException("Invalid type: " + ContentTypes.valueOf(selectedType));
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

    private String serializeDirect(String inputContent, SettingsConfig settingsConfig) {
        StringBuilder builder = new StringBuilder(2048);

        // TODO: Fix random seeding
        // TODO: Perhaps this could be a function of changing FirstName + LastName + Email to be intermingledly aware.
        Random random = new Random();
        long randomSeed = random.nextLong();

        Matcher tokenMatcher = TOKEN_PATTERN.matcher(inputContent);
        while (tokenMatcher.find()) {
            String tokenName = tokenMatcher.group(1);
            String value = settingsConfig.getSetting(tokenName).getNextValue(DataItemDescriptor.NullHandler.AS_NULL);
            tokenMatcher.appendReplacement(builder, value);
        }
        tokenMatcher.appendTail(builder);
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
        objectMapper.registerSubtypes(new NamedType(TextContentDescriptor.class, TextContentDescriptor.class.getSimpleName()));

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
