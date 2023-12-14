package com.github.cregrant.smaliscissors.removecode.classparts;

import com.github.cregrant.smaliscissors.Flags;
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
    private String ref;
    private final String name;
    private final String returnObject;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final boolean isConstructor;
    private boolean deleted = false;
    private boolean stubbed = false;
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
            stubbed = true;
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
        ref = generateRef(smaliClass, namePos);
        returnObject = line.substring(inputEnd + 1);

        if (inputEnd - inputStart > 1) {
            inputObjects = new ArgumentParser().parse(line);
        }
    }

    private String generateRef(SmaliClass smaliClass, int namePos) {
        return smaliClass.getRef().replace(".smali", "") + "->" + line.substring(namePos);
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
        if ((!inParameters && (stubbed || deleted)) || (line.startsWith("#")) || (!inParameters && !target.containsInside(getBody()))) {
            return null;
        }
        MethodCleaner cleaner = new MethodCleaner(this, target.getRef());
        HashSet<SmaliTarget> fieldsCanBeNull = cleaner.getFieldsCanBeNull();

        if (returnObject.contains(target.getRef())) {
            delete();
            cleaner.fillFieldsCanBeNull();
            return new SmaliCleanResult(getDeleteTarget(), fieldsCanBeNull);
        }

        cleaner.clean();
        if (cleaner.isSuccessful()) {
            if (!isAbstract) {
                setBody(cleaner.getNewBody());
            }
            return new SmaliCleanResult(fieldsCanBeNull);
        } else if (inParameters && !Flags.SMALI_ALLOW_METHOD_ARGUMENTS_CLEANUP) {
            cleaner.fillFieldsCanBeNull();
            return new SmaliCleanResult(new SmaliTarget().setRef(smaliClass.getRef()), fieldsCanBeNull);
        } else {
            makeStub(smaliClass);
            cleaner.fillFieldsCanBeNull();
            return new SmaliCleanResult(getDeleteTarget(), fieldsCanBeNull);
        }
    }

    @Override
    public void makeStub(SmaliClass smaliClass) {
        if (stubbed || deleted || isAbstract) {
            return;
        }
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
        return new SmaliTarget().setRef(canBeDeleted() ? ref : smaliClass.getRef());
    }

    private boolean canBeDeleted() {     //it is better to delete the class with the bridge method. Idk why too.
        return !line.contains(" bridge ");
    }

    private void replaceBodyWithStub(String stub) {        //there is a stub that'll help not to broke everything
        if (stubbed || deleted) {
            return;
        }
        stubbed = true;
        changeBody(stub + "#Deleted body:\n");
    }

    public void delete() {
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
            if (!Flags.SMALI_USE_CAST_FOR_STUB) {
                return "\n    .locals 1\n\n" +
                        "    const/4 v0, 0x0\n\n" +
                        "    return-object v0\n" +
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
    }

    public String generateConstructorStub() {
        boolean isEmptyConstructorForManifest = false;
        String superclass = smaliClass.getSuperclass();
        if (superclass.contains("Activity") || superclass.contains("Service") || superclass.contains("Receiver") || superclass.contains("Provider")) {
            isEmptyConstructorForManifest = true;
            if (!inputObjects.isEmpty() && smaliClass.containsMethodReference(smaliClass.getRef() + "-><init>()V", false)) {
                delete();
                return "";      //mandatory empty constructor already exists
            }
        }

        boolean returnObjectInit = isEmptyConstructorForManifest;
        boolean callUsesOnlyMethodArgs = false;
        String call = "";
        String body = getBody();
        if (!isEmptyConstructorForManifest) {
            int start = body.indexOf("    invoke-direct");
            int parametersStart = start + 19;
            int parametersEnd = body.indexOf("}", parametersStart);
            int possibleLocalParamPos = body.indexOf("v", parametersStart);
            callUsesOnlyMethodArgs = possibleLocalParamPos < parametersStart || possibleLocalParamPos > parametersEnd;
            int end = body.indexOf("\n", start);
            if (start == -1 || end == -1) {
                throw new InputMismatchException("Cannot find the superclass call!");
            }
            call = body.substring(start, end + 1);
        }
        if (returnObjectInit || call.contains("{p0}") || smaliClass.hasObjectSuperclass()) {
            return "\n    .locals 0\n\n" +
                    "    invoke-direct {p0}, " + smaliClass.getSuperclass() + "-><init>()V\n\n" +
                    "    return-void\n" +
                    ".end method\n\n";
        } else if (callUsesOnlyMethodArgs) {
            return "\n    .locals 0\n\n" +
                    call +
                    "    return-void\n" +
                    ".end method\n\n";

        } else if (!body.contains("new-instance") && !body.contains("sput") && !body.contains("sget")) {
            return body;
        } else {
            return "\n    .locals 0\n\n" +
                    "    return-void\n" +
                    ".end method\n\n";
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
        ref = generateRef(smaliClass, line.lastIndexOf(' ') + 1);
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

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isStubbed() {
        return stubbed;
    }

    public String getLine() {
        return line;
    }

    public String getReturnObject() {
        return returnObject;
    }

    public SmaliClass getSmaliClass() {
        return smaliClass;
    }
}
