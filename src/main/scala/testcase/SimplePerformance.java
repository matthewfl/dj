package testcase;

/**
 * Created by matthewfl
 */
public class SimplePerformance {

    private int a = 0;

    private int getA() { return a; }

    private void setA(int v) { a = v; }

    // mostly read and write into memory, can be about 25x slower
    // similar after warming up
    // the entire slow down appears to be the result of the write operation
    // when only intercepting read operations the difference in speed appears to be
    // within measurement error
    void test1() {
        for(int i = 0; i < 10; i++) {
            a = 0;
            while (a < Integer.MAX_VALUE - 1) {
                a++;
            }
        }
    }

    // using local variables mostly, 6x slower without warm up
    // after warmup 2x slower
    void test2() {
        for(a = 0; a < 100; a++) {
            for(int i = 0; i < Integer.MAX_VALUE - 1; i++) {}
        }
    }

    // all local variables 3x slower without warm up
    // within margin of error after warmmed up
    void test3() {
        for(int i = 0; i < 100; i++) {
            for(int j = 0; j < Integer.MAX_VALUE - 1; j++) {}
        }
    }


    void test4() {
        for(int i = 0; i < 10; i++) {
            setA(0);
            while(getA() < Integer.MAX_VALUE - 1) {
                setA(getA() + 1);
            }
        }
    }


    public static void main(String[] args) {

        SimplePerformance g = new SimplePerformance();

        // warm it up, make sure that we don't have an issue with the loading time
        long s1 = System.nanoTime();
        g.test4();
        long s2 = System.nanoTime();

        long startTime = System.nanoTime();
        g.test4();
        long endTime = System.nanoTime();

        System.out.println("This took: "+(endTime - startTime));
    }

}
