# Product Definition

## Project Name

基础设施组件

## Description

为微服务提供可复用的通用基础设施能力，包含 Quarkus 集成模块和独立库模块。

## Problem Statement

各服务独立集成第三方中间件，版本混乱、配置分散、维护成本高。

## Target Users

需要快速集成中间件能力的 Java 开发者。

## Key Goals

- 提供开箱即用的 Quarkus 扩展，降低各服务重复开发成本
- 隔离框架依赖，支持独立库在非 Quarkus 项目中使用
- 统一中间件集成标准，提升组件可复用性与可维护性

## Module Structure

The library is split into two categories:

| Category | Description |
|----------|-------------|
| **Quarkus Integration Modules** (`infrastructure-component-quarkus/`) | Depend on Quarkus; used as Quarkus extensions |
| **Standalone Library Modules** | No Quarkus dependency; usable in any Java project |
