package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.removecode.Gzip;
import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliCleanResult;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;
import com.github.cregrant.smaliscissors.removecode.method.ArgumentParser;
import com.github.cregrant.smaliscissors.removecode.method.MethodCleaner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.InputMismatchException;

public class ClassMethod implements ClassPart {
    private final SmaliClass smaliClass;
    private final String ref;
    private final String name;
    private final String returnObject;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final boolean isConstructor;
    private boolean deleted = false;
    private String line;
    private Object body;
    private ArrayList<String> inputObjects = new ArrayList<>(0);
    private int end;

    public ClassMethod(SmaliClass smaliClass, String text, int pos) {
        this.smaliClass = smaliClass;
        int signatureEnd = text.indexOf('\n', pos);
        this.line = text.substring(pos, signatureEnd);
        end = text.indexOf(".end method", pos) + 13;
        if (text.startsWith("#Deleted body", end)) {     //there is a deleted method body next to the "end"
            end = text.indexOf(".end method", end + 10) + 13;
            deleted = true;
        }
        if (end == 12) {
            throw new InputMismatchException("Cannot find the end of the method");
        }

        setBody(text.substring(signatureEnd, Math.min(end, text.length())));
        int inputStart = line.indexOf('(');
        int inputEnd = line.indexOf(')');
        int namePos = line.lastIndexOf(' ') + 1;
        name = line.substring(namePos, inputStart);
        isStatic = line.contains(" static ");
        isAbstract = line.contains(" abstract ");
        isConstructor = name.equals("<init>");
        ref = smaliClass.getRef().replace(".smali", "") + "->" + line.substring(namePos);
        returnObject = line.substring(inputEnd + 1);

        if (inputEnd - inputStart > 1) {
            inputObjects = new ArgumentParser().parse(line);
        }
    }

    public String getBody() {
        if (body instanceof Gzip) {
            return ((Gzip) body).decompress();
        } else {
            return (String) body;
        }
    }

    public void setBody(String newBody) {
        if (smaliClass.getProject().getMemoryManager().isExtremeLowMemory()) {
            body = new Gzip(newBody);
        } else {
            body = newBody;
        }
    }

    @Override
    public SmaliCleanResult clean(SmaliTarget target, SmaliClass smaliClass) {
        boolean inParameters = isParametersContainsTarget(target);
        if ((!inParameters && deleted) || (!inParameters && !target.containsInside(getBody()))) {
            return null;
        }
        if (returnObject.contains(target.getRef())) {
            deleteBody();
            return new SmaliCleanResult(getDeleteTarget());
        }

        MethodCleaner cleaner = new MethodCleaner(this, target.getRef());
        HashSet<SmaliTarget> fieldsCanBeNull = cleaner.getFieldsCanBeNull();
        cleaner.clean();
        if (cleaner.isSuccessful()) {
            if (!isAbstract) {
                setBody(cleaner.getNewBody());
            }
            return new SmaliCleanResult(fieldsCanBeNull);
        } else {
            replaceBodyWithStub(generateCommonStub());
            return new SmaliCleanResult(getDeleteTarget(), fieldsCanBeNull);
        }
    }

    @Override
    public void makeStub(SmaliClass smaliClass) {
        if (isConstructor) {
            replaceBodyWithStub(generateConstructorStub());
        } else {
            replaceBodyWithStub(generateCommonStub());
        }
    }

    private boolean isParametersContainsTarget(SmaliTarget target) {
        boolean result = false;
        for (String parameter : inputObjects) {
            if (parameter.contains(target.getRef())) {
                result = true;
                break;
            }
        }
        return result;
    }

    private SmaliTarget getDeleteTarget() {
        String reference;
        if (canBeDeleted()) {
            reference = ref;                                     //method reference
        } else {
            reference = ref.substring(0, ref.indexOf("->"));     //class reference
        }
        return new SmaliTarget().setRef(reference);
    }

    private boolean canBeDeleted() {     //it is better to delete the class with the bridge method. Idk why too.
        return !line.contains(" bridge ");
    }

