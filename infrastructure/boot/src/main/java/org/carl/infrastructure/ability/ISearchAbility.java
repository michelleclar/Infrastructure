package org.carl.infrastructure.ability;

import org.carl.infrastructure.search.plugins.es.IESOperations;

// TODO: complete
public interface ISearchAbility {
    IESOperations getSearchOperations();
}
