package com.moblesolutionworks.codex;

import android.support.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by yunarta on 12/8/15.
 */
class PropertySubscriberHandler implements Comparable<PropertySubscriberHandler> {

    private final Object target;

    private final ReflectionAnnotationProcessor.MethodInfo info;

    private final int hashCode;

    PropertySubscriberHandler(Object target, ReflectionAnnotationProcessor.MethodInfo info) {
        this.target = target;
        this.info = info;

        int result = target.hashCode();
        result = 31 * result + info.hashCode();
        hashCode = result;
    }

    void receiveProperty(Object arg) throws InvocationTargetException {
        try {
            info.method.invoke(target, arg);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }

            throw e;
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertySubscriberHandler handler = (PropertySubscriberHandler) o;
        return target.equals(handler.target) && info.equals(handler.info);
    }

    @Override
    public int compareTo(@NonNull PropertySubscriberHandler another) {
        return info.compareTo(another.info);
    }
}
