package com.unhuman.dataBuilder.input;

import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PromptHelper {
    private static final int NUMBER_SELECT_SPACES = 5;
    private static final int STREAM_FLUSH_MS = 25;

    public enum StartingIndex { ZERO, ONE }

    public static String promptForValue(String item, String defaultValue) {
        String input = null;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                if (defaultValue != null) {
                    output("Enter %s (default: %s): ", item, defaultValue);
                } else {
                    output("Enter %s: ", item);
                }
                input = scanner.nextLine();
                if (input == null || input.isBlank()) {
                    return defaultValue;
                }

                return input;
            } catch (Exception e) {
                error("Invalid value: %s\n", input);
            }
        }
    }

    public static String promptForValue(String item) {
        while (true) {
            String value = promptForValue(item, null);
            if (value != null) {
                return value;
            }
        }
    }

    public static int promptIntegerValue(String item) {
        return promptIntegerValue(item, null);
    }

    public static int promptIntegerValue(String item, Integer defaultValue) {
        while (true) {
            if (defaultValue != null) {
                item += " (default " + defaultValue + ")";
            }
            String checkInt = promptForValue(item, (defaultValue != null) ? defaultValue.toString() : null);
            try {
                return Integer.parseInt(checkInt);
            } catch (Exception e) {
                error("Invalid value: %s\n", checkInt);
            }
        }
    }

    public static int promptPercentage(String name, Integer defaultValue) {
        while (true) {
            int percentage = promptIntegerValue("percentage for " + name, defaultValue);
            if (percentage > 0 && percentage <= 100) {
                return percentage;
            }

            error("Percentage (%d) invalid\n", percentage);
        }
    }

    public static int promptPercentage(String name) {
        return promptPercentage(name, null);
    }

    public static String promptFilteredValue(String item, String regexMatcher) {
        while (true) {
            String checkString = promptForValue(item);
            if (checkString.matches(regexMatcher)) {
                return checkString;
            }
            error("Invalid value: %s\n", checkString);
        }
    }

    public static String promptFilteredValue(String item, String regexMatcher, String defaultValue) {
        while (true) {
            String checkString = promptForValue(item, defaultValue);
            if (checkString.matches(regexMatcher)) {
                return checkString;
            }
            error("Invalid value: %s\n", checkString);
        }
    }

    public static String promptForEnumValue(String instructions, StartingIndex startingIndex,
                                            Enum[] acceptableValues) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            output("Select %s:\n", instructions);
            for (int i = startingIndex.ordinal(); i < acceptableValues.length + startingIndex.ordinal(); i++) {
                String number = Integer.toString(i);
                String spaces = IntStream.range(0, NUMBER_SELECT_SPACES - number.length())
                        .mapToObj(j -> " ").collect(Collectors.joining(""));
                output("%s%s. %s\n", spaces, number, acceptableValues[i - startingIndex.ordinal()]);
            }
            output("Selection: ");

            String input = scanner.nextLine();

            for (Enum acceptableValue: acceptableValues) {
                String acceptableValueString = acceptableValue.name();
                if (acceptableValueString.equals(input)) {
                    return acceptableValueString;
                }
            }

            try {
                int selectedIndex = Integer.parseInt(input) - startingIndex.ordinal();
                if (selectedIndex >= 0 && selectedIndex < acceptableValues.length) {
                    return acceptableValues[selectedIndex].name();
                }
            } catch (Exception e) {
                // continue
            }
            error("Invalid value: %s\n", input);
        }
    }

    public static boolean promptYesNo(String message) {
        String checkYesNo = promptFilteredValue(
                message + " (y/n)", "^[YyNn]$");
        return checkYesNo.toLowerCase().equals("y");
    }


    public static void output(String format, Object... args) {
        System.out.printf(format, args);
        System.out.flush();
        waitForFlush();
    }

    public static void error(String format, Object... args) {
        System.err.printf(format, args);
        System.err.flush();
        waitForFlush();
    }

    private static void waitForFlush() {
        // This seems to make sure that the data flushes properly
        try {
            Thread.sleep(STREAM_FLUSH_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
