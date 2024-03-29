package com.github.cregrant.smaliscissors.removecode.method.opcodes;

import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;

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
            endOpcode = new Opcode(opcode.line);    //can't delete end if it is Unknown opcode
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
    public void deleteLine(ClassMethod method) {
        if (!deleted) {
            tag.deleteLine(method);
            super.deleteLine(method);
            endOpcode.deleteLine(method);
            for (Opcode opcode : content) {
                opcode.deleteLine(method);
            }
        }
        deleted = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(line).append('\n');
        for (Opcode op : content) {
            sb.append(op).append('\n');
        }
        sb.append(endOpcode);
        return sb.toString();
    }
}
