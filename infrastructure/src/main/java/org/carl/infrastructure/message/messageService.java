package org.carl.infrastructure.message;

import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "quarkus.message.enable", stringValue = "true")
public class messageService {}
