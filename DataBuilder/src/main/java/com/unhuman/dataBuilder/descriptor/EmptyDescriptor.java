package com.unhuman.dataBuilder.descriptor;

public class EmptyDescriptor extends AbstractEntityTypeDescriptor {
    public EmptyDescriptor(String name) {
        super(name);
    }

    // For Jackson
    private EmptyDescriptor() {
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
