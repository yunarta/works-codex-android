package com.mobilesolutionworks.codex.internal.doclet;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by yunarta on 18/8/15.
 */
public class EmitterDoclet {

    public Map<String, ActionDoclet> declaredActions = new TreeMap<>();

    public Map<String, PropertyDoclet> declaredProperties = new TreeMap<>();
}
