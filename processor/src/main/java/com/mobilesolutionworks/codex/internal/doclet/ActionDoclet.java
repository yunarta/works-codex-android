package com.mobilesolutionworks.codex.internal.doclet;

import com.mobilesolutionworks.codex.Action;
import com.mobilesolutionworks.codex.ActionParameter;
import com.mobilesolutionworks.codex.internal.util.PrintUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yunarta on 18/8/15.
 */
public class ActionDoclet {

    public final String className;

    public final String action;

    public final List<ActionParamDoclet> args;

    public ActionDoclet(String className, Action action) {
        this.className = className;
        this.action = action.name();

        this.args = new ArrayList<>();
        for (ActionParameter arg : action.args()) {
            args.add(new ActionParamDoclet(arg));
        }
    }


    public String key() {
        return action + args.size();
    }


    @Override
    public String toString() {
        return "ActionDoclet{class=" + className + ", action=" + signature() + "}";
    }

    public String signature() {
        return action + "(" + PrintUtils.concat(", ", args) + ")";
    }
}
