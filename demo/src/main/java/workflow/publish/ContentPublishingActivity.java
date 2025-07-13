package workflow.publish;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import workflow.model.ContentPublishingContext;
import workflow.model.ContentPublishingStatusEnum;

@ActivityInterface
public interface ContentPublishingActivity {
    /** Submit content for review */
    @ActivityMethod
    void submitForReview(ContentPublishingContext context);

    /** Approve content */
    @ActivityMethod
    void approveContent(ContentPublishingContext context);

    /** Reject content */
    @ActivityMethod
    void rejectContent(ContentPublishingContext context);

    /** Withdraw rejection */
    @ActivityMethod
    void withdrawRejection(ContentPublishingContext context);

    /** Revise content */
    @ActivityMethod
    void reviseContent(ContentPublishingContext context);

    /** Publish content */
    @ActivityMethod
    void publishContent(ContentPublishingContext context);

    /** Unpublish content */
    @ActivityMethod
    void unpublishContent(ContentPublishingContext context);

    /** Republish content */
    @ActivityMethod
    void republishContent(ContentPublishingContext context);

    @ActivityMethod
    ContentPublishingStatusEnum currentStatus(ContentPublishingContext context);

    /** Log state transition */
    @ActivityMethod
    void logStateTransition(
            String entityId, String fromState, String toState, String event, String reason);

    /** Send notification */
    @ActivityMethod
    void sendNotification(
            String entityId, String recipientId, String notificationType, String message);
}
