package workflow.model;

public class PublishRequest {
    private ContentPublishingStatusEnum status;
    private ContentPublishingContext context;
    private ContentPublishingEventEnum event;

    public ContentPublishingStatusEnum getStatus() {
        return status;
    }

    public PublishRequest setStatus(ContentPublishingStatusEnum status) {
        this.status = status;
        return this;
    }

    public ContentPublishingContext getContext() {
        return context;
    }

    public PublishRequest setContext(ContentPublishingContext context) {
        this.context = context;
        return this;
    }

    public ContentPublishingEventEnum getEvent() {
        return event;
    }

    public PublishRequest setEvent(ContentPublishingEventEnum event) {
        this.event = event;
        return this;
    }
}
