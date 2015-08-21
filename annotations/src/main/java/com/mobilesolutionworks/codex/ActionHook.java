package com.mobilesolutionworks.codex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An action hook.
 * <p>
 * Which is basically no contract event listener, which hook to action dispatched in the system.
 * <p>
 * Created by yunarta on 9/8/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ActionHook {
    String name();

    int priority() default Integer.MAX_VALUE;
}
