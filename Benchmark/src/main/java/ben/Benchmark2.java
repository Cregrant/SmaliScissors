package ben;

import com.github.cregrant.smaliscissors.Gzip;
import com.github.cregrant.smaliscissors.utils.IO;

public class Benchmark2 {
    public static void main(String[] args) throws Exception {
        new Gzip("    invoke-static {}, Lre/sova/five/mods/pushes/ааaaаaaа;->ааaaаaaa()Ljava/lang/String;");
        String path = "E:\\fs_config.txt";
        String body = IO.read("C:\\BAT\\fs_config.txt");
        IO.copy("C:\\BAT\\Discord_112.12_.apk", "C:\\BAT\\Discord_112.12_2222222.apk");
        int cycle = 0;
        long lastTime = System.currentTimeMillis();
        while (true) {
            IO.write(path, body);
            cycle++;
            long time = System.currentTimeMillis();
            if (time - lastTime > 1000) {
                lastTime = time;
                System.out.print(cycle + "\r");
                cycle = 0;
            }
        }
    }
}
