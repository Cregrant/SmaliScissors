package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.Objects;

public class Monitor extends Opcode {

    protected Monitor(String line) {
        super(line);
        scanRegisters();
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
