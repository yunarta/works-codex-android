package com.mobilesolutionworks.codex;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by yunarta on 9/8/15.
 */
public class Codex {

    private static final int START_ACTION    = 1;
    private static final int UPDATE_PROPERTY = 2;

    SparseArray<List<ActionHookHandler>>         allHooks       = new SparseArray<>();
    SparseArray<List<PropertySubscriberHandler>> allSubscribers = new SparseArray<>();
    SparseArray<PropertyHandler>                 allProperties  = new SparseArray<>();

    WeakReference<PropertySubscriberHandler[]> _subscriberHandlers;
    WeakReference<ActionHookHandler[]>         _actionHandlers;

    final Handler mHandler;

    public Codex() {
        this(Looper.getMainLooper());
    }

    public Codex(Handler handler) {
        this(handler.getLooper());
    }

    public Codex(Looper looper) {
        mHandler = new Handler(looper, new CallbackImpl());
    }

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
        SparseArray<PropertyHandler> properties = ReflectionAnnotationProcessor.findDefaultProperties(object);
        if (properties != null && properties.size() != 0) {
            length = properties.size();

            for (int i = 0; i < length; i++) {
                int key = properties.keyAt(i);
                PropertyHandler handler = properties.valueAt(i);
                allProperties.put(key, handler);

                Object value;
                try {
                    value = handler.getProperty();
                    dispatchPropertyToSubscribers(value, allSubscribers.get(key));
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Property " + handler + "throws an exception", e);
                }
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

                allSubscribers.put(key, registeredHandlers);
            }
        }

        // dispatch the properties to newly registered subscribers
        if (subscribers != null && subscribers.size() != 0) {
            for (int i = 0; i < length; i++) {
                int key = subscribers.keyAt(i);
                List<PropertySubscriberHandler> handlers = subscribers.valueAt(i);

                PropertyHandler propertyHandler = allProperties.get(key);
                if (propertyHandler != null) {
                    try {
                        Object value = propertyHandler.getProperty();
                        dispatchPropertyToSubscribers(value, handlers);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Property " + propertyHandler + "throws an exception", e);
                    }
                }
            }
        }
    }

    public void unregister(Object object) {
        SparseArray<List<ActionHookHandler>> hooks = ReflectionAnnotationProcessor.findActionHooks(object);
        int length = hooks.size();
        if (length != 0) {
            for (int i = 0; i < length; i++) {
                int key = hooks.keyAt(i);
                List<ActionHookHandler> handlers = hooks.valueAt(i);
                List<ActionHookHandler> registeredHooks = allHooks.get(key);

                registeredHooks.removeAll(handlers);
            }
        }

        SparseArray<PropertyHandler> properties = ReflectionAnnotationProcessor.findDefaultProperties(object);
        length = properties.size();
        if (length != 0) {
            for (int i = 0; i < length; i++) {
                int key = properties.keyAt(i);
                PropertyHandler handler = properties.valueAt(i);
                PropertyHandler registeredHandler = allProperties.get(key);

                if (handler.equals(registeredHandler)) {
                    allProperties.remove(key);
                }
            }
        }

        SparseArray<List<PropertySubscriberHandler>> subscribers = ReflectionAnnotationProcessor.findPropertySubscribers(object);
        length = hooks.size();
        if (length != 0) {
            for (int i = 0; i < length; i++) {
                int key = subscribers.keyAt(i);
                List<PropertySubscriberHandler> handlers = subscribers.valueAt(i);

                List<PropertySubscriberHandler> registeredSubscribers = allSubscribers.valueAt(key);
                registeredSubscribers.removeAll(handlers);
            }
        }
    }

    /**
     * Publish an action to the system and allow the hook to pick it up.
     */
    public void startActionEnforced(Object object, String name, Object... args) {
        int key = (name + args.length).hashCode();

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.obtainMessage(START_ACTION, key, 0, args).sendToTarget();
        } else {
            dispatchStartAction(key, args);
        }
    }
    /**
     * Publish an action to the system and allow the hook to pick it up.
     */
    public void startAction(String name, Object... args) {
        int key = (name + args.length).hashCode();

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.obtainMessage(START_ACTION, key, 0, args).sendToTarget();
        } else {
            dispatchStartAction(key, args);
        }
    }

    /**
     * Update specified property for this owner
     */
    public void updateProperty(Object owner, String name) {
        int key = name.hashCode();

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.obtainMessage(UPDATE_PROPERTY, key, 0, owner).sendToTarget();
        } else {
            dispatchUpdateProperty(owner, key);
        }
    }

    private void dispatchPropertyToSubscribers(Object value, List<PropertySubscriberHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) return;

        if (_subscriberHandlers == null || _subscriberHandlers.get() == null) {
            _subscriberHandlers = new WeakReference<>(new PropertySubscriberHandler[handlers.size()]);
        }

        for (PropertySubscriberHandler handler : handlers.toArray(_subscriberHandlers.get())) {
            try {
                handler.receiveProperty(value);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Could not dispatch property " + value + " to " + handler, e);
            }
        }
    }

    private class CallbackImpl implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case START_ACTION: {
                    dispatchStartAction(msg.arg1, (Object[]) msg.obj);
                    break;
                }

                case UPDATE_PROPERTY: {
                    dispatchUpdateProperty(msg.obj, msg.arg1);
                    break;
                }
            }

            return true;
        }
    }

    private void dispatchStartAction(int key, Object[] args) {
        List<ActionHookHandler> handlers = allHooks.get(key);
        if (handlers == null) return;

        if (_actionHandlers == null || _actionHandlers.get() == null) {
            _actionHandlers = new WeakReference<>(new ActionHookHandler[handlers.size()]);
        }

        for (ActionHookHandler handler : handlers.toArray(_actionHandlers.get())) {
            try {
                handler.actionHook(args);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatchUpdateProperty(Object owner, int key) {
        PropertyHandler propertyHandler = allProperties.get(key);
        if (propertyHandler != null) {
            if (propertyHandler.target != owner) {
                throw new IllegalStateException("only owner can dispatch this property change");
            }

            List<PropertySubscriberHandler> handlers = allSubscribers.get(key);
            try {
                Object value = propertyHandler.getProperty();
                dispatchPropertyToSubscribers(value, handlers);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Property " + propertyHandler + "throws an exception", e);
            }
        }
    }
}