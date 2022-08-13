package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.Prefs;
import com.github.cregrant.smaliscissors.removecode.Gzip;
import com.github.cregrant.smaliscissors.removecode.SmaliClass;
import com.github.cregrant.smaliscissors.removecode.SmaliTarget;
import com.github.cregrant.smaliscissors.removecode.method.ArgumentParser;
import com.github.cregrant.smaliscissors.removecode.method.MethodCleaner;

import java.util.ArrayList;

public class ClassMethod implements ClassPart {
    private final String ref;
    private final String name;
    private final String returnObject;
    private final String signature;
    private final boolean isStatic;
    private final boolean isAbstract;
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
        isStatic = line.contains(" static ");
        isAbstract = line.contains(" abstract ");
        name = line.substring(namePos, inputStart);
        ref = smaliClass.getRef().replace(".smali", "") + "->" + getSignature();
        returnObject = line.substring(inputEnd + 1);

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
        if (cleaner.isSuccessful()) {
            if (!isAbstract) {
                setBody(cleaner.getNewBody());
            }
            return null;
        } else {
            replaceBodyWithStub();
            return getDeleteTarget();
        }
    }

    private SmaliTarget getDeleteTarget() {
        SmaliTarget target = new SmaliTarget();
        if (canBeDeleted()) {
            target.setRef(ref);                                     //method reference
        } else {
            target.setRef(ref.substring(0, ref.indexOf("->")));     //class reference
        }
        return target;
    }

    private boolean canBeDeleted() {     //it is better to delete the class with the bridge method. Idk why too.
        return !getModifiers().contains(" bridge ");
    }

    private void replaceBodyWithStub() {        //there is a stub that'll help not to broke everything
        String stub = generateBodyStub() + "\n\n#Deleted body:\n";
        changeBody(stub);
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

    public String generateBodyStub() {
        if (returnObject.equals("V")) {
            return "\n    .locals 0\n\n" +
                    "    return-void\n" +
                    ".end method";
        } else if (returnObject.length() == 1) {
            boolean wide = returnObject.equals("J") || returnObject.equals("D");
            return "\n    .locals " + (wide ? "2" : "1") + "\n\n" +
                    "    const" + (wide ? "-wide" : "/4") + " v0, 0x0\n\n" +
                    "    return" + (wide ? "-wide" : "") + " v0\n" +
                    ".end method";
        } else {
            return "\n    .locals 1\n\n" +
                    "    new-instance v0, Ljava/lang/Object;\n\n" +
                    "    invoke-direct {v0}, Ljava/lang/Object;-><init>()V\n\n" +
                    "    check-cast v0, " + returnObject + "\n\n" +
                    "    return-object v0\n" +
                    ".end method";
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
