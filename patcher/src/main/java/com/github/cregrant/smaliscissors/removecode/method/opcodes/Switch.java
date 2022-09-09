package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.List;

public class Switch extends If implements AdditionalTable {

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
        throw new IllegalCallerException("Invalid switch tag access!");
    }

    @Override
    public void deleteLine() {
        super.deleteLine();
        table.deleteLine();
    }
}
