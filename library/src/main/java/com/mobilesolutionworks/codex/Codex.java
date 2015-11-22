package com.mobilesolutionworks.codex;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by yunarta on 9/8/15.
 */
public class Codex
{
    protected static final Logger LOGGER = Logger.getLogger(Codex.class.getName());

    private static final int START_ACTION    = 1;
    private static final int UPDATE_PROPERTY = 2;

    SparseArray<List<ActionHookHandler>>         allHooks       = new SparseArray<>();
    SparseArray<List<PropertySubscriberHandler>> allSubscribers = new SparseArray<>();
    SparseArray<PropertyHandler>                 allProperties  = new SparseArray<>();

    WeakReference<ArrayList<PropertySubscriberHandler>> _subscriberHandlers;
    WeakReference<ArrayList<ActionHookHandler>>         _actionHandlers;

    final Handler      mHandler;
    final List<Object> mObjects;

    boolean mUseWeakReference;

    SparseArray<String> mActions;

    public Codex()
    {
        this(Looper.getMainLooper());
    }

    public Codex(Handler handler)
    {
        this(handler.getLooper());
    }

    public Codex(Looper looper)
    {
        mHandler = new Handler(looper, new CallbackImpl());
        mObjects = new ArrayList<>();
        mActions = new SparseArray<>();
    }

    public void weakenReference()
    {
        mUseWeakReference = true;
    }

    public void register(Object object)
    {
        if (!mUseWeakReference) mObjects.add(object);

        SparseArray<List<ActionHookHandler>> hooks = ReflectionAnnotationProcessor.findActionHooks(object);

        // register all hooks
        int length = hooks.size();
        for (int i = 0; i < length; i++)
        {
            int                     key             = hooks.keyAt(i);
            List<ActionHookHandler> registeredHooks = allHooks.get(key, new ArrayList<ActionHookHandler>());

            List<ActionHookHandler> handlers = hooks.valueAt(i);

//            for (ActionHookHandler  handle : handlers)
//            {
//                LOGGER.fine(object + " key = " + key + " " + handle.info.method.getName() + " argc " + handle.info.argc);
//            }

            registeredHooks.addAll(handlers);
            Collections.sort(registeredHooks);


            allHooks.put(key, registeredHooks);
        }

        // check if the new object has property, and dispatch to existing property subscriber
        SparseArray<PropertyHandler> properties = ReflectionAnnotationProcessor.findDefaultProperties(object);
        if (properties != null && properties.size() != 0)
        {
            length = properties.size();

            for (int i = 0; i < length; i++)
            {
                int             key     = properties.keyAt(i);
                PropertyHandler handler = properties.valueAt(i);
                allProperties.put(key, handler);

                Object value;
                try
                {
                    value = handler.getProperty();
                    dispatchPropertyToSubscribers(value, allSubscribers.get(key));
                }
                catch (InvocationTargetException e)
                {
                    throw new RuntimeException("Property " + handler + "throws an exception", e);
                }
            }
        }

        // now get all property subscriber of the object
        SparseArray<List<PropertySubscriberHandler>> subscribers = ReflectionAnnotationProcessor.findPropertySubscribers(object);
        if (subscribers != null && subscribers.size() != 0)
        {
            length = subscribers.size();

            for (int i = 0; i < length; i++)
            {
                int                             key      = subscribers.keyAt(i);
                List<PropertySubscriberHandler> handlers = subscribers.valueAt(i);

                List<PropertySubscriberHandler> registeredHandlers = allSubscribers.get(key, new ArrayList<PropertySubscriberHandler>());
                registeredHandlers.addAll(handlers);

                allSubscribers.put(key, registeredHandlers);
            }
        }

        // dispatch the properties to newly registered subscribers
        if (subscribers != null && subscribers.size() != 0)
        {
            for (int i = 0; i < length; i++)
            {
                int                             key      = subscribers.keyAt(i);
                List<PropertySubscriberHandler> handlers = subscribers.valueAt(i);

                PropertyHandler propertyHandler = allProperties.get(key);
                if (propertyHandler != null)
                {
                    try
                    {
                        Object value = propertyHandler.getProperty();
                        dispatchPropertyToSubscribers(value, handlers);
                    }
                    catch (InvocationTargetException e)
                    {
                        throw new RuntimeException("Property " + propertyHandler + "throws an exception", e);
                    }
                }
            }
        }
    }

