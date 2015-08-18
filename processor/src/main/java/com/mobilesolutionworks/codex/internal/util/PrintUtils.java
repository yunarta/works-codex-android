package com.mobilesolutionworks.codex.internal.util;

import java.util.Collection;

/**
 * Created by yunarta on 19/8/15.
 */
public class PrintUtils {

    public static String concat(String separator, Collection args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append(separator).append(arg);
        }

        if (sb.length() > 0) {
            sb.delete(0, separator.length());
        }

        return sb.toString();
    }
}
