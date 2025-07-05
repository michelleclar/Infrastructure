package org.carl.infrastructure.dto;

import java.util.List;

public abstract class Scope extends DTO {
    List<String> includes;
    List<String> excludes;
}
