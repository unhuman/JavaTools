package com.unhuman.dataBuilder.descriptor;

public class EmptyDescriptor extends DataItemDescriptor {
    public EmptyDescriptor(String name) {
        super(name);
    }

    private EmptyDescriptor() {
        // For Jackson
        super();
    }

    @Override
    public void obtainConfiguration() {

    }

    @Override
    public String getNextValue(NullHandler nullHandler) {
        return "\"\"";
    }
}
