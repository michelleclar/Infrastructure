package org.carl.infra.common.exception;

import org.carl.infra.common.BizCode;
import org.carl.infra.component.web.config.exception.BizException;
import org.jboss.logging.Logger;

public class RecommendException {
    private static final Logger LOGGER = Logger.getLogger(QdrantException.class);

    public static BizException recommendQueryBuildException(String reason) {
        return biz(reason, BizCode.RECOMMEND_QUERY_BUILD);
    }

    public static BizException recommendQueryBuildException(Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return recommendQueryBuildException(throwable.getMessage());
    }

    public static BizException recommendQueryException(String reason) {
        return biz(reason, BizCode.RECOMMEND_QUERY);
    }

    public static BizException recommendQueryException(Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return recommendQueryException(throwable.getMessage());
    }

    private static BizException biz(String scenario, String reason) {
        return BizException.biz(reason, BizCode.SERVER_RECOMMEND, scenario, 500);
    }
}
