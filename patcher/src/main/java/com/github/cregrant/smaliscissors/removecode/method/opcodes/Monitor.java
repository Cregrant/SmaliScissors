package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.Objects;

public class Monitor extends Opcode {

    private boolean isStart;

    protected Monitor(String line) {
        super(line);
        scanRegisters();
        if (line.contains("enter")) {
            isStart = true;
        }
    }

    public boolean isStart() {
        return isStart;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Monitor monitor = (Monitor) o;
        return inputRegisters.equals(monitor.inputRegisters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputRegisters);
    }
}
