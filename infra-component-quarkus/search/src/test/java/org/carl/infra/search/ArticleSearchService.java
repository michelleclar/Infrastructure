package org.carl.infra.search;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infra.search.ability.ISearchAbility;
import org.carl.infra.search.plugins.es.ESService;
import org.carl.infra.search.plugins.es.IESOperations;

/** README 文档化的业务接入方式：实现 ISearchAbility，返回注入的 ESService。 */
@ApplicationScoped
public class ArticleSearchService implements ISearchAbility {

    @Inject ESService esService;

    @Override
    public IESOperations getSearchOperations() {
        return esService;
    }
}
