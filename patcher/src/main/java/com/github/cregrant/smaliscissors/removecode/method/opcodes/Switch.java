package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.List;

public class Switch extends Opcode implements AdditionalTable, Jump {

    private final Tag tag;
    private Table table;

    public Switch(String line) {
        super(line);
        tag = new Tag(line);
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public Tag getTableTag() {
        return tag;
    }

    public List<TableTag> getTableTags() {
        return table.getTableTags();
    }

    @Override
    public Tag getTag() {
        return null;
    }

    @Override
    public void deleteLine() {
        super.deleteLine();
        table.deleteLine();
    }
}
