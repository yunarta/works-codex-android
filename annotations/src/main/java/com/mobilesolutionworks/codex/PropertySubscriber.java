package com.mobilesolutionworks.codex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An prroperty hook for receiving property change event.
 * <p>
 * Created by yunarta on 9/8/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PropertySubscriber {
    String name();
}
