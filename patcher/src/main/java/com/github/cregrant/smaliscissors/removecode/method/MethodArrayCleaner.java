package com.github.cregrant.smaliscissors.removecode.method;

import com.github.cregrant.smaliscissors.removecode.method.opcode.*;

import java.util.ArrayList;
import java.util.HashMap;

public class MethodArrayCleaner {
    MethodOpcodeCleaner cleaner;
    HashMap<String, ArrayCluster> tracked = new HashMap<>();

    MethodArrayCleaner(MethodOpcodeCleaner cleaner) {
        this.cleaner = cleaner;
    }

    void track(Opcode op) {
        tracked.remove(op.getOutputRegister());     //if array is overwritten
        if (op instanceof Move) {
            modifyClusterRegister((Move) op);
            return;
        }
        if (!(op instanceof Array)) {
            return;
        }

        if (op instanceof NewArray) {
            trackNewCluster((NewArray) op);
        } else if (op instanceof ArrayPut || op instanceof ArrayGet) {
            trackExistCluster(op);
        }
    }

    String delete(Opcode op) {
        if (op instanceof ArrayPut || op instanceof ArrayGet) {    //remove array refs
            Array arrayOp = ((Array) op);
            ArrayCluster arrayCluster = tracked.get(arrayOp.getArrayRegister());
            if (arrayCluster == null) {
                throw new IllegalArgumentException("NewArray not found?");
            }
            arrayCluster.delete();
            return arrayOp.getArrayRegister();
        }
        return null;
    }

    void trackNewCluster(NewArray newArray) {
        ArrayCluster arrayCluster = new ArrayCluster(newArray);
        tracked.put(newArray.getOutputRegister(), arrayCluster);
    }

    void trackExistCluster(Opcode op) {
        Array arrayOp = ((Array) op);
        String register = arrayOp.getArrayRegister();
        ArrayCluster arrayCluster = tracked.get(register);
        if (arrayCluster == null) {
            NewArray array = cleaner.searchNewArray(register);      //happens when stack points between array creation and usage
            if (array == null) {
                throw new IllegalArgumentException("NewArray not found?");      //is new-array inside the method arguments?
            } else {
                ArrayCluster newCluster = new ArrayCluster(array);
                tracked.put(register, newCluster);
                trackExistCluster(op);
            }
        } else {
            arrayCluster.addUsage(arrayOp);
        }
    }

    void modifyClusterRegister(Move op) {
        ArrayCluster arrayCluster = tracked.remove(op.getInputRegisters().get(0));
        if (arrayCluster == null) {
            return;
        }
        tracked.put(op.getOutputRegister(), arrayCluster);
    }

    static class ArrayCluster {
        NewArray head;
        ArrayList<Opcode> usages = new ArrayList<>();

        ArrayCluster(NewArray newArray) {
            head = newArray;
        }

        void addUsage(Opcode op) {
            usages.add(op);
        }

        public void delete() {
            head.deleteLine();
            for (Opcode op : usages) {
                op.deleteLine();
            }
        }
    }
}
