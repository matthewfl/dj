package testcase;

/**
 * Created by matthewfl
 */
public class SimpleIOMain {

    static public void main(String[] args) {

        SimpleIOTarget t = new SimpleIOTarget(1, "/tmp/test");

        System.out.println(t.getContent());

    }
}
