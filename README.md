# SmaliScissors

This application is designed to apply patches from the ApkEditor android application to a prepared (disassembled) apk project.  
However, some of them will not work due to different XML file structures in different decompilers or some other problems.

### Patch support status:
* ADD_FILES          :heavy_check_mark:
* REMOVE_FILES       :heavy_check_mark:
* REMOVE_CODE        :white_check_mark:
* REMOVE_CODE_ACTION :white_check_mark:
* MATCH_REPLACE      :heavy_check_mark:
* MATCH_ASSIGN       :heavy_check_mark:
* MATCH_GOTO         :heavy_check_mark:
* GOTO               :heavy_check_mark:
* MERGE              :x:
* EXECUTE_DEX        :question:
* DUMMY              :heavy_check_mark:

More information about patches can be found on the wiki. 

# Build
Clone this repository:
```
git clone https://github.com/Cregrant/SmaliScissors.git & cd SmaliScissors
```

And build a minimalistic `patcher.jar` library to include it in your application:
```
gradlew :patcher:fatJar
```

Or build a `console_patcher.jar` file to run patches from the command line:
```
gradlew :console_launcher:fatJar
```

# Usage
Print usage (use Tab to find a jar file):
```
java -jar console_patcher-<version>.jar
```

You can use interactive selection:
```
java -jar console_patcher-<version>.jar -i C:\projects_root -p C:\patches_folder -s
```

Or use predefined values:
```
java -jar console_patcher-<version>.jar -i C:\projects_root\decompiled_project_folder -p "C:\patches_folder\your best patch.zip"
```
SmaliScissors prints its logs using The Simple Logging Facade for Java (SLF4J).

## License
SmaliScissors source code is released under the Apache License 2.0.
