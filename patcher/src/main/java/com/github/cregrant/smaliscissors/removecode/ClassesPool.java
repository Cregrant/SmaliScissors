package com.github.cregrant.smaliscissors.removecode;

import com.github.cregrant.smaliscissors.Project;
import com.github.cregrant.smaliscissors.common.decompiledfiles.SmaliFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

class ClassesPool {

    private static final Logger logger = LoggerFactory.getLogger(ClassesPool.class);
    private Map.Entry<String, ArrayList<SmaliFile>>[] array;
    private final Project project;

    public ClassesPool(Project project) {
        this.project = project;
        fillArray();
    }

    private void fillArray() {
        Map<String, Set<SmaliFile>> concurrentMap = scan();
        HashMap<String, ArrayList<SmaliFile>> map = new HashMap<>(concurrentMap.size());
        for (Map.Entry<String, Set<SmaliFile>> entry : concurrentMap.entrySet()) {
            ArrayList<SmaliFile> list = new ArrayList<>(entry.getValue());
            map.put(entry.getKey(), list);
        }
        array = (Map.Entry<String, ArrayList<SmaliFile>>[]) map.entrySet().toArray(new Map.Entry[0]);
    }

    private Map<String, Set<SmaliFile>> scan() {
        List<SmaliFile> smaliFiles = project.getSmaliList();
        final Map<String, Set<SmaliFile>> cMap = new ConcurrentHashMap<>(smaliFiles.size());
        for (SmaliFile file : smaliFiles) {
            String path = removePathObfuscation(file.getPath());
            int start = path.indexOf('/') + 1;
            String ref = "L" + path.substring(start, path.length() - 6) + ";";
            cMap.put(ref, Collections.synchronizedSet(new HashSet<SmaliFile>()));
        }

        ArrayList<Future<?>> futures = new ArrayList<>(smaliFiles.size());
        for (final SmaliFile file : smaliFiles) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    LinkedList<String> classRefs = extractClassRefs(file);
                    for (String ref : classRefs) {
                        Set<SmaliFile> list = cMap.get(ref);
                        if (list != null) {
                            list.add(file);
                        }
                    }
                }
            };
            futures.add(project.getExecutor().submit(r));
        }
        project.getExecutor().waitForFinish(futures);
        return cMap;
    }

    private LinkedList<String> extractClassRefs(SmaliFile file) {
        String body = file.getBody();
        LinkedList<String> strings = new LinkedList<>();
        int pos = 0;
        do {
            pos = body.indexOf('L', pos);
            if (pos == -1) {
                break;
            }

            int endPos = body.indexOf(';', pos);
            if (endPos == -1) {
                break;
            }

            if (body.charAt(body.lastIndexOf('\n', pos) + 1) != '#') {   //line is not commented out
                int nextLinePos = body.indexOf('\n', pos);
                if (nextLinePos < endPos) {         //not a ref
                    pos = nextLinePos;
                    continue;
                }
                String ref = body.substring(pos, endPos + 1);
                if (!ref.startsWith("Ljava") && !ref.startsWith("Ldalvik")) {     //skip internal classes
                    strings.add(ref);
                }
            }
            pos = endPos;
        } while (true);

        return strings;
    }

    private String removePathObfuscation(String path) {     //abc.1.smali -> abc.smali
        int dotPos = path.indexOf('.');
        if (dotPos == path.length() - 6) {
            return path;
        }
        return path.substring(0, dotPos + 1) + "smali";
    }

    Map.Entry<String, ArrayList<SmaliFile>>[] getArray() {
        return array;
    }
}