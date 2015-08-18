package com.mobilesolutionworks.codex.internal.doclet;

import com.mobilesolutionworks.codex.Property;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Created by yunarta on 18/8/15.
 */
public class PropertyDoclet {

    public final TypeMirror type;

    public final String className;

    public final String name;

    public final String method;

    public PropertyDoclet(String className, Property annotation, Element element) {
        this.className = className;
        this.name = annotation.name();

        ExecutableElement executable = (ExecutableElement) element;
        this.method = executable.getSimpleName().toString();
        this.type = executable.getReturnType();
    }

    public String key() {
        return name;
    }

    @Override
    public String toString() {
        return "PropertyDoclet{class=" + className + ", name=" + name + ", method=" + type + " " + method + "()}";
    }

}
