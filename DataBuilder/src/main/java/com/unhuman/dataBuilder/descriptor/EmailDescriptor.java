package com.unhuman.dataBuilder.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unhuman.dataBuilder.input.PromptHelper;

public class EmailDescriptor extends AbstractCohesiveDataDescriptor {
    @JsonProperty
    private String domain;

    public EmailDescriptor(String name) {
        super(name);
    }

    // For Jackson
    private EmailDescriptor() {
        super();
    }

    @Override
    public void obtainConfiguration() {
        domain = PromptHelper.promptForValue("email domain", "mailinator.com");
    }

    @Override
    public String getNextValue(NullHandler nullHandler) {
        // Track this item
        getNextRandom(Integer.MAX_VALUE);

        return '"' +
                new FirstNameDescriptor(getName()).getSeededFirstName(getRandomSeed()).substring(0, 1) +
                new LastNameDescriptor(getName()).getSeededLastName(getRandomSeed()) +
                "@" + domain + '"';
    }
}
