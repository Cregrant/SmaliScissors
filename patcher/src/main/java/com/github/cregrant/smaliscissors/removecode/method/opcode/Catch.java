package com.github.cregrant.smaliscissors.removecode.method.opcode;

public class Catch extends Opcode {
    private final Tag startTag;
    private final Tag endTag;

    public Catch(String line) {
        super(line);
        int end = line.lastIndexOf("}");
        int pos = line.lastIndexOf(":", end);
        endTag = new Tag(line.substring(pos, end));

        end = line.lastIndexOf(" ..", pos);
        pos = line.lastIndexOf(":", end);
        startTag = new Tag(line.substring(pos, end));
    }

    public Tag getStartTag() {
        return startTag;
    }

    public Tag getEndTag() {
        return endTag;
    }

    public boolean tagsEqual(Opcode other) {
        if (other instanceof Catch) {
            Catch otherCatch = ((Catch) other);
            return startTag.equals(otherCatch.getStartTag()) && endTag.equals(otherCatch.getEndTag());
        }
        return false;
    }

    @Override
    public boolean inputRegisterUsed(String register) {
        return false;
    }
}
