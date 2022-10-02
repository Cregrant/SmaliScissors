package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Table extends Opcode {

    private Tag tag;
    private Opcode endOpcode;
    private final ArrayList<Opcode> content = new ArrayList<>();

    public Table(String line) {
        super(line);
    }

    public boolean addOpcode(Opcode opcode) {
        if (!opcode.line.contains(".end")) {
            content.add(opcode);    //Unknown for .array-data and Tag for others
            return true;
        } else {
            endOpcode = opcode;
            return false;
        }
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

    List<TableTag> getTableTags() {
        if (content.isEmpty() || content.get(0) instanceof Unknown) {
            return Collections.emptyList();
        } else {
            List<TableTag> list = new ArrayList<>(content.size());
            for (Opcode opcode : content) {
                if (opcode instanceof TableTag) {
                    list.add(((TableTag) opcode));
                }
            }
            return list;
        }
    }

    @Override
    public void deleteLine() {
        if (!deleted) {
            tag.deleteLine();
            super.deleteLine();
            endOpcode.deleteLine();
            for (Opcode opcode : content) {
                opcode.deleteLine();
            }
        }
        deleted = true;
    }
}
