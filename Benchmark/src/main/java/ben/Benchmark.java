package ben;

import com.github.cregrant.smaliscissors.structures.common.SmaliFile;
import com.github.cregrant.smaliscissors.structures.opcodes.Const;
import com.github.cregrant.smaliscissors.structures.opcodes.Opcode;
import com.github.cregrant.smaliscissors.structures.opcodes.Return;
import com.github.cregrant.smaliscissors.structures.smali.SmaliClass;
import com.github.cregrant.smaliscissors.structures.smali.SmaliTarget;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ALL")
public class Benchmark {
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @State(Scope.Thread)
    public static class MyState {
        static String path = "C:\\BAT\\_INPUT_APK\\APK.Editor.Pro.v1.10.0.Final.Mod.Multi\\smali\\cc\\binmt\\signature\\PmsHookApplication.smali";
        static String body = ".class public Lcom/duplicatefilefixer/util/Session;\n" +
                ".super Ljava/lang/Object;\n" +
                ".source \"\"\n" +
                "\n" +
                "    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z\n" +
                "\n" +
                "    move-result v0\n" +
                "\n" +
                "    return v0\n" +
                ".end method";
        static Random random = new Random();
        static String target = "Lcom/discord/utilities/analytics/AnalyticsTracker";
        static String string = "12345";
        static String line = "    iput-object p1, p0, Lcom/discord/stores/StoreAnalytics;->hasTrackedAppUiShown:Ljava/util/concurrent/atomic/AtomicBoolean;";
        static ExecutorService executor = Executors.newFixedThreadPool(4);
        static String pattern = "(const-string [pv]\\d+)";
        static Pattern patternCompiled = Pattern.compile("(const-string [pv]\\d+)");
        static Matcher matcher = patternCompiled.matcher("");
        static float[] b = new float[1000];
        static boolean[] booleans = new boolean[1000];
        static int num = 4;
        static int num1 = 15;
        static float num2 = 0.2f;
        static float num3 = 5.223f;
        static String[] opcodes = new String[] {line};
        static ArrayList<String> list = new ArrayList<>();
        static ArrayList<String> list1 = new ArrayList<>(Arrays.asList(pattern, line, string, target));
        static ArrayList<String> list2 = new ArrayList<>();
        static ArrayList<Opcode> list3 = new ArrayList<>();
        static volatile AtomicBoolean bool = new AtomicBoolean(true);
        static Thread ne = new Thread();
        static int[] ints = new int[500];
        static float[] floats = new float[500];
        static String ret = "    return-void";
        static String go = "    const-string v0, \"clock\"";

        static {
            for (int i = 0; i < 500; i++) {
                ints[i] = random.nextInt(100);
                floats[i] = i;
                b[i] = random.nextFloat() * i;
                booleans[i] = random.nextBoolean();
                list.add(String.valueOf(i));
                if (new Random().nextFloat() < 0.02f) {
                    list2.add("    :cond_2");
                    list3.add(new Return(Arrays.asList(ret).toArray(new String[0]), 0));
                }
                else {
                    list2.add("    not cond");
                    list3.add(new Const(Arrays.asList(go).toArray(new String[0]), 0));
                }

            }
        }
    }

    @org.openjdk.jmh.annotations.Benchmark
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 60, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measure1(Blackhole blackhole, MyState state) throws Exception {
        SmaliTarget target = new SmaliTarget();
        target.setSkipPath("com/google");
        state.smaliClass.clean(target);
        blackhole.consume(state.num1);
    }

    @org.openjdk.jmh.annotations.Benchmark
    @Fork(value = 1, warmups = 0)
    @BenchmarkMode({Mode.Throughput})
    @Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 60, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void measure2(Blackhole blackhole, MyState state) throws Exception {
        SmaliTarget target = new SmaliTarget();
        target.setSkipPath("com/google");
        state.smaliClass.clean(target);
        blackhole.consume(state.num1);
    }
}
