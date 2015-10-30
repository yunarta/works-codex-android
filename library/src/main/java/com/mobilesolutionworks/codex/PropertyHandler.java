package com.mobilesolutionworks.codex;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by yunarta on 17/8/15.
 */
class PropertyHandler
{

    WeakReference<Object> target;

    private ReflectionAnnotationProcessor.MethodInfo info;

    private final int hashCode;

    PropertyHandler(Object target, ReflectionAnnotationProcessor.MethodInfo info)
    {
        this.target = new WeakReference<>(target);
        this.info = info;

        int result = target.hashCode();
        result = 31 * result + info.hashCode();

        this.hashCode = result;
    }

    public boolean isReachable()
    {
        return target.get() != null;
    }

    Object getProperty() throws InvocationTargetException
    {
        try
        {
            return info.method.invoke(target.get());
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError(e);
        }
        catch (InvocationTargetException e)
        {
            if (e.getCause() instanceof Error)
            {
                throw (Error) e.getCause();
            }

            throw e;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyHandler handler = (PropertyHandler) o;
        return target.get() != null && target.get().equals(handler.target.get()) && info.equals(handler.info);

    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }
}
