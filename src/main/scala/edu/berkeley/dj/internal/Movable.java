package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * This class represents classes that can be moved between different hosts
 * This will be automatically added to classes that don't have it accordinly
 */
public interface Movable {

    void __dj_seralize_obj(SeralizeManager manager);

}
