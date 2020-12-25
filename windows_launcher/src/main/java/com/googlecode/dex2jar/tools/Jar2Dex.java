/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2012 Panxiaobo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.dex2jar.tools;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Jar2Dex extends BaseCmd {
    public static void main(String... args) {
        new Jar2Dex().doMain(args);
    }

    @Override
    protected void doCommandLine() throws Exception {
        Path jar = new File(remainingArgs[0]).toPath();
        Path outDex = new File(remainingArgs[0].replace(".jar", ".dex")).toPath();

        Class<?> c = Class.forName("com.android.dx.command.Main");
        Method m = c.getMethod("main", String[].class);
        List<String> ps = new ArrayList<>(Arrays.asList("--dex", "--no-strict", "--output=" + outDex.toAbsolutePath().toString(), jar.toAbsolutePath().toString()));
        System.out.println("call com.android.dx.command.Main.main" + ps);
        m.invoke(null, new Object[] { ps.toArray(new String[0]) });
    }
}
