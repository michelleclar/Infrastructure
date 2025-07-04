package org.carl.infrastructure.common.exception;

import org.carl.infrastructure.common.BizCode;
import org.carl.infrastructure.component.web.config.exception.BizException;
import org.jboss.logging.Logger;

public class EmbeddingException {

    private static final Logger LOGGER = Logger.getLogger(EmbeddingException.class);

    public static BizException embeddingTextToVectorException(String reason) {
        return biz(reason, BizCode.EMBEDDING_TEXT_VECTOR);
    }

    public static BizException embeddingTextToVectorException(Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return embeddingTextToVectorException(throwable.getMessage());
    }

    public static BizException embeddingFaceToVectorException(String reason) {
        return biz(reason, BizCode.EMBEDDING_FACE_VECTOR);
    }

    public static BizException embeddingFaceToVectorException(Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return embeddingFaceToVectorException(throwable.getMessage());
    }

    private static BizException biz(String scenario, String reason) {
        return BizException.biz(reason, BizCode.MIDDLEWARE_EMBEDDING, scenario, 99999);
    }
}
