package com.mobilesolutionworks.codex;

/**
 * Created by yunarta on 18/8/15.
 */
public @interface Action {

    String value();

    ActionParameter[] args() default {};
}
