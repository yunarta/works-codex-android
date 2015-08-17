package com.moblesolutionworks.codex;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by yunarta on 17/8/15.
 */
class DefaultPropertyHandler {

    Object target;

    private ReflectionAnnotationProcessor.MethodInfo info;

    private final int hashCode;

    DefaultPropertyHandler(Object target, ReflectionAnnotationProcessor.MethodInfo info) {
        this.target = target;
        this.info = info;

        int result = target.hashCode();
        result = 31 * result + info.hashCode();

        this.hashCode = result;
    }

    Object getProperty() throws InvocationTargetException {
        try {
            return info.method.invoke(target);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultPropertyHandler handler = (DefaultPropertyHandler) o;
        return target.equals(handler.target) && info.equals(handler.info);

    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
