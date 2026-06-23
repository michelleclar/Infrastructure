package org.carl.infrastructure.persistence.utils;

/**
 * Neutral DDL warning or error.
 */
public record DdlIssue(String objectPath, String message) {}
