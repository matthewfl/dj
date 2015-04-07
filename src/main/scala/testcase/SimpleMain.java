package testcase;

/**
 * Created by matthew on 3/28/15.
 */
public class SimpleMain {

    public int qwer456asdf = 123;

    public int[] arrs = new int[10];

    static public void main(String[] args) {
        SimpleMain s = new SimpleMain();

        s.arrs[2] = 5;

        s.qwer456asdf = 456;

        Main m2 = new Main();

        m2.qwer456asdf_$eq(999);

        basic bb = new basic();

        bb.iter();

        System.out.println(bb);


        /*try {
            synchronized (s) {
                s.wait();
            }
        } catch (InterruptedException e) {}*/


    }

}
