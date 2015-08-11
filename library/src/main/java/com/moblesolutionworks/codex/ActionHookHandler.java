package com.moblesolutionworks.codex;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by yunarta on 12/8/15.
 */
public class ActionHookHandler implements Comparable<ActionHookHandler>
{
    private final Object target;

    private final ReflectionAnnotationProcessor.MethodInfo info;

    private final int hashCode;

    ActionHookHandler(Object target, ReflectionAnnotationProcessor.MethodInfo info)
    {
        this.target = target;
        this.info = info;

        int result = target.hashCode();
        result = 31 * result + info.hashCode();
        hashCode = result;
    }

    int actionHook(Object... args) throws InvocationTargetException
    {
        try
        {
            if (info.noModifier)
            {
                info.method.invoke(target, args);
                return 0;
            }
            else
            {
                return (int) info.method.invoke(target, args);
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
        return target.equals(handler.target) && info.equals(handler.info);
    }

    @Override
    public int compareTo(ActionHookHandler another)
    {
        return info.compareTo(another.info);
    }
}
