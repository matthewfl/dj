package testcase;

import edu.berkeley.dj.internal.DJIO;
import edu.berkeley.dj.internal.DJIOTargetMachineArgPosition;

/**
 * Created by matthewfl
 */
@DJIO
public final class SimpleIOTarget {

    private String fname;

    private int q;

    @DJIOTargetMachineArgPosition(1)
    public SimpleIOTarget(int target, int q) { //String fname) {
        //this.fname = fname;
    this.q = q;
    }

    public String getContent() {
        return "test";
    }

    public int getInt() { return 123; }

}
