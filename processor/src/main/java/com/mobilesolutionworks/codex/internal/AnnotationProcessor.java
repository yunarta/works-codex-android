package com.mobilesolutionworks.codex.internal;

import com.jamesmurty.utils.XMLBuilder2;
import com.mobilesolutionworks.codex.Action;
import com.mobilesolutionworks.codex.ActionHook;
import com.mobilesolutionworks.codex.ActionProvider;
import com.mobilesolutionworks.codex.Property;
import com.mobilesolutionworks.codex.PropertySubscriber;
import com.mobilesolutionworks.codex.internal.doclet.ActionDoclet;
import com.mobilesolutionworks.codex.internal.doclet.ActionHookDoclet;
import com.mobilesolutionworks.codex.internal.doclet.EmitterDoclet;
import com.mobilesolutionworks.codex.internal.doclet.PropertyDoclet;
import com.mobilesolutionworks.codex.internal.doclet.PropertySubscriberDoclet;
import com.mobilesolutionworks.codex.internal.doclet.ReceiverDoclet;

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
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Created by yunarta on 18/8/15.
 */
public class AnnotationProcessor extends AbstractProcessor {

    Map<String, EmitterDoclet> mEmitterDocMap = new TreeMap<>();
    Map<String, ReceiverDoclet> mReceiverMap = new TreeMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) return true;

        Map<String, String> options = processingEnv.getOptions();
        String outputDir = options.get("codexOutput");

        if (annotations.isEmpty()) return true;

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out));
        writer = createCodexWriter(outputDir, writer);

        Set<? extends Element> elements;

        elements = env.getElementsAnnotatedWith(ActionProvider.class);
        for (Element element : elements) {
            TypeElement enclosingClass = (TypeElement) element;
            String className = enclosingClass.getQualifiedName().toString();

            EmitterDoclet emitter = mEmitterDocMap.getOrDefault(className, new EmitterDoclet());
            mEmitterDocMap.put(className, emitter);

            ActionProvider annotation = element.getAnnotation(ActionProvider.class);
            Action[] actionInfos = annotation.actions();
            for (Action action : actionInfos) {
                ActionDoclet doclet = new ActionDoclet(className, action);

                String key = doclet.key();
                if (emitter.declaredActions.containsKey(key)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, className + " has overloaded " + doclet);
                }

                emitter.declaredActions.put(key, doclet);
            }
        }

        elements = env.getElementsAnnotatedWith(ActionHook.class);
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            String className = typeElement.getQualifiedName().toString();

            ReceiverDoclet receiver = mReceiverMap.getOrDefault(className, new ReceiverDoclet());
            mReceiverMap.put(className, receiver);

            ActionHook annotation = element.getAnnotation(ActionHook.class);
            ActionHookDoclet doclet = new ActionHookDoclet(className, annotation, element);

            String signature = doclet.key();
            Set<ActionHookDoclet> doclets = receiver.declaredActionHooks.getOrDefault(signature, new TreeSet<ActionHookDoclet>());
            receiver.declaredActionHooks.put(signature, doclets);

            doclets.add(doclet);
        }

        elements = env.getElementsAnnotatedWith(Property.class);
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            String className = typeElement.getQualifiedName().toString();

            EmitterDoclet emitter = mEmitterDocMap.getOrDefault(className, new EmitterDoclet());
            mEmitterDocMap.put(className, emitter);

            Property annotation = element.getAnnotation(Property.class);
            PropertyDoclet doclet = new PropertyDoclet(className, annotation, element);

            if (emitter.declaredProperties.containsKey(doclet.key())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, className + " have duplicate declaration for property " + doclet.name);
            }

            emitter.declaredProperties.put(doclet.key(), doclet);
        }

        elements = env.getElementsAnnotatedWith(PropertySubscriber.class);
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            String className = typeElement.getQualifiedName().toString();

            ReceiverDoclet receiver = mReceiverMap.getOrDefault(className, new ReceiverDoclet());
            mReceiverMap.put(className, receiver);

            PropertySubscriber annotation = element.getAnnotation(PropertySubscriber.class);
            PropertySubscriberDoclet doclet = new PropertySubscriberDoclet(className, annotation, element);
            if (!doclet.isValid()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, className + " have invalid declaration for property subscriber " + doclet);
            }

            Set<PropertySubscriberDoclet> doclets = receiver.declaredPropertySubscribers.getOrDefault(doclet.name, new TreeSet<PropertySubscriberDoclet>());
            receiver.declaredPropertySubscribers.put(doclet.name, doclets);

            doclets.add(doclet);
        }

        XMLBuilder2 codex = XMLBuilder2.create("codex");

        XMLBuilder2 emitters = codex.e("emitters");
        XMLBuilder2 receivers = codex.e("receivers");

        XMLBuilder2 actions = codex.e("actions");
        XMLBuilder2 properties = codex.e("properties");

        Map<String, ActionDoclet> allActions = new TreeMap<>();
        Map<String, PropertyDoclet> allProperties = new TreeMap<>();
        Map<String, List<ActionHookDoclet>> allActionHooks = new TreeMap<>();
        Map<String, List<PropertySubscriberDoclet>> allPropertySubscribers = new TreeMap<>();


        for (Map.Entry<String, EmitterDoclet> entry : mEmitterDocMap.entrySet()) {
            String name = entry.getKey();
            EmitterDoclet value = entry.getValue();

            XMLBuilder2 entity = emitters.e("emitter").a("class", name);
            XMLBuilder2 iterator = entity.e("actions");
            for (ActionDoclet doclet : value.declaredActions.values()) {
                String key = doclet.key();

                if (allActionHooks.containsKey(key)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, doclet + " had same signature with " + allActions.get(key));
                }

                allActions.put(key, doclet);

                XMLBuilder2 actionTag = iterator.e("action");
                actionTag.a("name", doclet.action);
                actionTag.a("signature", doclet.signature());
                iterator = actionTag.up();
            }

            iterator = entity.e("properties");
            for (PropertyDoclet doclet : value.declaredProperties.values()) {
                String key = doclet.key();

                if (allProperties.containsKey(key)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, doclet + " had been declared, " + allProperties.get(key));
                }

                allProperties.put(key, doclet);
                XMLBuilder2 propertyTag = iterator.e("property");
                propertyTag.a("name", doclet.name);
                propertyTag.a("type", doclet.type.toString());
                propertyTag.a("method", doclet.method);
                iterator = propertyTag.up();
            }

            iterator.up();
            entity.up();
        }

        emitters.up();

        for (Map.Entry<String, ReceiverDoclet> entry : mReceiverMap.entrySet()) {
            String name = entry.getKey();
            ReceiverDoclet value = entry.getValue();

            XMLBuilder2 entity = receivers.e("receiver").a("class", name);

            XMLBuilder2 iterator = entity.e("actionHooks");
            for (Map.Entry<String, Set<ActionHookDoclet>> hooks : value.declaredActionHooks.entrySet()) {
                String key = hooks.getKey();

                Set<ActionHookDoclet> doclets = hooks.getValue();
                for (ActionHookDoclet doclet : doclets) {
                    XMLBuilder2 actionTag = iterator.e("actionHook");
                    actionTag.a("name", doclet.action);
                    actionTag.a("method", doclet.signature());
                    iterator = actionTag.up();

                }

                List<ActionHookDoclet> list = allActionHooks.getOrDefault(key, new ArrayList<ActionHookDoclet>());
                allActionHooks.put(key, list);

                list.addAll(doclets);
            }

            iterator = entity.e("propertySubscribers");
            for (Map.Entry<String, Set<PropertySubscriberDoclet>> subscribers : value.declaredPropertySubscribers.entrySet()) {
                String key = subscribers.getKey();

                Set<PropertySubscriberDoclet> doclets = subscribers.getValue();
                for (PropertySubscriberDoclet doclet : doclets) {
                    XMLBuilder2 actionTag = iterator.e("propertySubscriber");
                    actionTag.a("name", doclet.name);
                    actionTag.a("method", doclet.signature());
                    iterator = actionTag.up();
                }

                List<PropertySubscriberDoclet> list = allPropertySubscribers.getOrDefault(key, new ArrayList<PropertySubscriberDoclet>());
                allPropertySubscribers.put(key, list);

                list.addAll(doclets);
            }

            iterator.up();
            entity.up();
        }

        receivers.up();

        for (Map.Entry<String, ActionDoclet> entry : allActions.entrySet()) {
            ActionDoclet doclet = entry.getValue();

            XMLBuilder2 actionTag = actions.e("action");
            actionTag.a("name", doclet.signature());
            actionTag.a("class", doclet.className);

            List<ActionHookDoclet> hooks = allActionHooks.remove(doclet.key());
            if (hooks != null) {
                for (ActionHookDoclet hook : hooks) {
                    XMLBuilder2 hookTag = actionTag.e("hook");
//                    hookTag.a("name", hook.action);
                    hookTag.a("method", hook.signature());
                    hookTag.a("class", hook.className);
                    hookTag.up();
                }
            }

            actionTag.up();
        }
        actions.up();

        for (Map.Entry<String, PropertyDoclet> entry : allProperties.entrySet()) {
            PropertyDoclet doclet = entry.getValue();

            XMLBuilder2 propertyTag = properties.e("property");
            propertyTag.a("name", doclet.name);
            propertyTag.a("method", doclet.method);
            propertyTag.a("type", doclet.type.toString());
            propertyTag.a("class", doclet.className);

            List<PropertySubscriberDoclet> subscribers = allPropertySubscribers.remove(doclet.name);
            if (subscribers != null) {
                for (PropertySubscriberDoclet subscriber : subscribers) {
                    XMLBuilder2 hookTag = propertyTag.e("subscriber");
//                    hookTag.a("name", subscriber.name);
                    hookTag.a("method", subscriber.signature());
                    hookTag.a("owner", subscriber.className);
                    hookTag.up();
                }
            }

            propertyTag.up();
        }
        properties.up();

        XMLBuilder2 orphanHooks = codex.e("orphan-actionHooks");

        for (Map.Entry<String, List<ActionHookDoclet>> entry : allActionHooks.entrySet()) {
            List<ActionHookDoclet> doclets = entry.getValue();
            for (ActionHookDoclet doclet : doclets) {
                XMLBuilder2 hookTag = orphanHooks.e("hook");
                hookTag.a("name", doclet.action);
                hookTag.a("method", doclet.signature());
                hookTag.a("class", doclet.className);
                hookTag.up();
            }
        }
        orphanHooks.up();

        XMLBuilder2 orphanSubscriber = codex.e("orphan-subscriber");

        for (Map.Entry<String, List<PropertySubscriberDoclet>> entry : allPropertySubscribers.entrySet()) {
            List<PropertySubscriberDoclet> doclets = entry.getValue();
            for (PropertySubscriberDoclet doclet : doclets) {
                XMLBuilder2 hookTag = orphanSubscriber.e("subscriber");
                hookTag.a("name", doclet.name);
                hookTag.a("method", doclet.signature());
                hookTag.a("class", doclet.className);
                hookTag.up();
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
        supportedAnnotationTypes.add(ActionProvider.class.getCanonicalName());
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