    private void replaceBodyWithStub(String stub) {        //there is a stub that'll help not to broke everything
        if (deleted) {
            return;
        }
        deleted = true;
        changeBody(stub + "#Deleted body:\n");
    }

    private void deleteBody() {
        if (deleted) {
            return;
        }
        deleted = true;
        line = '#' + line;
        changeBody(null);
    }

    private void changeBody(String stub) {      //internal method
        StringBuilder sb = new StringBuilder();
        if (stub != null) {
            sb.append(stub);
        }
        int pos = sb.length() - 5;
        sb.append(getBody());
        while ((pos = sb.indexOf("\n ", pos + 5)) != -1) {    //comment out every line
            sb.insert(pos + 1, '#');
        }
        sb.insert(sb.lastIndexOf(".end method"), '#');
        setBody(sb.toString());
    }

    public String generateCommonStub() {
        if (returnObject.equals("V")) {
            return "\n    .locals 0\n\n" +
                    "    return-void\n" +
                    ".end method\n\n";
        } else if (returnObject.length() == 1) {
            boolean wide = returnObject.equals("J") || returnObject.equals("D");
            return "\n    .locals " + (wide ? "2" : "1") + "\n\n" +
                    "    const" + (wide ? "-wide" : "/4") + " v0, 0x0\n\n" +
                    "    return" + (wide ? "-wide" : "") + " v0\n" +
                    ".end method\n\n";
        } else {
            return "\n    .locals 1\n\n" +
                    "    new-instance v0, Ljava/lang/Object;\n\n" +
                    "    invoke-direct {v0}, Ljava/lang/Object;-><init>()V\n\n" +
                    "    check-cast v0, " + returnObject + "\n\n" +
                    "    return-object v0\n" +
                    ".end method\n\n";
        }
    }

    public String generateConstructorStub() {
        String body = getBody();
        int start = body.indexOf("    invoke-direct {p0");
        int end = body.indexOf("\n", start);
        if (start == -1 || end == -1) {
            throw new InputMismatchException("Cannot find the superclass call!");
        }
        String call = body.substring(start, end);
        if (call.contains("{p0}")) {
            return "\n    .locals 0\n\n" +
                    "    invoke-direct {p0}, " + smaliClass.getSuperclass() + "-><init>()V\n\n" +
                    "    return-void\n" +
                    ".end method\n\n";
        } else {
            ArrayList<String> parsedArguments = new ArgumentParser().parse(call);
            if (parsedArguments.isEmpty()) {
                throw new IllegalStateException("generateConstructorStub() arguments parse error!");
            }
            StringBuilder opcodes = new StringBuilder();
            StringBuilder registers = new StringBuilder();
            StringBuilder args = new StringBuilder();
            for (int i = 0; i < parsedArguments.size(); i++) {
                opcodes.append(generateObject(parsedArguments.get(i), i));
                registers.append(", v").append(i);
                args.append(parsedArguments.get(i));
            }
            return "\n    .locals " + parsedArguments.size() + "\n\n" +
                    opcodes +
                    "    invoke-direct {p0" + registers + "}, " + smaliClass.getSuperclass() + "-><init>(" + args + ")V\n\n" +
                    "    return-void\n" +
                    ".end method\n\n";

        }
    }

    private String generateObject(String type, int register) {
        if (type.equals("Ljava/lang/String;")) {
            return "    const-string v" + register + ", \"test\"\n\n";
        } else {
            throw new IllegalStateException("Cannot generate the object!");
        }
    }

    public void setInputObjects(ArrayList<String> inputObjects) {
        this.inputObjects = inputObjects;
        StringBuilder sb = new StringBuilder();
        for (String obj : inputObjects) {
            sb.append(obj);
        }
        int inputStart = line.indexOf('(') + 1;
        int inputEnd = line.indexOf(')');
        line = line.substring(0, inputStart) + sb + line.substring(inputEnd);
    }

    @Override
    public int getEndPos() {
        return end;
    }

    @Override
    public String getText() {
        return line + getBody();
    }

    public String getRef() {
        return ref;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getInputObjects() {
        return inputObjects;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public String getLine() {
        return line;
    }

    public String getReturnObject() {
        return returnObject;
    }
}
