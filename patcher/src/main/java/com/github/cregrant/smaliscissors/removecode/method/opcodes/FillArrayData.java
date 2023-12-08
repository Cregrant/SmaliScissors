package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;

public class FillArrayData extends Opcode implements AdditionalTable {

    private final Tag tag;
    private Table table;

    public FillArrayData(String line) {
        super(line);
        scanRegisters();
        tag = new Tag(line);
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public Tag getTableTag() {
        return tag;
    }

    @Override
    public void deleteLine(ClassMethod method) {
        super.deleteLine(method);
        table.deleteLine(method);
    }
}
