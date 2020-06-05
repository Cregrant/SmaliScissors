public class Threads {
    static int running = 0;
    static final Object lock = new Object();

    static class Some extends Thread{
        public void run() {
            running++;
            synchronized (lock) {

            }
            running--;
        }

    }
}
