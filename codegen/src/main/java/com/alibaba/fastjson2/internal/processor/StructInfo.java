package com.alibaba.fastjson2.internal.processor;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StructInfo {
    final int modifiers;
    final boolean referenceDetect;
    final boolean smartMatch;

    String typeKey;
    int readerFeatures;
    int writerFeatures;
    final TypeElement element;
    final String name;
    final String binaryName;
    final Map<String, AttributeInfo> attributes = new LinkedHashMap<>();

    public StructInfo(
            Types types,
            TypeElement element,
            DeclaredType jsonCompiledDeclaredType,
            DeclaredType jsonTypeDeclaredType,
            String name,
            String binaryName
    ) {
        this.element = element;
        this.name = name;
        this.binaryName = binaryName;

        this.modifiers = Analysis.getModifiers(element.getModifiers());

        AnnotationMirror jsonType = null;
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            DeclaredType annotationType = mirror.getAnnotationType();
            if (types.isSameType(annotationType, jsonTypeDeclaredType)) {
                jsonType = mirror;
            }
        }

        boolean referenceDetect = true, smartMatch = true;
        if (jsonType != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : jsonType.getElementValues().entrySet()) {
                String annFieldName = entry.getKey().getSimpleName().toString();
                AnnotationValue value = entry.getValue();
                switch (annFieldName) {
                    case "disableReferenceDetect":
                        referenceDetect = !(Boolean) value.getValue();
                        break;
                    case "disableSmartMatch":
                        smartMatch = !(Boolean) value.getValue();
                        break;
                    default:
                        break;
                }
            }
        }

        this.referenceDetect = referenceDetect;
        this.smartMatch = smartMatch;
    }

    public AttributeInfo getAttributeByField(String name, VariableElement field) {
        AttributeInfo attr = attributes.get(name);
        TypeMirror type = field.asType();

        if (attr == null) {
            attr = new AttributeInfo(name, field.asType(), field, null, null, null);
            AttributeInfo origin = attributes.putIfAbsent(name, attr);
            if (origin != null) {
                attr = origin;
            }
        }

        attr.field = field;
        return attr;
    }

    public AttributeInfo getAttributeByMethod(
            String name,
            TypeMirror type,
            ExecutableElement getter,
            ExecutableElement setter
    ) {
        AttributeInfo attr = attributes.get(name);
        if (attr == null) {
            attr = new AttributeInfo(name, type, null, getter, setter, null);
            AttributeInfo origin = attributes.putIfAbsent(name, attr);
            if (origin != null) {
                attr = origin;
            }
        }

        if (getter != null) {
            attr.getMethod = getter;
        }
        if (setter != null) {
            attr.setMethod = setter;
        }

        return attr;
    }

    public List<AttributeInfo> getReaderAttributes() {
        return attributes.values()
                .stream()
                .filter(AttributeInfo::supportSet)
                .sorted()
                .collect(Collectors.toList());
    }
}
