package testcase;

import java.util.ArrayList;

/**
 * Created by matthewfl
 */
public class SimpleConvertBridge {

    public static void main(String[] args) {
        // test if we can convert back and forth between two different classes

        /*Object o = ConvertBridge.getConverter().makeNative(new int[5]);

        Unsafe u = Unsafe.getUnsafe();

        assert(u != null);

        AtomicInteger i = new AtomicInteger(0);

        i.getAndAdd(1);

        assert(i.get() == 1);

        InternalInterface.getInternalInterface().simplePrint("simple print test");

        Object nu = ConvertBridge.getConverter().makeNative(u);

        u = (Unsafe)ConvertBridge.getConverter().makeDJ(nu);

        assert(u != null);

        //System.out.println("system print");
*/

        /*
        ArrayList<Integer> ii = new ArrayList<>();

        Object oi = ConvertBridge.getConverter().makeNative(ii);

        assert(oi.getClass().getName().equals("java.util.ArrayList"));
*/

        System.out.println("-----------------------------------------------------------------------test------------------------------------------------");

        ArrayList<Integer> ii = new ArrayList<>();

        ii.add(123);

        System.out.println(ii.get(0));

    }

}
