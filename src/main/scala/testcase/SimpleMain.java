package testcase;

/**
 * Created by matthew on 3/28/15.
 */
public class SimpleMain {

    public int qwer456asdf = 123;

    static public void main(String[] args) {
        SimpleMain s = new SimpleMain();

        s.qwer456asdf = 456;

        Main m2 = new Main();

        m2.qwer456asdf_$eq(999);


        /*try {
            synchronized (s) {
                s.wait();
            }
        } catch (InterruptedException e) {}*/


    }

}
