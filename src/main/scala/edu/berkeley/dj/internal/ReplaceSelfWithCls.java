package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public @interface ReplaceSelfWithCls {

    String name() default "";

    Class<?> cls() default Void.class;

}
