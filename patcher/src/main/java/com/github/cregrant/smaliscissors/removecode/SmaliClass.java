package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassHeader;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassPart;
import com.github.cregrant.smaliscissors.removecode.method.MethodParser;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.Blank;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.Invoke;
import com.github.cregrant.smaliscissors.removecode.method.opcodes.Opcode;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;

public class SmaliClass {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SmaliClass.class);
    private final Project project;
    private final SmaliFile file;
    private final String ref;
    private final ArrayList<ClassPart> parts;

    public SmaliClass(Project project, SmaliFile df, String body) {
        this.project = project;
        file = df;
        df.setSmaliClass(this);
        String temp = SmaliTarget.removePathObfuscation(df.getPath());
        String shortPath = temp.substring(temp.indexOf('/') + 1, temp.lastIndexOf(".smali"));
        ref = 'L' + shortPath + ';';
        parts = new ClassParser(this, body).parseParts();
    }

    public SmaliCleanResult clean(SmaliTarget target) {
        SmaliCleanResult result = new SmaliCleanResult();
        for (ClassPart part : parts) {
            result.merge(part.clean(target, this));
        }
        return result;
    }

    public void makeStub() {
        if (getHeader().getText().contains(ClassHeader.STUB)) {
            return;
        }
        for (ClassPart part : parts) {
            part.makeStub(this);
        }
    }

    public String getNewBody() {
        StringBuilder builder = new StringBuilder();
        for (ClassPart part : parts) {
            builder.append(part.getText());
        }
        return builder.toString();
    }

    public ArrayList<ClassPart> getBodyParts() {
        return parts;
    }

    public boolean changeSuperclassOk(String deletedSuperclassRef) {
        boolean result = false;
        for (ClassPart part : parts) {
            if (part instanceof ClassMethod) {
                ClassMethod method = ((ClassMethod) part);
                if (method.isConstructor()) {
                    if (method.getLine().contains(" synthetic ")) {
                        return false;
                    }
                    fixConstructor(method, deletedSuperclassRef);     //fix for a deleted superclass
                    result = true;
                }
            }
        }
        if (!result && !isAbstract()) {
            logger.warn("<init> method not found inside the " + ref + " class!");
        }
        return result;   //just delete that strange class without constructors
    }

    private void fixConstructor(ClassMethod classMethod, String deletedSuperclassRef) {
        if (classMethod.isAbstract() || classMethod.isDeleted() || classMethod.isStubbed()) {
            return;
        }
        String curBody = classMethod.getBody();
        int pos = curBody.indexOf(deletedSuperclassRef + "-><init>(");
        if (pos == -1) {
            String newBody = tryFixComplexConstructor(classMethod);   //todo probably useless because app already broken
            if (newBody == null) {
                throw new IllegalStateException("Can't delete " + ref + " superclass. Too complex?");
            } else {
                classMethod.setBody(newBody);
            }
        }
        int start = curBody.lastIndexOf('\n', pos) + 1;
        int end = curBody.indexOf('\n', pos);
        classMethod.setBody(
                curBody.substring(0, start) +
                        '#' + curBody.substring(start, end) +
                        "\n\n    invoke-direct {p0}, Ljava/lang/Object;-><init>()V" +
                        curBody.substring(end));
    }

    private String tryFixComplexConstructor(ClassMethod method) {
        ArrayList<Opcode> opcodes = new MethodParser(method, "#stub").parse();
        String register = "p0";
        for (int i = 0; i < opcodes.size(); i++) {
            Opcode op = opcodes.get(i);
            if (op.isDeleted()) {
                continue;
            }

            String outputRegister = op.getOutputRegister();
            if (op.inputRegisterUsed(register)) {

                if (op instanceof Invoke) {
                    Invoke invoke = (Invoke) op;
                    if (invoke.isConstructor()) {
                        opcodes.set(i, new Invoke("    invoke-direct {p0}, Ljava/lang/Object;-><init>()V", "noTarget"));
                        StringBuilder sb = new StringBuilder();
                        for (Opcode opcode : opcodes) {
                            sb.append(opcode.toString());
                            if (!(opcode instanceof Blank)) {
                                sb.append('\n');
                            }
                        }
                        return sb.toString();
                    }
                } else if (!outputRegister.isEmpty() && !outputRegister.equals(register)) {
                    register = outputRegister;
                }
            }
        }
        return null;
    }

    public boolean containsMethodReference(String reference, boolean isStatic) {
        for (ClassPart part : parts) {
            if (part instanceof ClassMethod) {
                ClassMethod method = ((ClassMethod) part);
                if (method.isStatic() == isStatic && method.getRef().equals(reference)) {
                    return true;
                }
            }
        }
        return false;

    }

    public String getSuperclass() {
        return getHeader().getSuperclass();
    }

    public boolean hasObjectSuperclass() {
        return getSuperclass().equals(ClassHeader.OBJECT_REF);
    }

    public ClassHeader getHeader() {
        return (ClassHeader) parts.get(0);
    }

    public boolean isAbstract() {
        return getHeader().isAbstract();
    }

    public boolean isPathValid() {
        return getHeader().getText().contains(ref);    //header do not contain valid path? Welcome to apktool.
    }

    public String getRef() {
        return ref;
    }

    public SmaliFile getFile() {
        return file;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public String toString() {
        return ref;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SmaliClass that = (SmaliClass) o;
        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}
