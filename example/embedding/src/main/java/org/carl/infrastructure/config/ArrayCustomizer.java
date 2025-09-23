package org.carl.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

import io.quarkus.jackson.ObjectMapperCustomizer;

import jakarta.inject.Singleton;

@Singleton
public class ArrayCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper
                .coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.EmptyArray, CoercionAction.AsNull);
    }
}
