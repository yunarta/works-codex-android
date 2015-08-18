package com.mobilesolutionworks.codex.internal;

import com.jamesmurty.utils.XMLBuilder2;
import com.mobilesolutionworks.codex.Action;
import com.mobilesolutionworks.codex.ActionHook;
import com.mobilesolutionworks.codex.ActionInfo;
import com.mobilesolutionworks.codex.Property;
import com.mobilesolutionworks.codex.PropertySubscriber;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Created by yunarta on 18/8/15.
 */
public class AnnotationProcessor extends AbstractProcessor {

    class ActionInfoDoc {

        String owner;

        String name;

        String[] args;

        public String key() {
            return name + args.length;
        }

        public String method() {
            if (args.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String arg : args) {
                    sb.append(", ").append(arg);
                }
                sb.delete(0, 2);

                return name + "(" + sb.toString() + ")";
            } else {
                return name + "()";
            }
        }
    }

    class PropertyDoc {

        String owner;

        String name;

        String type;

        String method;
    }

    class EmitterDoc {

        Map<String, ActionInfoDoc> declaredActions = new TreeMap<>();

        Map<String, PropertyDoc> declaredProperties = new TreeMap<>();
    }

    class ActionHookInfoDoc implements Comparable<ActionHookInfoDoc> {

        String name;

        String method;

        String owner;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActionHookInfoDoc doc = (ActionHookInfoDoc) o;

            if (!name.equals(doc.name)) return false;
            if (!method.equals(doc.method)) return false;
            return owner.equals(doc.owner);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + method.hashCode();
            result = 31 * result + owner.hashCode();
            return result;
        }

        @Override
        public int compareTo(ActionHookInfoDoc o) {
            return name.compareTo(o.name);
        }
    }

    class PropertySubscriberInfoDoc implements Comparable<PropertySubscriberInfoDoc> {

        String name;

        String method;

        String owner;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PropertySubscriberInfoDoc doc = (PropertySubscriberInfoDoc) o;

            if (!name.equals(doc.name)) return false;
            if (!method.equals(doc.method)) return false;
            return owner.equals(doc.owner);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + method.hashCode();
            result = 31 * result + owner.hashCode();
            return result;
        }

        @Override
        public int compareTo(PropertySubscriberInfoDoc o) {
            return name.compareTo(o.name);
        }
    }

    class ReceiverDoc {

        Map<String, Set<ActionHookInfoDoc>> declaredActionHooks = new TreeMap<>();

        Map<String, Set<PropertySubscriberInfoDoc>> declaredPropertySubscribers = new TreeMap<>();
    }

    Map<String, EmitterDoc>  mEmitterDocMap = new TreeMap<>();
    Map<String, ReceiverDoc> mReceiverMap   = new TreeMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) return true;

        Map<String, String> options = processingEnv.getOptions();
        String outputDir = options.get("codexOutput");

        if (annotations.isEmpty()) return true;

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out));
        writer = createCodexWriter(outputDir, writer);

        Set<? extends Element> elements;

        elements = env.getElementsAnnotatedWith(Action.class);
        for (Element element : elements) {
            Action annotation = element.getAnnotation(Action.class);
            TypeElement typeElement = (TypeElement) element;

            String key = typeElement.getQualifiedName().toString();

            EmitterDoc ci = mEmitterDocMap.getOrDefault(key, new EmitterDoc());
            mEmitterDocMap.put(key, ci);

            ActionInfo[] infos = annotation.actions();
            for (ActionInfo info : infos) {
                ActionInfoDoc doc = new ActionInfoDoc();
                doc.owner = key;
                doc.name = info.name();
                doc.args = info.args();

                String actionKey = doc.key();
                if (ci.declaredActions.containsKey(actionKey)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, key + " has duplicate decration of action " + doc.name);
                }

                ci.declaredActions.put(actionKey, doc);
            }
        }

        elements = env.getElementsAnnotatedWith(ActionHook.class);
        for (Element element : elements) {
            ActionHook annotation = element.getAnnotation(ActionHook.class);
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();

            ExecutableElement variable = (ExecutableElement) element;
            List<? extends VariableElement> parameters = variable.getParameters();
            int argc = parameters.size();

            StringBuffer sb = new StringBuffer();
            for (VariableElement param : parameters) {
                sb.append(", ").append(param.getSimpleName());
            }
            if (sb.length() > 0) {
                sb.delete(0, 2);
            }

            String methodName = element.getSimpleName().toString() + "(" + sb.toString() + ")";

            String key = typeElement.getQualifiedName().toString();
            String hookKey = annotation.name() + argc;

            ReceiverDoc info = mReceiverMap.getOrDefault(key, new ReceiverDoc());
            mReceiverMap.put(key, info);

            Set<ActionHookInfoDoc> set = info.declaredActionHooks.getOrDefault(hookKey, new TreeSet<ActionHookInfoDoc>());
            info.declaredActionHooks.put(hookKey, set);

            ActionHookInfoDoc doc = new ActionHookInfoDoc();
            doc.name = annotation.name();
            doc.method = methodName;
            doc.owner = key;

            set.add(doc);
        }

        elements = env.getElementsAnnotatedWith(Property.class);
        for (Element element : elements) {
            Property annotation = element.getAnnotation(Property.class);
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();

            String key = typeElement.getQualifiedName().toString();

            EmitterDoc info = mEmitterDocMap.getOrDefault(key, new EmitterDoc());
            mEmitterDocMap.put(key, info);

            if (info.declaredProperties.containsKey(annotation.name())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, key + " have duplicate declaration for property " + annotation.name());
            }

            ExecutableElement variable = (ExecutableElement) element;
            TypeMirror type = variable.getReturnType();

            PropertyDoc doc = new PropertyDoc();
            doc.name = annotation.name();
            doc.method = element.getSimpleName().toString();
            doc.owner = key;
            doc.type = type.toString();
            info.declaredProperties.put(annotation.name(), doc);
        }

        elements = env.getElementsAnnotatedWith(PropertySubscriber.class);
        for (Element element : elements) {
            PropertySubscriber annotation = element.getAnnotation(PropertySubscriber.class);
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();

            String key = typeElement.getQualifiedName().toString();
            String subscriberKey = annotation.name();

            ReceiverDoc info = mReceiverMap.getOrDefault(key, new ReceiverDoc());
            mReceiverMap.put(key, info);

            Set<PropertySubscriberInfoDoc> set = info.declaredPropertySubscribers.getOrDefault(subscriberKey, new TreeSet<PropertySubscriberInfoDoc>());
            info.declaredPropertySubscribers.put(subscriberKey, set);

            PropertySubscriberInfoDoc doc = new PropertySubscriberInfoDoc();
            doc.name = annotation.name();
            doc.method = element.getSimpleName().toString();
            doc.owner = key;

            set.add(doc);
        }


        XMLBuilder2 codex = XMLBuilder2.create("codex");

        XMLBuilder2 emitters = codex.e("emitters");
        XMLBuilder2 receivers = codex.e("receivers");

        XMLBuilder2 actions = codex.e("actions");
        XMLBuilder2 properties = codex.e("properties");

        Map<String, ActionInfoDoc> allActions = new TreeMap<>();
        Map<String, PropertyDoc> allProperties = new TreeMap<>();
        Map<String, List<ActionHookInfoDoc>> allActionHooks = new TreeMap<>();
        Map<String, List<PropertySubscriberInfoDoc>> allPropertySubscribers = new TreeMap<>();


        for (Map.Entry<String, EmitterDoc> entry : mEmitterDocMap.entrySet()) {
            String name = entry.getKey();
            EmitterDoc value = entry.getValue();

            XMLBuilder2 entity = emitters.e("entity").a("type", name);
            XMLBuilder2 iterator = entity.e("actions");
            for (Map.Entry<String, ActionInfoDoc> action : value.declaredActions.entrySet()) {
                String key = action.getKey();
                ActionInfoDoc doc = action.getValue();

                boolean conflict = allActions.containsKey(key);
                if (conflict) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, doc.method() + " had been declared in " + allActions.get(key).owner);
                }

                allActions.put(key, doc);

                XMLBuilder2 actionTag = iterator.e("action");
                actionTag.a("name", doc.method());
                iterator = actionTag.up();
            }

            iterator = entity.e("properties");
            for (Map.Entry<String, PropertyDoc> property : value.declaredProperties.entrySet()) {
                String key = property.getKey();

                boolean conflict = allProperties.containsKey(key);
                if (conflict) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, property + " had been declared in " + allProperties.get(key));
                }

                PropertyDoc doc = property.getValue();
                allProperties.put(key, doc);

                XMLBuilder2 propertyTag = iterator.e("property");
                propertyTag.a("name", doc.name);
                propertyTag.a("method", doc.method);
                propertyTag.a("type", doc.type);
                iterator = propertyTag.up();
            }

            iterator.up();
            entity.up();
        }

        emitters.up();

        for (Map.Entry<String, ReceiverDoc> entry : mReceiverMap.entrySet()) {
            String name = entry.getKey();
            ReceiverDoc value = entry.getValue();

            XMLBuilder2 entity = receivers.e("receiver").a("type", name);

            XMLBuilder2 iterator = entity.e("actionHooks");
            for (Map.Entry<String, Set<ActionHookInfoDoc>> docs : value.declaredActionHooks.entrySet()) {
                String hookKey = docs.getKey();
                Set<ActionHookInfoDoc> set = docs.getValue();


                for (ActionHookInfoDoc doc : set) {
                    XMLBuilder2 actionTag = iterator.e("actionHook");
                    actionTag.a("name", doc.name);
                    actionTag.a("method", doc.method);
                    iterator = actionTag.up();

                    List<ActionHookInfoDoc> list = allActionHooks.getOrDefault(hookKey, new ArrayList<ActionHookInfoDoc>());
                    allActionHooks.put(hookKey, list);

                    list.add(doc);
                }
            }

            iterator = entity.e("propertySubscribers");
            for (Map.Entry<String, Set<PropertySubscriberInfoDoc>> eSubscriber : value.declaredPropertySubscribers.entrySet()) {
                String hookKey = eSubscriber.getKey();
                Set<PropertySubscriberInfoDoc> set = eSubscriber.getValue();

                for (PropertySubscriberInfoDoc doc : set) {
                    XMLBuilder2 actionTag = iterator.e("propertySubscriber");
                    actionTag.a("name", doc.name);
                    actionTag.a("method", doc.method);
                    iterator = actionTag.up();

                    List<PropertySubscriberInfoDoc> list = allPropertySubscribers.getOrDefault(hookKey, new ArrayList<PropertySubscriberInfoDoc>());
                    allPropertySubscribers.put(hookKey, list);

                    list.add(doc);
                }
            }

            iterator.up();
            entity.up();
        }

        receivers.up();

        for (Map.Entry<String, ActionInfoDoc> entry : allActions.entrySet()) {
            XMLBuilder2 actionTag = actions.e("action");

            ActionInfoDoc actionInfo = entry.getValue();
            actionTag.a("name", actionInfo.method());
            actionTag.a("owner", actionInfo.owner);

            List<ActionHookInfoDoc> actionHookInfo = allActionHooks.remove(actionInfo.key());
            if (actionHookInfo != null) {
                for (ActionHookInfoDoc hook : actionHookInfo) {
                    XMLBuilder2 hookTag = actionTag.e("hook");
                    hookTag.a("name", hook.name);
                    hookTag.a("method", hook.method);
                    hookTag.a("owner", hook.owner);
                    hookTag.up();
                }
            }

            actionTag.up();
        }
        actions.up();


        for (Map.Entry<String, PropertyDoc> entry : allProperties.entrySet()) {
            XMLBuilder2 propertyTag = properties.e("property");
            PropertyDoc doc = entry.getValue();

            propertyTag.a("name", doc.name);
            propertyTag.a("method", doc.method);
            propertyTag.a("type", doc.type);
            propertyTag.a("owner", doc.owner);

            List<PropertySubscriberInfoDoc> subscriberInfoDocs = allPropertySubscribers.remove(doc.name);
            if (subscriberInfoDocs != null) {
                for (PropertySubscriberInfoDoc subscriber : subscriberInfoDocs) {
                    XMLBuilder2 hookTag = propertyTag.e("subscriber");
                    hookTag.a("name", subscriber.name);
                    hookTag.a("method", subscriber.method);
                    hookTag.a("owner", subscriber.owner);
                    hookTag.up();
                }
            }

            propertyTag.up();
        }
        properties.up();

        XMLBuilder2 orphanHooks = codex.e("orphan-actionHooks");

        for (Map.Entry<String, List<ActionHookInfoDoc>> entry : allActionHooks.entrySet()) {
            List<ActionHookInfoDoc> actionHookInfo = entry.getValue();
            if (actionHookInfo != null) {
                for (ActionHookInfoDoc hook : actionHookInfo) {
                    XMLBuilder2 hookTag = orphanHooks.e("hook");
                    hookTag.a("name", hook.name);
                    hookTag.a("method", hook.method);
                    hookTag.a("owner", hook.owner);
                    hookTag.up();
                }
            }
        }
        orphanHooks.up();

        XMLBuilder2 orphanSubscriber = codex.e("orphan-subscriber");

        for (Map.Entry<String, List<PropertySubscriberInfoDoc>> entry : allPropertySubscribers.entrySet()) {
            List<PropertySubscriberInfoDoc> subscriberInfoDocs = entry.getValue();
            if (subscriberInfoDocs != null) {
                for (PropertySubscriberInfoDoc subscriber : subscriberInfoDocs) {
                    XMLBuilder2 hookTag = orphanSubscriber.e("subscriber");
                    hookTag.a("name", subscriber.name);
                    hookTag.a("method", subscriber.method);
                    hookTag.a("owner", subscriber.owner);
                    hookTag.up();
                }
            }

        }
        orphanSubscriber.up();


        codex.up();
        codex.toWriter(true, writer, null);
        writer.flush();
        writer.close();

        return true;
    }

    private PrintWriter createCodexWriter(String outputDir, PrintWriter writer) {
        try {
            if (outputDir != null) {
                writer = new PrintWriter(new FileOutputStream(new File(outputDir, "codex.xml")));
            }
        } catch (FileNotFoundException e) {
            // e.printStackTrace();
            e.printStackTrace();
        }
        return writer;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportedAnnotationTypes = new HashSet<>();
        supportedAnnotationTypes.add(Action.class.getCanonicalName());
        supportedAnnotationTypes.add(ActionHook.class.getCanonicalName());
        supportedAnnotationTypes.add(Property.class.getCanonicalName());
        supportedAnnotationTypes.add(PropertySubscriber.class.getCanonicalName());

        return supportedAnnotationTypes;
    }

    @Override
    public Set<String> getSupportedOptions() {
        HashSet<String> supportedOptions = new HashSet<>();
        supportedOptions.add("codexOutput");

        return supportedOptions;
    }
}
