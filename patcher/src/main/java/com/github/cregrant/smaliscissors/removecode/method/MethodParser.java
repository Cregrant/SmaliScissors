package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.method.opcode.Invoke;
import com.github.cregrant.smaliscissors.removecode.method.opcode.InvokeResult;
import com.github.cregrant.smaliscissors.removecode.method.opcode.Opcode;

import java.util.ArrayList;

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
        for (String line : lines) {
            Opcode op = Opcode.parseOpcode(line, target);
            opcodes.add(op);

            if (op instanceof InvokeResult) {   //append InvokeResult to a Invoke opcode
                Opcode prevOpcode = opcodes.get(opcodes.size() - 3);
                if (prevOpcode instanceof Invoke) {
                    ((Invoke) prevOpcode).setInvokeResultLink(op);
                }
            }
        }
        return opcodes;
    }
}

