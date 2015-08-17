package com.moblesolutionworks.codex;

import android.util.Log;
import android.util.SparseArray;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by yunarta on 9/8/15.
 */
public class Codex {
    SparseArray<List<ActionHookHandler>>        allHooks       = new SparseArray<>();
    SparseArray<List<PropertySubscriberHandler>> allSubscribers = new SparseArray<>();
    SparseArray<DefaultPropertyHandler>         allProperties  = new SparseArray<>();

    public void register(Object object) {
        SparseArray<List<ActionHookHandler>> hooks = ReflectionAnnotationProcessor.findActionHooks(object);

        // register all hooks
        int length = hooks.size();
        for (int i = 0; i < length; i++) {
            int key = hooks.keyAt(i);
            List<ActionHookHandler> registeredHooks = allHooks.get(key, new ArrayList<ActionHookHandler>());

            List<ActionHookHandler> handlers = hooks.valueAt(i);

            registeredHooks.addAll(handlers);
            Collections.sort(registeredHooks);

            allHooks.put(key, registeredHooks);
        }

        // check if the new object has property, and dispatch to existing property subscriber
        SparseArray<DefaultPropertyHandler> properties = ReflectionAnnotationProcessor.findDefaultProperties(object);
        if (properties != null && properties.size() != 0) {
            length = properties.size();

            for (int i = 0; i < length; i++) {
                int key = properties.keyAt(i);
                DefaultPropertyHandler handler = properties.valueAt(i);
                allProperties.put(key, handler);

                Object value;
                try {
                    value = handler.getProperty();
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                dispatchPropertyChange(value, allSubscribers.get(key));
            }
        }

        // now get all property subscriber of the object
        SparseArray<List<PropertySubscriberHandler>> subscribers = ReflectionAnnotationProcessor.findPropertySubscribers(object);
        if (subscribers != null && subscribers.size() != 0) {
            length = subscribers.size();

            for (int i = 0; i < length; i++) {
                int key = subscribers.keyAt(i);
                List<PropertySubscriberHandler> handlers = subscribers.valueAt(i);

                List<PropertySubscriberHandler> registeredHandlers = allSubscribers.get(key, new ArrayList<PropertySubscriberHandler>());
                registeredHandlers.addAll(handlers);
            }
        }

        // dispatch the properties to newly registered subscribers
        if (subscribers != null && subscribers.size() != 0) {
            for (int i = 0; i < length; i++) {
                int key = subscribers.keyAt(i);
                List<PropertySubscriberHandler> handlers = subscribers.valueAt(i);

                DefaultPropertyHandler propertyHandler = allProperties.get(key);
                if (propertyHandler == null) return;

                Object value;
                try {
                    value = propertyHandler.getProperty();
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                dispatchPropertyChange(value, handlers);
            }
        }
    }

    /**
     * Publish an action to the system and allow the hook to pick it up.
     */
    public void startAction(String name, Object... args) {
        int key = (name + args.length).hashCode();
        List<ActionHookHandler> handlers = allHooks.get(key);
        if (handlers == null) return;

        ArrayList<ActionHookHandler> executingHandlers = new ArrayList<>(handlers);
        for (ActionHookHandler handler : executingHandlers) {
            try {
                handler.actionHook(args);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Update specified property for this owner
     */
    public void updateProperty(Object owner, String name) {
        int key = name.hashCode();

        DefaultPropertyHandler propertyHandler = allProperties.get(key);
        if (propertyHandler == null) return;

        if (propertyHandler.target != owner) {
            throw new IllegalStateException("only owner can dispatch this property change");
        }

        Object value;
        try {
            value = propertyHandler.getProperty();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        dispatchPropertyChange(value, allSubscribers.get(key));
    }

    private void dispatchPropertyChange(Object value, List<PropertySubscriberHandler> handlers) {
        if (handlers == null) return;

        for (PropertySubscriberHandler handler : handlers) {
            try {
                handler.receiveProperty(value);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
