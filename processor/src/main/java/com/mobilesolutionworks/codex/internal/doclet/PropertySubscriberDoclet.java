package com.mobilesolutionworks.codex.internal.doclet;

import com.mobilesolutionworks.codex.PropertySubscriber;
import com.mobilesolutionworks.codex.internal.util.PrintUtils;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * Created by yunarta on 18/8/15.
 */
public class PropertySubscriberDoclet implements Comparable<PropertySubscriberDoclet> {

    public final String name;

    public final String method;

    public final String className;

    public final ActionParamDoclet arg;

    private final List<ActionParamDoclet> args;

    public PropertySubscriberDoclet(String className, PropertySubscriber annotation, Element element) {
        this.className = className;
        this.name = annotation.name();

        ExecutableElement executable = (ExecutableElement) element;
        this.method = executable.getSimpleName().toString();
        args = new ArrayList<>();


        List<? extends VariableElement> params = executable.getParameters();

        ActionParamDoclet doclet = null;
        for (VariableElement param : params) {
            doclet = new ActionParamDoclet(param.getSimpleName().toString(), param.asType());
            args.add(doclet);
        }

        arg = doclet;
    }

    @Override
    public String toString() {
        return "PropertySubscriberDoclet{" + name + " " + signature() + "}";
    }

    public String signature() {
        return method + "(" + PrintUtils.concat(", ", args) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertySubscriberDoclet doc = (PropertySubscriberDoclet) o;

        if (!name.equals(doc.name)) return false;
        if (!signature().equals(doc.signature())) return false;
        return className.equals(doc.className);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + signature().hashCode();
        result = 31 * result + className.hashCode();
        return result;
    }

    @Override
    public int compareTo(PropertySubscriberDoclet o) {
        return name.compareTo(o.name);
    }

    public boolean isValid() {
        return args.size() == 1;
    }
}
