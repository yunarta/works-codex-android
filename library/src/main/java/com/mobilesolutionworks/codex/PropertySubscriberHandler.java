package com.mobilesolutionworks.codex;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by yunarta on 12/8/15.
 */
class PropertySubscriberHandler implements Comparable<PropertySubscriberHandler>
{

    final WeakReference<Object> target;

    private final ReflectionAnnotationProcessor.MethodInfo info;

    private final int hashCode;

    PropertySubscriberHandler(Object target, ReflectionAnnotationProcessor.MethodInfo info)
    {
        this.target = new WeakReference<Object>(target);
        this.info = info;

        int result = target.hashCode();
        result = 31 * result + info.hashCode();
        hashCode = result;
    }

    public boolean isReachable()
    {
        return target.get() != null;
    }

    void receiveProperty(Object arg) throws InvocationTargetException
    {
        try
        {
            info.method.invoke(target.get(), arg);
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
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertySubscriberHandler handler = (PropertySubscriberHandler) o;
        return target.get() != null && target.get().equals(handler.target.get()) && info.equals(handler.info);
    }

    @Override
    public int compareTo(PropertySubscriberHandler another)
    {
        return info.compareTo(another.info);
    }
}
