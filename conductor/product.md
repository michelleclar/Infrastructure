# Product Definition

## Project Name

Infrastructure Components

## Description

A multi-module Java library that packages Quarkus integrations and standalone utilities for backend services.

## Problem Statement

Microservice teams repeatedly reimplement the same infrastructure concerns (auth, caching, messaging, persistence) — this library centralizes and standardizes those solutions.

## Target Users

Internal platform/infrastructure teams maintaining shared service capabilities.

## Key Goals

1. Provide Quarkus-integrated components for Quarkus-based microservices — delivered as Quarkus extensions that plug cleanly into the Quarkus CDI lifecycle.
2. Provide standalone modules usable in any Java project without a Quarkus dependency — framework-agnostic libraries that can be consumed by non-Quarkus services.

## Module Structure

The library is split into two categories:

| Category | Description |
|----------|-------------|
| **Quarkus Integration Modules** (`infrastructure-component-quarkus/`) | Depend on Quarkus; used as Quarkus extensions |
| **Standalone Library Modules** | No Quarkus dependency; usable in any Java project |
