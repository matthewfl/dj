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

    AtomicLong readCount = new AtomicLong();

    AtomicLong writeTime = new AtomicLong();

    AtomicLong writeTimeSq = new AtomicLong();

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
        sb.append(rt / readCount.get());
        sb.append(" ");
        sb.append((rc * rs - rt * rt) / (rc * (rc - 1)));
        sb.append("] writes=[");
        double wc = writeCount.get();
        double wt = writeTime.get();
        double ws = writeTimeSq.get();
        sb.append(writeCount.get());
        sb.append(" ");
        sb.append(wt / writeCount.get());
        sb.append(" ");
        sb.append((wc * ws - wt * wt) / (wc * (wc - 1)));
        sb.append("])");

        return sb.toString();
    }

    public static long getTime () {
        return System.nanoTime();
    }

    public static void addReadTime(long time) {
        getLogger().readTime.addAndGet(time);
        getLogger().readTimeSq.addAndGet(time*time);
        getLogger().readCount.addAndGet(1);
    }

    public static void addWriteTime(long time) {
        getLogger().writeTime.addAndGet(time);
        getLogger().writeTimeSq.addAndGet(time*time);
        getLogger().writeCount.addAndGet(1);
    }

    public static void countRPC() {
        getLogger().rpcCount.addAndGet(1);
    }


}
