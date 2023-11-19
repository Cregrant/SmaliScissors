package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.Flags;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class OpcodeArrayCleaner {
    private final MethodOpcodeCleaner cleaner;
    private final HashMap<String, ArrayCluster> tracked = new HashMap<>();
    private final ArrayList<String> duplicatedRegisters = new ArrayList<>();

    OpcodeArrayCleaner(MethodOpcodeCleaner cleaner) {
        this.cleaner = cleaner;
    }

    void scan() {
        ArrayList<Opcode> opcodes = cleaner.getOpcodes();
        for (int i = 0; i < opcodes.size(); i++) {
            Opcode opcode = opcodes.get(i);
            if (opcode instanceof NewArray) {
                NewArray newArrayOpcode = ((NewArray) opcode);
                ArrayCluster cluster = new ArrayCluster(new PositionalOpcode(newArrayOpcode, i));
                String arrayRegister = newArrayOpcode.getArrayRegister();
                if (tracked.get(arrayRegister) != null) {
                    duplicatedRegisters.add(arrayRegister);
                }
                tracked.put(newArrayOpcode.getArrayRegister(), cluster);
            }
        }
        for (int i = 0; i < opcodes.size(); i++) {
            Opcode opcode = opcodes.get(i);
            if (!(opcode instanceof ArrayPut || opcode instanceof ArrayGet)) {
                continue;
            }
            String arrayRegister = ((Array) opcode).getArrayRegister();
            ArrayCluster arrayCluster = tracked.get(arrayRegister);
            if (arrayCluster != null) {
                arrayCluster.addUsage(new PositionalOpcode(opcode, i));  //arrayCluster is null if array is provided through method arguments
            }
        }
    }

    List<MethodCleaner.Line> delete(Opcode op) {
        if (tracked.isEmpty()) {
            scan();
        }
        if (Flags.SMALI_PRESERVE_PARTIALLY_CLEANED_ARRAYS) {
            op.deleteLine();
            return Collections.emptyList();
        } else {
            String arrayRegister = ((Array) op).getArrayRegister();
            ArrayCluster arrayCluster = tracked.get(arrayRegister);
            if (duplicatedRegisters.contains(arrayRegister) || arrayCluster == null) {
                throw new IllegalArgumentException("Can't process array removal. Deleting method...");
            }
            return arrayCluster.delete();
        }
    }

    static class ArrayCluster {
        final PositionalOpcode newArray;
        final ArrayList<PositionalOpcode> usages = new ArrayList<>();

        ArrayCluster(PositionalOpcode newArray) {
            this.newArray = newArray;
        }

        void addUsage(PositionalOpcode opcode) {
            usages.add(opcode);
        }

        public List<MethodCleaner.Line> delete() {
            ArrayList<MethodCleaner.Line> deletedLines = new ArrayList<>();

            newArray.opcode.deleteLine();
            deletedLines.add(new MethodCleaner.Line(newArray.opcode.getOutputRegister(), newArray.pos));

            for (PositionalOpcode opcode : usages) {
                opcode.opcode.deleteLine();
                String register = opcode.opcode.getOutputRegister();
                if (!register.isEmpty()) {
                    deletedLines.add(new MethodCleaner.Line(opcode.opcode.getOutputRegister(), opcode.pos));
                }
            }
            return deletedLines;
        }
    }

    static class PositionalOpcode {
        final Opcode opcode;
        final int pos;

        PositionalOpcode(Opcode opcode, int pos) {
            this.opcode = opcode;
            this.pos = pos;
        }
    }
}
