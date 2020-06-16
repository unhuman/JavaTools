package com.unhuman.dataBuilder.descriptor;

import com.unhuman.dataBuilder.input.PromptHelper;

import java.util.regex.Matcher;

public class FileContentDescriptor extends DataItemDescriptor {
    private int fieldFromContent = -1;
    private boolean isString = false;

    public FileContentDescriptor(String name) {
        super(name);
    }

    @Override
    public void obtainConfiguration() {
        fieldFromContent = PromptHelper.promptIntegerValue("regex group");
        String checkYesNo = PromptHelper.promptFilteredValue(
                getName() + " is String value? (y/n)", "^[YyNn]$");
        isString = checkYesNo.toLowerCase().equals("y");
    }

    @Override
    public String getNextValue(NullHandler nullHandler) {
        Matcher matcher = getCurrentMatcherState();
        String value = matcher.group(fieldFromContent);

        if (value == null) {
            return NullHandler.AS_NULL.equals(nullHandler) ? null : "";
        }
        return isString ? '"' + value + '"' : value;
    }
}
