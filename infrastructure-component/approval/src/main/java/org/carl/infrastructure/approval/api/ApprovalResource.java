package org.carl.infrastructure.approval.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.carl.infrastructure.approval.core.ApprovalService;
import org.carl.infrastructure.approval.user.UserContext;

@Path("/approval")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApprovalResource {

    @Inject ApprovalService service;

    @POST
    @Path("/process/start")
    public Response start(@HeaderParam("X-User-Id") String userId, ApprovalStartRequest request) {
        requireUser(userId);
        UserContext.setCurrentUserId(userId);
        try {
            long instanceId = service.startProcess(request.getBizKey(), request.getNodes());
            return Response.ok(new ApprovalStartResponse(instanceId)).build();
        } finally {
            UserContext.clear();
        }
    }

    @POST
    @Path("/task/{taskId}/approve")
    public Response approve(
            @HeaderParam("X-User-Id") String userId,
            @PathParam("taskId") long taskId,
            ApprovalActionRequest request) {
        requireUser(userId);
        UserContext.setCurrentUserId(userId);
        try {
            service.approveTask(taskId, request == null ? null : request.getComment());
            return Response.ok().build();
        } finally {
            UserContext.clear();
        }
    }

    @POST
    @Path("/task/{taskId}/back")
    public Response back(
            @HeaderParam("X-User-Id") String userId,
            @PathParam("taskId") long taskId,
            ApprovalActionRequest request) {
        requireUser(userId);
        UserContext.setCurrentUserId(userId);
        try {
            service.backTask(taskId, request == null ? null : request.getComment());
            return Response.ok().build();
        } finally {
            UserContext.clear();
        }
    }

    @POST
    @Path("/task/{taskId}/transfer")
    public Response transfer(
            @HeaderParam("X-User-Id") String userId,
            @PathParam("taskId") long taskId,
            ApprovalTransferRequest request) {
        requireUser(userId);
        if (request == null || request.getToUserId() == null || request.getToUserId().isBlank()) {
            throw new WebApplicationException("toUserId is required", Response.Status.BAD_REQUEST);
        }
        UserContext.setCurrentUserId(userId);
        try {
            service.transferTask(taskId, request.getToUserId(), request.getComment());
            return Response.ok().build();
        } finally {
            UserContext.clear();
        }
    }

    @GET
    @Path("/tasks/todo")
    public Response todo(@HeaderParam("X-User-Id") String userId) {
        requireUser(userId);
        UserContext.setCurrentUserId(userId);
        try {
            return Response.ok(service.listTodo()).build();
        } finally {
            UserContext.clear();
        }
    }

    @GET
    @Path("/tasks/done")
    public Response done(@HeaderParam("X-User-Id") String userId) {
        requireUser(userId);
        UserContext.setCurrentUserId(userId);
        try {
            return Response.ok(service.listDone()).build();
        } finally {
            UserContext.clear();
        }
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new WebApplicationException("X-User-Id is required", Response.Status.BAD_REQUEST);
        }
    }
}
