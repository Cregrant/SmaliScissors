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

import com.googlecode.d2j.dex.Dex2jar;
import com.googlecode.d2j.reader.DexFileReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Dex2jarCmd extends BaseCmd {

    public static void main(String... args) {
        new Dex2jarCmd().doMain(args);
    }

    protected void doCommandLine() throws Exception {
        Path currentDir = new File(".").toPath();
        for (String fileName : remainingArgs) {
            String baseName = getBaseName(new File(fileName).toPath());
            Path file = currentDir.resolve(baseName + ".jar");
            DexFileReader dex = new DexFileReader(Files.readAllBytes(new File(fileName).toPath()));

            new Dex2jar(dex)
                    .withExceptionHandler(null)
                    .reUseReg(false)
                    .skipDebug()
                    .optimizeSynchronized(true)
                    .printIR(true)
                    .noCode(false)
                    .skipExceptions(true)
                    .to(file);
        }
    }
}
