package testcase;

import edu.berkeley.dj.internal.DJIO;
import edu.berkeley.dj.internal.DJIOTargetMachineArgPosition;

/**
 * Created by matthewfl
 */
@DJIO
public final class SimpleIOTarget {

    private String fname;

    @DJIOTargetMachineArgPosition(1)
    public SimpleIOTarget(int target, String fname) {
        this.fname = fname;
    }

    public String getContent() {
        return "test";
    }


}
