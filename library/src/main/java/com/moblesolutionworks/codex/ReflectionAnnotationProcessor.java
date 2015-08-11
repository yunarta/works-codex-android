package com.moblesolutionworks.codex;


import android.util.SparseArray;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by yunarta on 9/8/15.
 */
public class ReflectionAnnotationProcessor
{
    static final SparseArray<SparseArray<Set<MethodInfo>>> allActionHooks;
    static final SparseArray<SparseArray<Set<MethodInfo>>> allPropertyHooks;
    static final SparseArray<SparseArray<MethodInfo>> allDefaultProperties;

    static
    {
        allActionHooks = new SparseArray<>();
        allPropertyHooks = new SparseArray<>();
        allDefaultProperties = new SparseArray<>();
    }

    static class MethodInfo implements Comparable<MethodInfo>
    {
        final int name;

        final int priority;

        final boolean cached;

        final Method method;

        final int argc;

        final boolean noModifier;

        int hashCode;

        public MethodInfo(ActionHook annotation, Method method)
        {
            this(annotation.name().hashCode(), annotation.priority(), false, method);
        }


        public MethodInfo(PropertyHook annotation, Method method)
        {
            this(annotation.name().hashCode(), 0, false, method);
        }

        public MethodInfo(DefaultProperty annotation, Method method)
        {
            this(annotation.name().hashCode(), 0, annotation.cached(), method);
        }

        private MethodInfo(int name, int priority, boolean cached, Method method)
        {
            this.name = name;
            this.priority = priority;
            this.cached = cached;

            this.method = method;
            this.method.setAccessible(true);

            Class<?>[] params = method.getParameterTypes();
            this.argc = params.length;

            this.noModifier = Void.TYPE.equals(method.getReturnType());

            int result = name;
            result = 31 * result + argc;
            result = 31 * result + priority;
            result = 31 * result + (cached ? 1 : 0);
            result = 31 * result + method.hashCode();

            this.hashCode = result;
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

            MethodInfo info = (MethodInfo) o;
            return name == info.name && priority == info.priority && cached == info.cached && argc == info.argc && method.equals(info.method);
        }

        @Override
        public int compareTo(MethodInfo another)
        {
            return priority < another.priority ? -1 : (priority == another.priority ? 0 : 1);
        }
    }

    public static void process(Class<?> cl)
    {
        Class<?> examined = cl;

        do
        {
            SparseArray<Set<MethodInfo>> classActionHookMethods = new SparseArray<>();
            SparseArray<Set<MethodInfo>> classPropertyHookMethods = new SparseArray<>();
            SparseArray<MethodInfo> classDefaultPropertyMethods = new SparseArray<>();

            for (Method method : examined.getDeclaredMethods())
            {
                if (method.isBridge())
                {
                    // The compiler sometimes creates synthetic bridge methods as part of the
                    // type erasure process. As of JDK8 these methods now include the same
                    // annotations as the original declarations. They should be ignored for
                    // subscribe/produce.
                    continue;
                }

                if (method.isAnnotationPresent(ActionHook.class))
                {
                    ActionHook hook = method.getAnnotation(ActionHook.class);

                    // method has to be public
                    if ((method.getModifiers() & Modifier.PUBLIC) == 0)
                    {
                        throw new IllegalArgumentException(
                                String.format(Locale.ENGLISH,
                                        "Method %s has @ActionHook annotation but is not public", method)
                        );
                    }

                    Class<?>[] params = method.getParameterTypes();

                    int name = (hook.name() + params.length).hashCode();
                    Set<MethodInfo> methods = classActionHookMethods.get(name);
                    if (methods == null)
                    {
                        methods = new HashSet<>();
                        classActionHookMethods.put(name, methods);
                    }

                    methods.add(new MethodInfo(hook, method));
                }
                else if (method.isAnnotationPresent(PropertyHook.class))
                {
                    PropertyHook hook = method.getAnnotation(PropertyHook.class);

                    Class<?>[] params = method.getParameterTypes();
                    if ((params.length != 1) || ((method.getModifiers() & Modifier.PUBLIC) == 0))
                    {
                        throw new IllegalArgumentException(
                                String.format(Locale.ENGLISH,
                                        "Method %s has @PropertyHook annotation but has too many argumets", method)
                        );
                    }

                    int name = hook.name().hashCode();
                    Set<MethodInfo> methods = classPropertyHookMethods.get(name);
                    if (methods == null)
                    {
                        methods = new HashSet<>();
                        classPropertyHookMethods.put(name, methods);
                    }

                    methods.add(new MethodInfo(hook, method));
                }
                else if (method.isAnnotationPresent(DefaultProperty.class))
                {
                    DefaultProperty hook = method.getAnnotation(DefaultProperty.class);

                    Class<?>[] params = method.getParameterTypes();
                    Class<?> returnType = method.getReturnType();

                    if ((params.length != 0) || ((method.getModifiers() & Modifier.PUBLIC) == 0) || returnType.equals(Void.TYPE))
                    {
                        throw new IllegalArgumentException(
                                String.format(Locale.ENGLISH,
                                        "Method %s has @DefaultProperty annotation but has argumets", method)
                        );
                    }

                    int name = hook.name().hashCode();
                    MethodInfo methodInfo = classDefaultPropertyMethods.get(name);
                    if (methodInfo != null)
                    {
                        throw new IllegalArgumentException(
                                String.format(Locale.ENGLISH,
                                        "Method %s has already been registered", hook.name())
                        );
                    }

                    classDefaultPropertyMethods.put(name, new MethodInfo(hook, method));
                }
            }

            allActionHooks.put(examined.hashCode(), classActionHookMethods);
            allPropertyHooks.put(examined.hashCode(), classPropertyHookMethods);
            allDefaultProperties.put(examined.hashCode(), classDefaultPropertyMethods);

            if (examined.isAnnotationPresent(InheritCodex.class))
            {
                examined = examined.getSuperclass();
            }
            else
            {
                examined = null;
            }
        }
        while (examined != null);
    }

    public static SparseArray<Set<ActionHookHandler>> findActionHooks(Object object)
    {
        Class<?> cl = object.getClass();

        SparseArray<Set<ActionHookHandler>> actionHookHandlers = new SparseArray<>();

        SparseArray<Set<MethodInfo>> actionHooks = allActionHooks.get(cl.hashCode());
        if (actionHooks == null)
        {
            process(cl);
            actionHooks = allActionHooks.get(cl.hashCode());
        }

        int length = actionHooks.size();
        if (length != 0)
        {
            for (int i = 0; i < length; i++)
            {
                int key = actionHooks.keyAt(i);

                Set<MethodInfo> methodInfos = actionHooks.valueAt(i);
                Set<ActionHookHandler> handlers = new HashSet<>();

                for (MethodInfo methodInfo : methodInfos)
                {
                    ActionHookHandler handler = new ActionHookHandler(object, methodInfo);
                    handlers.add(handler);
                }

                actionHookHandlers.put(key, handlers);
            }
        }

        return actionHookHandlers;
    }
}
