package testcase;

import java.util.Iterator;

/**
 * Created by matthew
 */
public class basic {

    final StringBuffer buff;

    public basic() {
        buff = new StringBuffer();

        buff.append("this isn't that bad");
    }

    @Override
    public String toString() { return buff.toString(); }

    public Iterator<String> iter() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                return buff.toString();
            }

        };
    }
}
