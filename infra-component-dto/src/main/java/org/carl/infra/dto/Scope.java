package org.carl.infra.dto;

import java.util.List;

public abstract class Scope extends DTO {
    List<String> includes;
    List<String> excludes;
}
