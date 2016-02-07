package edu.berkeley.dj.internal;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matthewfl
 */
public class InternalLogger {

    static final InternalLogger logger = new InternalLogger();

    public static InternalLogger getLogger() { return logger; }

    AtomicLong readTime = new AtomicLong();

    AtomicLong readTimeSq = new AtomicLong();

    long readTimeMax = 0;

    AtomicLong readCount = new AtomicLong();

    AtomicLong writeTime = new AtomicLong();

    AtomicLong writeTimeSq = new AtomicLong();

    long writeTimeMax = 0;

    AtomicLong writeCount = new AtomicLong();

    AtomicLong rpcCount = new AtomicLong();


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("InternalLogger(");
        sb.append("reads=[");
        double rc = readCount.get();
        double rt = readTime.get();
        double rs = readTimeSq.get();
        sb.append(readCount.get());
        sb.append(" ");
        sb.append(rt / readCount.get() / 1000);
        sb.append(" ");
        sb.append(Math.sqrt((rc * rs - rt * rt) / (rc * (rc - 1))) / 1000);
        sb.append(" ");
        sb.append(((double)readTimeMax) / 1000);
        sb.append("] writes=[");
        double wc = writeCount.get();
        double wt = writeTime.get();
        double ws = writeTimeSq.get();
        sb.append(writeCount.get());
        sb.append(" ");
        sb.append(wt / writeCount.get() / 1000);
        sb.append(" ");
        sb.append(Math.sqrt((wc * ws - wt * wt) / (wc * (wc - 1))) / 1000);
        sb.append(" ");
        sb.append(((double)writeTimeMax) / 1000);
        sb.append("])");

        return sb.toString();
    }

    public static long getTime () {
        // return micro seconds
        return System.nanoTime() / 1000;
    }

    public static void addReadTime(long time) {
        getLogger().readTime.addAndGet(time);
        getLogger().readTimeSq.addAndGet(time*time);
        getLogger().readCount.addAndGet(1);
        if(time > getLogger().readTimeMax) {
            getLogger().readTimeMax = time;
        }
    }

    public static void addWriteTime(long time) {
        getLogger().writeTime.addAndGet(time);
        getLogger().writeTimeSq.addAndGet(time*time);
        getLogger().writeCount.addAndGet(1);
        if(time > getLogger().writeTimeMax) {
            getLogger().writeTimeMax = time;
        }
    }

    public static void countRPC() {
        getLogger().rpcCount.addAndGet(1);
    }


}
