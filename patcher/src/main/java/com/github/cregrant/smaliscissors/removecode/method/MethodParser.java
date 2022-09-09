package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.*;

import java.util.ArrayList;
import java.util.HashMap;

public class MethodParser {
    private final ClassMethod method;
    private final String target;

    public MethodParser(ClassMethod method, String target) {
        this.method = method;
        this.target = target;
    }

    public ArrayList<Opcode> parse() {
        String methodBody = method.getBody();
        String[] lines = methodBody.split("\n");
        if (methodBody.endsWith("\n\n")) {
            lines[lines.length - 1] = lines[lines.length - 1] + "\n";
        }
        ArrayList<Opcode> opcodes = new ArrayList<>(lines.length);
        HashMap<Tag, Table> tables = new HashMap<>();
        boolean tableStarted = false;
        Table table = null;

        for (String line : lines) {
            Opcode op;
            if (tableStarted) {
                op = Opcode.parseTableOpcode(line);
                tableStarted = table.addOpcode(op);     //false if reach end
            } else {
                op = Opcode.parseOpcode(line, target);
            }
            opcodes.add(op);

            if (op instanceof MoveResult) {   //append MoveResult to a prev opcode
                Opcode prevOpcode = opcodes.get(opcodes.size() - 3);
                if (prevOpcode instanceof Invoke) {
                    ((Invoke) prevOpcode).setMoveResultLink(op);
                } else if (prevOpcode instanceof FilledNewArray) {
                    ((FilledNewArray) prevOpcode).setMoveResultLink(op);
                }
            } else if (op instanceof Table) {
                Table newTable = (Table) op;
                newTable.setTag((Tag) opcodes.get(opcodes.size() - 2));
                table = newTable;
                tables.put(table.getTag(), table);
                tableStarted = true;
            }
        }

        for (Opcode op : opcodes) {
            if (op instanceof AdditionalTable) {
                AdditionalTable aTable = ((AdditionalTable) op);
                Table mappedTable = tables.get(aTable.getTableTag());
                if (mappedTable == null) {
                    throw new IllegalStateException("Error mapping table to an opcode!");
                }
                aTable.setTable(mappedTable);
            }
        }

        return opcodes;
    }
}

