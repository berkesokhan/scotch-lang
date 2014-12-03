package me.qmx.jitescript;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.FieldNode;

@SuppressWarnings("unchecked")
public class FieldDefinition {

    private final String fieldName;
    private final int modifiers;
    private final String signature;
    private final Object value;
    private final List<AnnotationData> annotations;

    public FieldDefinition(String fieldName, int modifiers, String signature, Object value) {
        this.fieldName = fieldName;
        this.modifiers = modifiers;
        this.signature = signature;
        this.value = value;
        this.annotations = new ArrayList<>();
    }

    public FieldNode getFieldNode() {
        FieldNode node = new FieldNode(modifiers, fieldName, signature, null, value);
        node.visibleAnnotations = new ArrayList<AnnotationData>();
        node.visibleAnnotations.addAll(annotations.stream().map(AnnotationData::getNode).collect(toList()));
        return node;
    }

    public FieldDefinition addAnnotation(AnnotationData annotation) {
        annotations.add(annotation);
        return this;
    }
}
