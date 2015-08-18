package com.mobilesolutionworks.codex;

/**
 * Created by yunarta on 18/8/15.
 */
public @interface Action {

    String name();

    String[] args1() default {};

    Class[] argv1() default {};

    ActionParameter[] args() default {};
}
