package com.mobilesolutionworks.codex.internal.doclet;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by yunarta on 18/8/15.
 */
public class ReceiverDoclet {

    public Map<String, Set<ActionHookDoclet>> declaredActionHooks = new TreeMap<>();

    public Map<String, Set<PropertySubscriberDoclet>> declaredPropertySubscribers = new TreeMap<>();
}
