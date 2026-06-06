package org.carl.infrastructure.workflow.spi;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

/**
 * Resolves dot-path access on {@link JsonNode} for EL expressions like {@code
 * ${businessData.amount}}. Terminal scalar nodes are unwrapped to boolean / double / text;
 * container nodes (object / array) are kept as {@link JsonNode} so the next access continues to
 * flow through this resolver.
 *
 * <p>Read-only — {@link #setValue} throws {@link UnsupportedOperationException}.
 */
public final class JsonNodeELResolver extends ELResolver {

    @Override
    public Object getValue(ELContext c, Object base, Object property) {
        if (!(base instanceof JsonNode jn) || property == null) {
            return null;
        }
        c.setPropertyResolved(true);
        JsonNode child = jn.get(property.toString());
        if (child == null || child.isMissingNode() || child.isNull()) {
            return null;
        }
        if (child.isBoolean()) {
            return child.booleanValue();
        }
        if (child.isNumber()) {
            return child.doubleValue();
        }
        if (child.isTextual()) {
            return child.textValue();
        }
        return child;
    }

    @Override
    public Class<?> getType(ELContext c, Object base, Object property) {
        if (base instanceof JsonNode) {
            c.setPropertyResolved(true);
            return Object.class;
        }
        return null;
    }

    @Override
    public void setValue(ELContext c, Object base, Object property, Object value) {
        if (base instanceof JsonNode) {
            throw new UnsupportedOperationException("JsonNode is read-only in EL expressions");
        }
    }

    @Override
    public boolean isReadOnly(ELContext c, Object base, Object property) {
        if (base instanceof JsonNode) {
            c.setPropertyResolved(true);
            return true;
        }
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext c, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext c, Object base) {
        return base instanceof JsonNode ? Object.class : null;
    }
}
