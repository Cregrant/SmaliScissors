package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;

public class MethodParser {

    private static final Logger logger = LoggerFactory.getLogger(MethodParser.class);
    private final ClassMethod method;
    private final String target;
    private ArrayList<Opcode> opcodes;
    private int pos;

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
        opcodes = new ArrayList<>(lines.length);
        HashMap<Tag, Table> tables = new HashMap<>();

        for (; pos < lines.length; pos++) {
            String line = lines[pos];
            Opcode op = Opcode.parseOpcode(line, target);
            opcodes.add(op);

            if (op instanceof MoveResult) {
                appendMoveResult(op);
            } else if (op instanceof Table) {
                Table table = (Table) op;
                fillTable(lines, table);
                tables.put(table.getTag(), table);
            }
        }

        for (Opcode op : opcodes) {         //map tables to opcodes
            if (op instanceof AdditionalTable) {
                AdditionalTable aTable = ((AdditionalTable) op);
                Table mappedTable = tables.get(aTable.getTableTag());
                if (mappedTable == null) {
                    throw new InputMismatchException("Error mapping table to an opcode!");
                }
                aTable.setTable(mappedTable);
            }
        }
        return opcodes;
    }

    private void appendMoveResult(Opcode op) {     //append MoveResult to a prev opcode
        Opcode prevOpcode = opcodes.get(opcodes.size() - 3);
        if (prevOpcode instanceof Invoke) {
            ((Invoke) prevOpcode).setMoveResultLink(op);
        } else if (prevOpcode instanceof FilledNewArray) {
            ((FilledNewArray) prevOpcode).setMoveResultLink(op);
        }
    }

    private void fillTable(String[] lines, Table table) {
        table.setTag((Tag) opcodes.get(opcodes.size() - 2));
        boolean nextExists;
        do {
            pos++;
            Opcode op = Opcode.parseTableOpcode(lines[pos]);
            nextExists = table.addOpcode(op);
        } while (nextExists && pos < lines.length);
    }
}

