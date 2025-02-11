package org.carl.infrastructure.broadcast;

import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "quarkus.message.enable", stringValue = "true")
public class messageService {}
