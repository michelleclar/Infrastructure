package org.carl.domain.qdrant.filter;

public enum FilterStrategy {
    match,
    range,
    geo_bounding_box,
    geo_radius,
    geo_polygon,
    values_count,
    is_empty,
    is_null
}
