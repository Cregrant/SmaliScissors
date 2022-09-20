package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.removecode.Gzip;
import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;
import com.github.cregrant.smaliscissors.removecode.method.ArgumentParser;
import com.github.cregrant.smaliscissors.removecode.method.MethodCleaner;

import java.util.ArrayList;

public class ClassMethod implements ClassPart {
    private final SmaliClass smaliClass;
    private final String ref;
    private final String name;
    private final String returnObject;
    private final String signature;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final boolean isConstructor;
    private String modifiers;
    private Object body;
    private ArrayList<String> inputObjects = new ArrayList<>(2);
    private int end;

    public ClassMethod(SmaliClass smaliClass, String text, int pos) {
        int signatureEnd = text.indexOf('\n', pos);
        String line = text.substring(pos, signatureEnd);
        end = text.indexOf(".end method", pos) + 13;
        if (text.startsWith("#Deleted body", end)) {     //there is deleted method body next to the "end"
            end = text.indexOf(".end method", end + 10) + 13;
        }
        if (end == 12) {
            throw new IllegalArgumentException();
        }

        setBody(text.substring(signatureEnd, Math.min(end, text.length())));
        int inputStart = line.indexOf('(');
        int inputEnd = line.indexOf(')');
        int namePos = line.lastIndexOf(' ') + 1;
        signature = line.substring(namePos);
        modifiers = line.substring(0, namePos);
        name = line.substring(namePos, inputStart);
        isStatic = line.contains(" static ");
        isAbstract = line.contains(" abstract ");
        isConstructor = name.equals("<init>");
        ref = smaliClass.getRef().replace(".smali", "") + "->" + getSignature();
        returnObject = line.substring(inputEnd + 1);
        this.smaliClass = smaliClass;

        if (inputEnd - inputStart > 1) {
            inputObjects = new ArgumentParser().parse(line);
        }
    }

    private String buildSignature(ArrayList<String> input) {
        StringBuilder sb = new StringBuilder();
        for (String obj : input) {
            sb.append(obj);
        }
        return modifiers + name + '(' + sb + ')' + returnObject;
    }

    public String getBody() {
        if (Prefs.allowCompression) {
            return ((Gzip) body).decompress();
        } else {
            return (String) body;
        }
    }

    public void setBody(String newBody) {
        if (Prefs.allowCompression) {
            body = new Gzip(newBody);
        } else {
            body = newBody;
        }
    }

    @Override
    public SmaliTarget clean(SmaliTarget target, SmaliClass smaliClass) {
        String oldBody = getBody();
        if (!getSignature().contains(target.getRef()) && !target.containsInside(oldBody)) {
            return null;
        }
        if (returnObject.contains(target.getRef())) {
            deleteBody();
            return getDeleteTarget();
        }

        MethodCleaner cleaner = new MethodCleaner(this, target.getRef());
        cleaner.clean();
        if (target.getRef().contains("Lcom/applovin/") && cleaner.getNewBody().equals(oldBody)) {
            oldBody = oldBody;
        }
        if (cleaner.isSuccessful()) {
            if (!isAbstract) {
                setBody(cleaner.getNewBody());
            }
            return null;
        } else {
            replaceBodyWithStub(generateCommonStub());
            return getDeleteTarget();
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
        return !getModifiers().contains(" bridge ");
    }

    private void replaceBodyWithStub(String stub) {        //there is a stub that'll help not to broke everything
        changeBody(stub + "#Deleted body:\n");
    }

    private void deleteBody() {
        deleteMethodDeclaration();
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

    private void deleteMethodDeclaration() {
        modifiers = '#' + getModifiers();
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
            throw new IllegalArgumentException("Cannot find the superclass call!");
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

    @Override
    public int getEndPos() {
        return end;
    }

    @Override
    public String getText() {
        return buildSignature(inputObjects) + getBody();
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

    public void setInputObjects(ArrayList<String> inputObjects) {
        this.inputObjects = inputObjects;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public String getModifiers() {
        return modifiers;
    }

    public String getSignature() {
        return signature;
    }
}
