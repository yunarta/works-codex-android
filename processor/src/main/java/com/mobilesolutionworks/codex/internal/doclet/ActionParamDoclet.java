package com.mobilesolutionworks.codex.internal.doclet;

import com.mobilesolutionworks.codex.ActionParameter;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

/**
 * Created by yunarta on 19/8/15.
 */
public class ActionParamDoclet {

    public final String name;

    public final TypeMirror type;

    public ActionParamDoclet(ActionParameter annotation) {
        this.name = annotation.name();
        this.type = getTypeFromActionParameter(annotation);
    }

    public ActionParamDoclet(String name, TypeMirror type) {
        this.name = name;
        this.type = type;
    }

    private TypeMirror getTypeFromActionParameter(ActionParameter action) {
        TypeMirror typeMirrors = null;
        try {
            action.type();
        } catch (MirroredTypeException e) {
            typeMirrors = e.getTypeMirror();
        }

        return typeMirrors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionParamDoclet that = (ActionParamDoclet) o;
        return type.equals(that.type);

    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type + " " + name;
    }
}
