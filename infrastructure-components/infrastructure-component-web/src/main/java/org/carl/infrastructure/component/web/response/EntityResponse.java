package org.carl.infrastructure.component.web.response;

public class EntityResponse extends Entity {

    private boolean success;

    private String errCode;

    private String errMessage;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    @Override
    public String toString() {
        return "Response  [success="
                + success
                + ", errCode="
                + errCode
                + ", errMessage="
                + errMessage
                + "]";
    }

    public static EntityResponse buildSuccess() {
        EntityResponse entityResponse = new EntityResponse();
        entityResponse.setSuccess(true);
        return entityResponse;
    }

    public static EntityResponse buildFailure(String errCode, String errMessage) {
        EntityResponse entityResponse = new EntityResponse();
        entityResponse.setSuccess(false);
        entityResponse.setErrCode(errCode);
        entityResponse.setErrMessage(errMessage);
        return entityResponse;
    }
}
