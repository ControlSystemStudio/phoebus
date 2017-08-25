package org.phoebus.core.types;

@SuppressWarnings("nls")
public class ProcessVariable {

    private final String name;

    public ProcessVariable(String name) {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ProcessVariable(" + name + ")";
    }
}
