package workflow.publish;

import jakarta.enterprise.context.ApplicationScoped;

import workflow.model.ContentPublishingContext;
import workflow.model.ContentPublishingStatusEnum;

import java.sql.Timestamp;

@ApplicationScoped
public class ContentPublishingActivityImpl implements ContentPublishingActivity {

    @Override
    public void submitForReview(ContentPublishingContext context) {
        System.out.println("=== Submitting content for review ===");
        System.out.println("Entity ID: " + context.getEntityId());
        System.out.println("Author: " + context.getAuthorId());
        System.out.println("Content: " + context.getContent());

        // Simulate business logic
        context.setSubmitTime(new Timestamp(System.currentTimeMillis()));

        // Log transition
        logStateTransition(
                context.getEntityId(),
                "DRAFT",
                "SUBMITTED",
                "SUBMIT",
                "Content submitted for review");

        // Send notification to reviewers
        sendNotification(
                context.getEntityId(),
                "reviewer-team",
                "REVIEW_REQUEST",
                "New content submitted for review by " + context.getAuthorId());
    }

    @Override
    public void approveContent(ContentPublishingContext context) {
        System.out.println("=== Approving content ===");
        System.out.println("Entity ID: " + context.getEntityId());
        System.out.println("Reviewer: " + context.getReviewerId());

        // Simulate approval process
        // Clear any previous rejection
        context.setRejectReason(null);

        // Start content processing (format conversion, optimization)
        System.out.println("Starting content processing and optimization...");

        logStateTransition(
                context.getEntityId(),
                "SUBMITTED",
                "PROCESSING",
                "APPROVE",
                "Content approved by reviewer " + context.getReviewerId());

        sendNotification(
                context.getEntityId(),
                context.getAuthorId(),
                "CONTENT_APPROVED",
                "Your content has been approved and is being processed");
    }

    @Override
    public void rejectContent(ContentPublishingContext context) {
        System.out.println("=== Rejecting content ===");
        System.out.println("Entity ID: " + context.getEntityId());
        System.out.println("Reviewer: " + context.getReviewerId());
        System.out.println("Reject Reason: " + context.getRejectReason());

        // Simulate rejection process
        // Store rejection details in database

        logStateTransition(
                context.getEntityId(),
                "SUBMITTED",
                "REJECTED",
                "REJECT",
                "Content rejected: " + context.getRejectReason());

        sendNotification(
                context.getEntityId(),
                context.getAuthorId(),
                "CONTENT_REJECTED",
                "Your content has been rejected. Reason: " + context.getRejectReason());
    }

    @Override
    public void withdrawRejection(ContentPublishingContext context) {
        System.out.println("=== Withdrawing rejection ===");
        System.out.println("Entity ID: " + context.getEntityId());
        System.out.println("Withdrawn by: " + context.getAuth());

        // Clear rejection reason
        context.setRejectReason(null);

        logStateTransition(
                context.getEntityId(),
                "REJECTED",
                "SUBMITTED",
                "WITHDRAW",
                "Rejection withdrawn by " + context.getAuth());

        sendNotification(
                context.getEntityId(),
                context.getAuthorId(),
                "REJECTION_WITHDRAWN",
                "The rejection of your content has been withdrawn");
    }

    @Override
    public void reviseContent(ContentPublishingContext context) {
        System.out.println("=== Revising content ===");
        System.out.println("Entity ID: " + context.getEntityId());
        System.out.println("Author: " + context.getAuthorId());

        // Simulate revision process
        context.setRejectReason(null); // Clear rejection reason
        int currentRevision =
                Integer.parseInt(
                        context.getRevisionCount() != null ? context.getRevisionCount() : "0");
        context.setRevisionCount(String.valueOf(currentRevision + 1));

        logStateTransition(
                context.getEntityId(),
                "REJECTED",
                "DRAFT",
                "REVISE",
                "Content revised (revision #" + context.getRevisionCount() + ")");

        sendNotification(
                context.getEntityId(),
                context.getAuthorId(),
                "CONTENT_REVISED",
                "Content has been moved back to draft for revision");
    }

    @Override
    public void publishContent(ContentPublishingContext context) {
        System.out.println("=== Publishing content ===");
        System.out.println("Entity ID: " + context.getEntityId());

        // Simulate publishing process
        context.markAsPublished(context.getAuth());

        // Simulate content distribution
        System.out.println("Distributing content to CDN...");
        System.out.println("Updating search index...");
        System.out.println("Generating social media previews...");

        logStateTransition(
                context.getEntityId(),
                "PROCESSING",
                "PUBLISHED",
                "PUBLISH",
                "Content successfully published");

        sendNotification(
                context.getEntityId(),
                context.getAuthorId(),
                "CONTENT_PUBLISHED",
                "Your content has been published successfully");
    }

    @Override
    public void unpublishContent(ContentPublishingContext context) {
        System.out.println("=== Unpublishing content ===");
        System.out.println("Entity ID: " + context.getEntityId());
        System.out.println("Unpublished by: " + context.getAuth());

        // Simulate unpublishing process
        context.markAsUnpublished(
                context.getAuth(), "Content unpublished by " + context.getAuthorId());

        // Remove from CDN and search index
        System.out.println("Removing content from CDN...");
        System.out.println("Removing from search index...");

        logStateTransition(
                context.getEntityId(),
                "PUBLISHED",
                "UNPUBLISHED",
                "UNPUBLISH",
                "Content unpublished by " + context.getAuth());

        sendNotification(
                context.getEntityId(),
                context.getAuthorId(),
                "CONTENT_UNPUBLISHED",
                "Your content has been unpublished");
    }

    @Override
    public void republishContent(ContentPublishingContext context) {
        System.out.println("=== Republishing content ===");
        System.out.println("Entity ID: " + context.getEntityId());

        // Simulate republishing process
        context.markAsPublished(context.getAuth());

        // Re-distribute content
        System.out.println("Re-distributing content to CDN...");
        System.out.println("Re-indexing content...");

        logStateTransition(
                context.getEntityId(),
                "UNPUBLISHED",
                "PUBLISHED",
                "PUBLISH",
                "Content republished");

        sendNotification(
                context.getEntityId(),
                context.getAuthorId(),
                "CONTENT_REPUBLISHED",
                "Your content has been republished");
    }

    @Override
    public ContentPublishingStatusEnum currentStatus(ContentPublishingContext context) {
        return ContentPublishingStatusEnum.DRAFT;
    }

    @Override
    public void logStateTransition(
            String entityId, String fromState, String toState, String event, String reason) {

        System.out.println("=== State Transition Log ===");
        System.out.println("Entity ID: " + entityId);
        System.out.println("Transition: " + fromState + " -> " + toState);
        System.out.println("Event: " + event);
        System.out.println("Reason: " + reason);
        System.out.println("Timestamp: " + new Timestamp(System.currentTimeMillis()));

        // In real implementation, save to database
        // stateTransitionRepository.save(new StateTransitionRecord(...));
    }

    @Override
    public void sendNotification(
            String entityId, String recipientId, String notificationType, String message) {
        System.out.println("=== Sending Notification ===");
        System.out.println("Entity ID: " + entityId);
        System.out.println("Recipient: " + recipientId);
        System.out.println("Type: " + notificationType);
        System.out.println("Message: " + message);

        // In real implementation, send via notification service
        // notificationService.send(recipientId, notificationType, message);
    }
}
