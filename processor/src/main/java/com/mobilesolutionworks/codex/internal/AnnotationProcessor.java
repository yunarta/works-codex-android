package com.mobilesolutionworks.codex.internal;

import com.jamesmurty.utils.XMLBuilder2;
import com.mobilesolutionworks.codex.Action;
import com.mobilesolutionworks.codex.ActionHook;
import com.mobilesolutionworks.codex.Property;
import com.mobilesolutionworks.codex.PropertySubscriber;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
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

    class CodexInfo {

        public Set<String> declaredActions    = new TreeSet<>();
        public Set<String> declaredProperties = new TreeSet<>();
    }

    Map<String, CodexInfo> mCodexInfoMap = new TreeMap<>();
//    Map<String, Set<String>>

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

            CodexInfo info = mCodexInfoMap.getOrDefault(key, new CodexInfo());
            mCodexInfoMap.put(key, info);

            List<String> c = Arrays.asList(annotation.name());
            System.out.println("c = " + c);
            System.out.println("info = " + info);

            info.declaredActions.addAll(c);
        }

//        elements = env.getElementsAnnotatedWith(ActionHook.class);
//        for (Element element : elements) {
//            ActionHook annotation = element.getAnnotation(ActionHook.class);
//            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
//
//            String key = typeElement.getQualifiedName().toString();
//
//            CodexInfo info = mCodexInfoMap.getOrDefault(key, new CodexInfo());
//            mCodexInfoMap.put(key, info);
//
//            if (!info.declaredProperties.add(annotation.name())) {
//                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, key + " have duplicate declaration for property " + annotation.name());
//            }
//        }

        elements = env.getElementsAnnotatedWith(Property.class);
        for (Element element : elements) {
            Property annotation = element.getAnnotation(Property.class);
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();

            String key = typeElement.getQualifiedName().toString();

            CodexInfo info = mCodexInfoMap.getOrDefault(key, new CodexInfo());
            mCodexInfoMap.put(key, info);

            if (!info.declaredProperties.add(annotation.name())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, key + " have duplicate declaration for property " + annotation.name());
            }
        }

        XMLBuilder2 codex = XMLBuilder2.create("codex");

        XMLBuilder2 entities = codex.e("entities");
        XMLBuilder2 actions = codex.e("actions");
        XMLBuilder2 properties = codex.e("properties");

        Map<String, String> allActions = new TreeMap<>();
        Map<String, String> allProperties = new TreeMap<>();
        for (Map.Entry<String, CodexInfo> entry : mCodexInfoMap.entrySet()) {
            String name = entry.getKey();
            CodexInfo value = entry.getValue();

            XMLBuilder2 entity = entities.e("entity").a("type", name);
            XMLBuilder2 iterator = entity.e("actions");
            for (String action : value.declaredActions) {
                boolean conflict = allActions.containsKey(action);
                if (conflict) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, action + " had been declared in " + allActions.get(action));
                }

                allActions.put(action, name);

                XMLBuilder2 actionTag = iterator.e("action");
                actionTag.a("name", action);
                iterator = actionTag.up();
            }

            iterator = entity.e("properties");
            for (String property : value.declaredProperties) {
                boolean conflict = allProperties.containsKey(property);
                if (conflict) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, property + " had been declared in " + allProperties.get(property));
                }

                allProperties.put(property, name);

                XMLBuilder2 propertyTag = iterator.e("property");
                propertyTag.a("name", property);
                iterator = propertyTag.up();
            }

            iterator.up();
            entity.up();
        }

        entities.up();

        for (Map.Entry<String, String> entry : allActions.entrySet()) {
            XMLBuilder2 actionTag = actions.e("action");

            actionTag.a("name", entry.getKey());
            actionTag.a("type", entry.getValue());

            actionTag.up();
        }
        actions.up();


        for (Map.Entry<String, String> entry : allProperties.entrySet()) {
            XMLBuilder2 propertyTag = properties.e("property");

            propertyTag.a("name", entry.getKey());
            propertyTag.a("type", entry.getValue());

            propertyTag.up();
        }
        properties.up();

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