    public void unregister(Object object)
    {
        if (!mUseWeakReference) mObjects.remove(object);

        SparseArray<List<ActionHookHandler>> hooks  = ReflectionAnnotationProcessor.findActionHooks(object);
        int                                  length = hooks.size();

        if (length != 0)
        {
            for (int i = 0; i < length; i++)
            {
                int                     key             = hooks.keyAt(i);
                List<ActionHookHandler> handlers        = hooks.valueAt(i);
                List<ActionHookHandler> registeredHooks = allHooks.get(key);

                registeredHooks.removeAll(handlers);
            }
        }

        SparseArray<PropertyHandler> properties = ReflectionAnnotationProcessor.findDefaultProperties(object);
        length = properties.size();
        if (length != 0)
        {
            for (int i = 0; i < length; i++)
            {
                int             key               = properties.keyAt(i);
                PropertyHandler handler           = properties.valueAt(i);
                PropertyHandler registeredHandler = allProperties.get(key);

                if (handler.equals(registeredHandler))
                {
                    allProperties.remove(key);
                }
            }
        }

        SparseArray<List<PropertySubscriberHandler>> subscribers = ReflectionAnnotationProcessor.findPropertySubscribers(object);
        length = subscribers.size();
        if (length != 0)
        {
            for (int i = 0; i < length; i++)
            {
                int                             key      = subscribers.keyAt(i);
                List<PropertySubscriberHandler> handlers = subscribers.valueAt(i);

                List<PropertySubscriberHandler> registeredSubscribers = allSubscribers.get(key);
                if (registeredSubscribers != null)
                {
                    registeredSubscribers.removeAll(handlers);
                }
            }
        }
    }

    /**
     * Publish an action to the system and allow the hook to pick it up.
     */
    public void startActionEnforced(Object object, String name, Object... args)
    {
        int key = (name + args.length).hashCode();

        mHandler.obtainMessage(START_ACTION, key, 0, args).sendToTarget();
//        if (Looper.myLooper() != mHandler.getLooper()) {
//        } else {
//            dispatchStartAction(key, args);
//        }
    }

    /**
     * Publish an action to the system and allow the hook to pick it up.
     */
    public void startAction(String name, Object... args)
    {
        int key = (name + args.length).hashCode();
        mActions.put(key, name);

        mHandler.obtainMessage(START_ACTION, key, 0, args).sendToTarget();
//        if (Looper.myLooper() != mHandler.getLooper()) {
//        } else {
//            dispatchStartAction(key, args);
//        }
    }

    /**
     * Update specified property for this owner
     */
    public void updateProperty(Object owner, String name)
    {
        int key = name.hashCode();

        mHandler.obtainMessage(UPDATE_PROPERTY, key, 0, owner).sendToTarget();
//        if (Looper.myLooper() != mHandler.getLooper()) {
//        } else {
//            dispatchUpdateProperty(owner, key);
//        }
    }

    private void dispatchPropertyToSubscribers(Object value, List<PropertySubscriberHandler> handlers)
    {
        if (handlers == null || handlers.isEmpty()) return;

        if (_subscriberHandlers == null || _subscriberHandlers.get() == null)
        {
            _subscriberHandlers = new WeakReference<>(new ArrayList<PropertySubscriberHandler>());
        }

        ArrayList<PropertySubscriberHandler> list = _subscriberHandlers.get();
        list.clear();

        for (PropertySubscriberHandler h : handlers)
        {
            if (h.isReachable()) list.add(h);
        }
        handlers.retainAll(list);

        for (PropertySubscriberHandler handler : new ArrayList<>(list))
        {
            try
            {
                handler.receiveProperty(value);
            }
            catch (InvocationTargetException e)
            {
                throw new RuntimeException("Could not dispatch property " + value + " to " + handler, e);
            }
        }
    }

    private void dispatchStartAction(int key, Object[] args)
    {
        List<ActionHookHandler> handlers = allHooks.get(key);

        if (handlers == null)
        {
            String action = mActions.get(key);
            Log.w("Codex", "no action for [" + action + "] found in this Codex");
            return;
        }

        if (_actionHandlers == null || _actionHandlers.get() == null)
        {
            _actionHandlers = new WeakReference<>(new ArrayList<ActionHookHandler>());
        }

        ArrayList<ActionHookHandler> list = _actionHandlers.get();
        list.clear();

        for (ActionHookHandler h : handlers)
        {
            if (h.isReachable()) list.add(h);
        }
        handlers.retainAll(list);

//        LOGGER.fine("list = " + list);
        for (ActionHookHandler handler : new ArrayList<>(list))
        {
            try
            {
//                LOGGER.fine("target = " + handler.target.get() + " " + handler.info.method.getName());
                handler.actionHook(args);
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class CallbackImpl implements Handler.Callback
    {

        @Override
        public boolean handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case START_ACTION:
                {
                    dispatchStartAction(msg.arg1, (Object[]) msg.obj);
                    break;
                }

                case UPDATE_PROPERTY:
                {
                    dispatchUpdateProperty(msg.obj, msg.arg1);
                    break;
                }
            }

            return true;
        }
    }

    private void dispatchUpdateProperty(Object owner, int key)
    {
        PropertyHandler propertyHandler = allProperties.get(key);
        if (propertyHandler != null)
        {
            if (!propertyHandler.isReachable())
            {
                allProperties.remove(key);
                return;
            }

            if (propertyHandler.target.get() != owner)
            {
                throw new IllegalStateException("only owner can dispatch this property " + propertyHandler.info.method.getName() + " change");
            }

            List<PropertySubscriberHandler> handlers = allSubscribers.get(key);
            try
            {
                Object value = propertyHandler.getProperty();
                dispatchPropertyToSubscribers(value, handlers);
            }
            catch (InvocationTargetException e)
            {
                throw new RuntimeException("Property " + propertyHandler + "throws an exception", e);
            }
        }
    }
}