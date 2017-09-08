package org.phoebus.core.types;

import java.io.Serializable;

@SuppressWarnings("nls")
public class ProcessVariable implements Serializable {
    private static final long serialVersionUID = -2697682613620592711L;
    private final String name;

    public ProcessVariable(String name) {
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
