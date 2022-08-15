package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Main;
import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassMethod;
import com.github.cregrant.smaliscissors.removecode.classparts.ClassPart;
import com.github.cregrant.smaliscissors.removecode.method.MethodParser;
import com.github.cregrant.smaliscissors.removecode.method.opcode.Blank;
import com.github.cregrant.smaliscissors.removecode.method.opcode.Invoke;
import com.github.cregrant.smaliscissors.removecode.method.opcode.Opcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SmaliClass {
    private final SmaliFile file;
    private final String ref;
    private final ArrayList<ClassPart> parts;
    private boolean keepClass;      //keep activities, services anf other AndroidManifest.xml things
    private String newBody;

    public SmaliClass(Project project, SmaliFile df, String body) {
        file = df;
        String temp = df.getPath();
        String shortPath = temp.substring(temp.indexOf('/') + 1, temp.lastIndexOf(".smali"));
        ref = 'L' + shortPath + ';';
        parts = new ClassParser(this, body).parseParts();
        if (project.getProtectedClasses().contains(shortPath)) {
            keepClass = true;
        }
        if (!parts.get(0).getText().contains(shortPath)) {  //header do not contain valid path? Welcome to apktool.
            throw new IllegalArgumentException("Class header and filesystem path mismatch!");
        }
    }

    public List<SmaliTarget> clean(SmaliTarget target) {
        ArrayList<SmaliTarget> dependencies = new ArrayList<>(3);
        StringBuilder builder = new StringBuilder();
        for (ClassPart part : parts) {
            SmaliTarget dependency = part.clean(target, this);
            builder.append(part.getText());
            if (dependency != null) {
                dependencies.add(dependency);
            }
        }
        newBody = builder.toString();
        if (keepClass) {
            dependencies.clear();
            SmaliTarget dep = new SmaliTarget();
            dep.setRef(ref);
            dep.denyDeletion();
            dependencies.add(dep);
        }
        return dependencies;
    }

    public String getNewBody() {
        return newBody;
    }

    public boolean deleteSuperclass(String classRef) {
        for (ClassPart part : parts) {
            if (part instanceof ClassMethod) {
                ClassMethod method = ((ClassMethod) part);
                if (method.getName().equals("<init>")) {
                    if (method.getModifiers().contains(" synthetic ")) {
                        return false;
                    }
                    fixConstructor(method, classRef);     //fix for a deleted superclass
                    return true;
                }
            }
        }
        Main.out.println("Warning: <init> method not found inside the " + ref + " class!");
        return false;   //just delete that strange class
    }

    private void fixConstructor(ClassMethod classMethod, String deletedSuperClass) {
        String curBody = classMethod.getBody();
        int pos = curBody.indexOf(deletedSuperClass + "-><init>(");
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

    public String getRef() {
        return ref;
    }

    public SmaliFile getFile() {
        return file;
    }

    public boolean isKeepClass() {
        return keepClass;
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
