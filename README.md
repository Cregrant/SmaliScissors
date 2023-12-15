# SmaliScissors

SmaliScissors is an application designed to apply patches from the ApkEditor android application to a prepared (
disassembled) apk project.  
However, please note that certain patches may not work as expected due to variations in XML file structure across
different decompilers or other potential issues.

### Patch support status:

- **ADD_FILES**          :heavy_check_mark:
- **REMOVE_FILES**       :heavy_check_mark:
- **REMOVE_CODE**        :white_check_mark: (New)
- **REMOVE_CODE_ACTION** :white_check_mark: (New)
- **MATCH_REPLACE**      :heavy_check_mark:
- **MATCH_ASSIGN**       :heavy_check_mark:
- **MATCH_GOTO**         :heavy_check_mark:
- **GOTO**               :heavy_check_mark:
- **MERGE**              :x: (Not supported)
- **EXECUTE_DEX**        :question: (Supported with limitations)
- **DUMMY**              :heavy_check_mark:

For more detailed information about patches, please refer to the [wiki](https://github.com/Cregrant/SmaliScissors/wiki).

# Build

To get started, clone this repository:

```bash
git clone https://github.com/Cregrant/SmaliScissors.git && cd SmaliScissors
```

Then, build the minimalistic `patcher.jar` library for inclusion in your application:

```bash
gradlew :patcher:fatJar
```

For more details on integrating SmaliScissors with your Gradle project and configuring logback (for SLF4J), refer to the
[Example lib usage wiki](https://github.com/Cregrant/SmaliScissors/wiki/Example_lib_usage.md).

---

Alternatively, build a `console_patcher.jar` file to execute patches from the command line:

```bash
gradlew :console_launcher:fatJar
```

# Usage

To print the usage information (use Tab to find a JAR file):

```bash
java -jar console_patcher-<version>.jar
```

For interactive selection, use:

```bash
java -jar console_patcher-<version>.jar -i C:\projects_root -p C:\patches_folder -s
```

Or use predefined values:

```bash
java -jar console_patcher-<version>.jar -i C:\projects_root\decompiled_project_folder -p "C:\patches_folder\your best patch.zip"
```

SmaliScissors logs information using The Simple Logging Facade for Java (SLF4J).

## License

The source code for SmaliScissors is released under the Apache License 2.0.