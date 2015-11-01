package com.mobilesolutionworks.codex;


import android.util.SparseArray;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by yunarta on 9/8/15.
 */
public class ReflectionAnnotationProcessor
{
    static final SparseArray<SparseArray<List<MethodInfo>>> allActionHooks;
    static final SparseArray<SparseArray<List<MethodInfo>>> allPropertySubscribers;
    static final SparseArray<SparseArray<MethodInfo>>       allDefaultProperties;

    static
    {
        allActionHooks = new SparseArray<>();
        allPropertySubscribers = new SparseArray<>();
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
            this(annotation.value().hashCode(), annotation.priority(), false, method);
        }


        public MethodInfo(PropertySubscriber annotation, Method method)
        {
            this(annotation.value().hashCode(), 0, false, method);
        }

        public MethodInfo(Property annotation, Method method)
        {
            this(annotation.value().hashCode(), 0, annotation.cached(), method);
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
        process(cl, cl);
    }

    public static void process(Class<?> register, Class<?> cl)
    {
        Class<?> examined = cl;

        SparseArray<List<MethodInfo>> classActionHookMethods         = new SparseArray<>();
        SparseArray<List<MethodInfo>> classPropertySubscriberMethods = new SparseArray<>();
        SparseArray<MethodInfo>       classDefaultPropertyMethods    = new SparseArray<>();

        do
        {
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

                    int name = (hook.value() + params.length).hashCode();
                    List<MethodInfo> methods = classActionHookMethods.get(name);
                    if (methods == null)
                    {
                        methods = new ArrayList<>();
                        classActionHookMethods.put(name, methods);
                    }

                    methods.add(new MethodInfo(hook, method));
                }
                else if (method.isAnnotationPresent(PropertySubscriber.class))
                {
                    PropertySubscriber subscriber = method.getAnnotation(PropertySubscriber.class);

                    Class<?>[] params = method.getParameterTypes();
                    if ((params.length != 1) || ((method.getModifiers() & Modifier.PUBLIC) == 0))
                    {
                        throw new IllegalArgumentException(
                                String.format(Locale.ENGLISH,
                                        "Method %s has @PropertySubscriber annotation but has too many argumets", method)
                        );
                    }

                    int name = subscriber.value().hashCode();
                    List<MethodInfo> methods = classPropertySubscriberMethods.get(name);
                    if (methods == null)
                    {
                        methods = new ArrayList<>();
                        classPropertySubscriberMethods.put(name, methods);
                    }

                    methods.add(new MethodInfo(subscriber, method));
                }
                else if (method.isAnnotationPresent(Property.class))
                {
                    Property property = method.getAnnotation(Property.class);

                    Class<?>[] params = method.getParameterTypes();
                    Class<?> returnType = method.getReturnType();

                    if ((params.length != 0) || ((method.getModifiers() & Modifier.PUBLIC) == 0) || returnType.equals(Void.TYPE))
                    {
                        throw new IllegalArgumentException(
                                String.format(Locale.ENGLISH,
                                        "Method %s has @Property annotation but has argumets", method)
                        );
                    }

                    int name = property.value().hashCode();
                    MethodInfo methodInfo = classDefaultPropertyMethods.get(name);
                    if (methodInfo != null)
                    {
                        throw new IllegalArgumentException(
                                String.format(Locale.ENGLISH,
                                        "Method %s has already been registered", property.value())
                        );
                    }

                    classDefaultPropertyMethods.put(name, new MethodInfo(property, method));
                }
            }

            if (examined.isAnnotationPresent(InheritCodex.class))
            {
                final Class<?>[] interfaces = examined.getInterfaces();
                for (Class<?> i : interfaces)
                {
                    process(register, i);
                }

                examined = examined.getSuperclass();
            }
            else
            {
                examined = null;
            }
        }
        while (examined != null);

        SparseArray<List<MethodInfo>> array;

        array = allActionHooks.get(register.hashCode());
        if (array == null) array = new SparseArray<>();

        int size = classActionHookMethods.size();
        for (int i = 0; i < size; i++)
        {
            array.append(classActionHookMethods.keyAt(i), classActionHookMethods.valueAt(i));
        }

        allActionHooks.put(register.hashCode(), array);

        array = allPropertySubscribers.get(register.hashCode());
        if (array == null) array = new SparseArray<>();

        size = classPropertySubscriberMethods.size();
        for (int i = 0; i < size; i++)
        {
            array.append(classPropertySubscriberMethods.keyAt(i), classPropertySubscriberMethods.valueAt(i));
        }

        allPropertySubscribers.put(register.hashCode(), array);

        SparseArray<MethodInfo> array2 = allDefaultProperties.get(register.hashCode());
        if (array2 == null) array2 = new SparseArray<>();

        size = classDefaultPropertyMethods.size();
        for (int i = 0; i < size; i++)
        {
            array2.append(classDefaultPropertyMethods.keyAt(i), classDefaultPropertyMethods.valueAt(i));
        }

        allDefaultProperties.put(register.hashCode(), array2);
    }

    public static SparseArray<List<ActionHookHandler>> findActionHooks(Object object)
    {
        Class<?> cl = object.getClass();

        SparseArray<List<ActionHookHandler>> actionHookHandlers = new SparseArray<>();

        SparseArray<List<MethodInfo>> actionHooks = allActionHooks.get(cl.hashCode());
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

                List<MethodInfo> methodInfos = actionHooks.valueAt(i);
                List<ActionHookHandler> handlers = new ArrayList<>();

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

    public static SparseArray<PropertyHandler> findDefaultProperties(Object object)
    {
        Class<?> cl = object.getClass();

        SparseArray<PropertyHandler> defaultPropertyHandlers = new SparseArray<>();

        SparseArray<MethodInfo> defaultProperties = allDefaultProperties.get(cl.hashCode());
        if (defaultProperties == null)
        {
            process(cl);
            defaultProperties = allDefaultProperties.get(cl.hashCode());
        }

        int length = defaultProperties.size();
        if (length != 0)
        {
            for (int i = 0; i < length; i++)
            {
                int key = defaultProperties.keyAt(i);

                MethodInfo methodInfo = defaultProperties.valueAt(i);

                PropertyHandler handler = new PropertyHandler(object, methodInfo);
                defaultPropertyHandlers.put(key, handler);
            }
        }

        return defaultPropertyHandlers;

    }

    public static SparseArray<List<PropertySubscriberHandler>> findPropertySubscribers(Object object)
    {
        Class<?> cl = object.getClass();

        SparseArray<List<PropertySubscriberHandler>> actionHookHandlers = new SparseArray<>();

        SparseArray<List<MethodInfo>> propertySubscribers = allPropertySubscribers.get(cl.hashCode());
        if (propertySubscribers == null)
        {
            process(cl);
            propertySubscribers = allPropertySubscribers.get(cl.hashCode());
        }

        int length = propertySubscribers.size();
        if (length != 0)
        {
            for (int i = 0; i < length; i++)
            {
                int key = propertySubscribers.keyAt(i);

                List<MethodInfo> methodInfos = propertySubscribers.valueAt(i);
                List<PropertySubscriberHandler> handlers = new ArrayList<>();

                for (MethodInfo methodInfo : methodInfos)
                {
                    PropertySubscriberHandler handler = new PropertySubscriberHandler(object, methodInfo);
                    handlers.add(handler);
                }

                actionHookHandlers.put(key, handlers);
            }
        }

        return actionHookHandlers;
    }
}
