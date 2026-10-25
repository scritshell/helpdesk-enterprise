package com.portfolio.helpdesk.attachment.api;

import java.time.Instant;

public final class AttachmentDtos {
    private AttachmentDtos() {}

    public record AttachmentResponse(Long id, String originalName, long sizeBytes, String contentType,
        String uploadedBy, Instant createdAt) {}
}
