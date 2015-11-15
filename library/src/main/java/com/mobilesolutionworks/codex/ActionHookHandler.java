package com.mobilesolutionworks.codex;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by yunarta on 12/8/15.
 */
class ActionHookHandler implements Comparable<ActionHookHandler>
{

    final WeakReference<Object> target;

    final ReflectionAnnotationProcessor.MethodInfo info;

    private final int hashCode;

    ActionHookHandler(Object target, ReflectionAnnotationProcessor.MethodInfo info)
    {
        this.target = new WeakReference<>(target);
        this.info = info;

        int result = target.hashCode();
        result = 31 * result + info.hashCode();
        hashCode = result;
    }

    public boolean isReachable()
    {
        return target.get() != null;
    }

    int actionHook(Object... args) throws InvocationTargetException
    {
        try
        {
            if (info.noModifier)
            {
                info.method.invoke(target.get(), args);
                return 0;
            }
            else
            {
                return (int) info.method.invoke(target.get(), args);
            }

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

        ActionHookHandler handler = (ActionHookHandler) o;
        return target.get() != null && target.get().equals(handler.target.get()) && info.equals(handler.info);
    }

    @Override
    public int compareTo(ActionHookHandler another)
    {
        return info.compareTo(another.info);
    }
}
