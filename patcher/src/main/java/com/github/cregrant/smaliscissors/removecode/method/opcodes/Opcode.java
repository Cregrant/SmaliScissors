package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;

import java.util.LinkedList;
import java.util.Objects;

public class Opcode {
    protected final LinkedList<String> inputRegisters = new LinkedList<>();
    protected String outputRegister = "";
    protected boolean deleted;
    protected String line;

    protected Opcode(String line) {
        this.line = line;
        if (line == null || line.isEmpty() || line.charAt(0) == '#') {
            deleted = true;
        }
    }

    public static Opcode parseOpcode(String str, String target) {
        Opcode result;

        int offset = str.startsWith("#") ? 5 : 4;
        int end = str.indexOf(" ", 8);

        if (str.isEmpty()) {
            result = new Blank();
        } else if (str.startsWith(".line", offset)) {
            result = new Debug(str);
        } else if (str.startsWith("invoke", offset)) {
            result = new Invoke(str, target);
        } else if (str.startsWith("const", offset) || str.startsWith("move-exception", offset)) {
            result = new Create(str);
        } else if (str.startsWith("new-instance", offset)) {
            result = new NewInstance(str);
        } else if (str.startsWith("sget", offset) || str.startsWith("iget", offset)) {
            result = new Get(str);
        } else if (str.startsWith("sput", offset) || str.startsWith("iput", offset)) {
            result = new Put(str);
        } else if (str.startsWith("move-result", offset) && str.charAt(9) != 'e') {    //not move-exception
            result = new MoveResult(str);
        } else if (str.startsWith("move", offset) && str.charAt(9) != 'e') {
            result = new Move(str);
        } else if (str.startsWith("goto", offset)) {
            result = new Goto(str);
        } else if (str.startsWith("if", offset)) {
            result = new If(str);
        } else if (str.startsWith(":", offset)) {
            result = new Tag(str);
        } else if (str.startsWith("return", offset)) {
            result = new Return(str);
        } else if (str.startsWith("throw", offset)) {
            result = new Throw(str);
        } else if (str.startsWith("instance-of", offset) || str.startsWith("array-length", offset) ||
                str.startsWith("neg-", offset) || str.startsWith("not-", offset) ||
                str.contains("-to-") || str.startsWith("rsub-int", offset) ||
                str.startsWith("/lit8", end - 5) || str.startsWith("/lit16", end - 6)) {
            result = new Transform(str);
        } else if (str.startsWith("/2addr", end - 6)) {
            result = new DoubleMath(str);
        } else if (str.startsWith("check-cast", offset)) {
            result = new Cast(str);
        } else if (str.startsWith("new-array", offset)) {
            result = new NewArray(str);
        } else if (str.startsWith("aput", offset)) {
            result = new ArrayPut(str);
        } else if (str.startsWith("aget", offset)) {
            result = new ArrayGet(str);
        } else if (str.startsWith(".catch", offset)) {
            result = new Catch(str);
        } else if (str.startsWith("fill-array-data", offset)) {
            result = new FillArrayData(str);
        } else if (str.startsWith("filled-new-array", offset)) {
            result = new FilledNewArray(str);
        } else if (str.startsWith("monitor", offset)) {
            result = new Monitor(str);
        } else if (str.startsWith("switch", offset + 7)) {
            result = new Switch(str);
        } else if (str.startsWith("cmp", offset) || str.startsWith("add-", offset) ||
                str.startsWith("sub-", offset) || str.startsWith("mul-", offset) ||
                str.startsWith("div-", offset) || str.startsWith("rem-", offset) ||
                str.startsWith("and-", offset) || str.startsWith("or-", offset) ||
                str.startsWith("xor-", offset) || str.startsWith("shl-", offset) ||
                str.startsWith("shr-", offset) || str.startsWith("ushr-", offset)) {
            result = new TripleMath(str);
        } else if (str.startsWith(".array-data", offset) ||
                str.startsWith(".packed-switch", offset) || str.startsWith(".sparse-switch", offset)) {
            result = new Table(str);
        } else {
            result = new Unknown(str);
        }
        return result;
    }

    public static Opcode parseTableOpcode(String str) {
        if (str.contains(":")) {
            return new TableTag(str);     //0x41 -> :sswitch_5
        } else {
            return new Unknown(str);      //0x10101a5
        }
    }

    public boolean inputRegisterUsed(String register) {
        return !inputRegisters.isEmpty() && inputRegisters.contains(register);
    }

    public void scanRegisters() {
        int start = line.indexOf(" ", 8) + 1;
        if (start == 0) {
            return;
        }

        int startPos = 0;
        int i = start;
        for (; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == 'v' || ch == 'p') {
                startPos = i;
                i++;
            } else if (startPos != 0 && (ch == ',' || ch == ' ')) {
                inputRegisters.add(line.substring(startPos, i));
                startPos = 0;
            } else if (ch == 'L' || ch == ':' || ch == '}' || ch == '"') {
                break;
            }
        }
        if (startPos != 0) {
            inputRegisters.add(line.substring(startPos, i));
        }

        int dotPos = start + 5;
        if (inputRegisters.size() == 2 && line.length() > dotPos && line.charAt(dotPos) == '.') {
            fillRegistersRanged();     //range call like {v1 .. v5} for filled-new-array or invoke opcode
        }
    }

    private void fillRegistersRanged() {
        String type = inputRegisters.getFirst().substring(0, 1);
        int start = Integer.parseInt(inputRegisters.getFirst().substring(1));
        int end = Integer.parseInt(inputRegisters.removeLast().substring(1));
        for (int i = start + 1; i <= end; i++) {
            inputRegisters.add(type + i);
        }
    }

    public void deleteLine(ClassMethod method) {
        if (!deleted) {
            line = "#" + line;
        }
        deleted = true;
    }

    public String getOutputRegister() {
        return outputRegister;
    }

    public LinkedList<String> getInputRegisters() {
        return inputRegisters;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Opcode opcode = (Opcode) o;
        return deleted == opcode.deleted && line.equals(opcode.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deleted, line);
    }

    @Override
    public String toString() {
        return line;
    }

}
