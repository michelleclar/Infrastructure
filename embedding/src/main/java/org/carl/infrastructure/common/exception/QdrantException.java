package org.carl.infrastructure.common.exception;

import org.carl.infrastructure.common.BizCode;
import org.carl.infrastructure.component.web.config.exception.BizException;
import org.jboss.logging.Logger;

public class QdrantException {

    private static final Logger LOGGER = Logger.getLogger(QdrantException.class);

    public static BizException qdrantUpsertException(String reason) {
        return biz(reason, BizCode.QDRANT_POINT_UPSERT);
    }

    public static BizException qdrantUpsertException(Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return qdrantUpsertException(throwable.getMessage());
    }

    public static BizException qdrantQueryException(String reason) {
        return biz(reason, BizCode.QDRANT_POINT_QUERY);
    }

    public static BizException qdrantQueryException(Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return qdrantQueryException(throwable.getMessage());
    }

    public static BizException qdrantCollectionCreateException(String reason) {
        return biz(reason, BizCode.QDRANT_COLLECTION_QUERY);
    }

    public static BizException qdrantCollectionCreateException(Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return qdrantCollectionCreateException(throwable.getMessage());
    }

    private static BizException biz(String scenario, String reason) {
        return BizException.biz(reason, BizCode.MIDDLEWARE_QDRANT, scenario, 99999);
    }
}
