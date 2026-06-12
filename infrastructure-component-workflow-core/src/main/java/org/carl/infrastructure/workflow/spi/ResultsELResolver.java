package org.carl.infrastructure.workflow.spi;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;

import org.carl.infrastructure.workflow.definition.NodeResult;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

/**
 * Resolves {@code results['nodeId'].outcome} / {@code results.nodeId.outcome} style paths.
 *
 * <ul>
 *   <li>{@code base} is {@link ConditionEvaluator.ResultsView}: property is treated as a node id
 *       and {@link NodeResult} (or {@code null}) is returned via {@link
 *       ConditionEvaluator.ResultsView#get(String)}.
 *   <li>{@code base} is {@link NodeResult}: properties {@code outcome} / {@code status} / {@code
 *       payload} / {@code message} are exposed. {@code status} returns the enum name as string.
 *       Unknown properties fall through to a payload-key lookup so {@code results.x.field} reads
 *       {@code payload.get("field")}.
 * </ul>
 *
 * <p>Read-only.
 */
public final class ResultsELResolver extends ELResolver {

    @Override
    public Object getValue(ELContext c, Object base, Object property) {
        if (base instanceof ConditionEvaluator.ResultsView rv && property != null) {
            c.setPropertyResolved(true);
            return rv.get(property.toString());
        }
        if (base instanceof NodeResult nr && property != null) {
            c.setPropertyResolved(true);
            return switch (property.toString()) {
                case "outcome" -> nr.outcome();
                case "status" -> nr.status() == null ? null : nr.status().name();
                case "payload" -> nr.payload();
                case "message" -> nr.message();
                default -> {
                    Map<String, Object> p = nr.payload();
                    yield p == null ? null : p.get(property.toString());
                }
            };
        }
        return null;
    }

    @Override
    public Class<?> getType(ELContext c, Object base, Object property) {
        if (base instanceof ConditionEvaluator.ResultsView || base instanceof NodeResult) {
            c.setPropertyResolved(true);
            return Object.class;
        }
        return null;
    }

    @Override
    public void setValue(ELContext c, Object base, Object property, Object value) {
        if (base instanceof ConditionEvaluator.ResultsView || base instanceof NodeResult) {
            throw new UnsupportedOperationException(
                    "results / NodeResult is read-only in EL expressions");
        }
    }

    @Override
    public boolean isReadOnly(ELContext c, Object base, Object property) {
        if (base instanceof ConditionEvaluator.ResultsView || base instanceof NodeResult) {
            c.setPropertyResolved(true);
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("removal")
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext c, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext c, Object base) {
        return (base instanceof ConditionEvaluator.ResultsView || base instanceof NodeResult)
                ? Object.class
                : null;
    }
}
