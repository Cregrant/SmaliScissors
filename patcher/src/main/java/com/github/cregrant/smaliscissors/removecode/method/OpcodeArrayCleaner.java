package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.Flags;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
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
            boolean arrayCreated = opcode instanceof NewArray;
            boolean arrayFetchedByMethod = opcode instanceof Invoke && opcode.toString().contains(")[");
            boolean arrayFetchedByField = opcode instanceof Get && opcode.toString().contains(":[");
            if (!arrayCreated && !arrayFetchedByMethod && !arrayFetchedByField) {
                continue;
            }

            ArrayCluster cluster = new ArrayCluster(new PositionalOpcode(opcode, i));
            String arrayRegister = opcode.getOutputRegister();
            if (tracked.get(arrayRegister) != null) {
                duplicatedRegisters.add(arrayRegister);
            }
            tracked.put(arrayRegister, cluster);

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

    ArrayCleanerResult delete(Opcode op) {
        if (tracked.isEmpty()) {
            scan();
        }
        if (Flags.SMALI_PRESERVE_PARTIALLY_CLEANED_ARRAYS) {
            op.deleteLine(cleaner.getMethod());
            return ArrayCleanerResult.empty();
        } else {
            String arrayRegister = ((Array) op).getArrayRegister();
            ArrayCluster arrayCluster = tracked.get(arrayRegister);
            if (duplicatedRegisters.contains(arrayRegister) || arrayCluster == null) {
                throw new IllegalArgumentException("Can't process array removal. Deleting method...");
            }

            List<MethodCleaner.Line> lines = arrayCluster.delete(cleaner.getMethod());
            List<Opcode> insertList = arrayCluster.array.opcode instanceof Invoke ? ((Invoke) arrayCluster.array.opcode).getAndClearInsertList() : Collections.<Opcode>emptyList();
            return new ArrayCleanerResult(lines, insertList);
        }
    }

    static class ArrayCluster {
        final PositionalOpcode array;
        final ArrayList<PositionalOpcode> usages = new ArrayList<>();

        ArrayCluster(PositionalOpcode array) {
            this.array = array;
        }

        void addUsage(PositionalOpcode opcode) {
            usages.add(opcode);
        }

        public List<MethodCleaner.Line> delete(ClassMethod method) {
            ArrayList<MethodCleaner.Line> deletedLines = new ArrayList<>();

            array.opcode.deleteLine(method);
            deletedLines.add(new MethodCleaner.Line(array.opcode.getOutputRegister(), array.pos));

            for (PositionalOpcode positionalOpcode : usages) {
                Opcode opcode = positionalOpcode.opcode;
                opcode.deleteLine(method);
                if (!opcode.getOutputRegister().isEmpty()) {
                    deletedLines.add(new MethodCleaner.Line(opcode.getOutputRegister(), positionalOpcode.pos));
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

    static class ArrayCleanerResult {
        final List<MethodCleaner.Line> lines;
        final List<Opcode> insertList;

        public ArrayCleanerResult(List<MethodCleaner.Line> lines, List<Opcode> insertList) {
            this.lines = lines;
            this.insertList = insertList;
        }

        public static ArrayCleanerResult empty() {
            return new ArrayCleanerResult(Collections.<MethodCleaner.Line>emptyList(), Collections.<Opcode>emptyList());
        }
    }
}
