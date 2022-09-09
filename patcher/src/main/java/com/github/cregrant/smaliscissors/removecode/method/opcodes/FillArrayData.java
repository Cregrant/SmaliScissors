package com.github.cregrant.smaliscissors.removecode.method.opcodes;

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
    public void deleteLine() {
        super.deleteLine();
        table.deleteLine();
    }
}
