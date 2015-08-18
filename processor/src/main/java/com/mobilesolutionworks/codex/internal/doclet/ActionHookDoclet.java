package com.mobilesolutionworks.codex.internal.doclet;

import com.mobilesolutionworks.codex.ActionHook;
import com.mobilesolutionworks.codex.internal.util.PrintUtils;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * Created by yunarta on 18/8/15.
 */
public class ActionHookDoclet implements Comparable<ActionHookDoclet> {

    public final String className;

    public final String action;

    public final int priority;

    public final String method;

    public final List<ActionParamDoclet> args;

    public ActionHookDoclet(String className, ActionHook annotation, Element element) {
        this.className = className;
        this.action = annotation.name();
        this.priority = annotation.priority();

        ExecutableElement executable = (ExecutableElement) element;
        method = executable.getSimpleName().toString();
        args = new ArrayList<>();

        List<? extends VariableElement> params = executable.getParameters();
        for (VariableElement param : params) {
            args.add(new ActionParamDoclet(param.getSimpleName().toString(), param.asType()));
        }
    }

    @Override
    public String toString() {
        return "ActionHookDoclet{" + action + " " + signature() + "}";
    }

    public String key() {
        return action + args.size();
    }

    public String signature() {
        return method + "(" + PrintUtils.concat(", ", args) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionHookDoclet doc = (ActionHookDoclet) o;

        if (!action.equals(doc.action)) return false;
        if (!signature().equals(doc.signature())) return false;
        return className.equals(doc.className);

    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + signature().hashCode();
        result = 31 * result + className.hashCode();
        return result;
    }

    @Override
    public int compareTo(ActionHookDoclet o) {
        return action.compareTo(o.action);
    }
}
