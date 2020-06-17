package com.unhuman.dataBuilder.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unhuman.dataBuilder.input.PromptHelper;

public class EmailDescriptor extends DataItemDescriptor {
    @JsonProperty
    private String domain;

    public EmailDescriptor(String name) {
        super(name);
    }

    private EmailDescriptor() {
        // For Jackson
        super();
    }

    @Override
    public void obtainConfiguration() {
        domain = PromptHelper.promptForValue("email domain", "mailinator.com");
    }

    @Override
    public String getNextValue(NullHandler nullHandler) {
        return '"' + new FirstNameDescriptor(getName()).setIterationState(null, getRandomSeed()).getNextValue(NullHandler.AS_NULL) +
                new LastNameDescriptor(getName()).setIterationState(null, getRandomSeed()).getNextValue(NullHandler.AS_NULL) +
                "@" + domain + '"';
    }
}
