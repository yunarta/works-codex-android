package com.moblesolutionworks.codex;

import android.util.SparseArray;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by yunarta on 9/8/15.
 */
public class Codex
{
    SparseArray<Set<ActionHookHandler>> allHooks = new SparseArray<>();

    public void register(Object object)
    {
        SparseArray<Set<ActionHookHandler>> hooks = ReflectionAnnotationProcessor.findActionHooks(object);

        int length = hooks.size();
        for (int i = 0; i < length; i++)
        {
            int key = hooks.keyAt(i);
            Set<ActionHookHandler> registeredHooks = allHooks.get(key, new TreeSet<ActionHookHandler>());

            Set<ActionHookHandler> handlers = hooks.valueAt(i);
            registeredHooks.addAll(handlers);

            allHooks.put(key, registeredHooks);
        }
    }

    /**
     * Public an action to the system and allow the hook to pick it up.
     */
    public void startAction(String name, Object... args)
    {
        int key = (name + args.length).hashCode();
        Set<ActionHookHandler> handlers = allHooks.get(key);
        if (handlers == null) return;

        for (ActionHookHandler handler : handlers)
        {
            try
            {
                handler.actionHook(args);
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
        }
    }
}
