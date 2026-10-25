package com.portfolio.helpdesk.attachment.domain;

import java.time.Instant;

import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.ticket.domain.Ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private UserAccount uploadedBy;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "storage_key", nullable = false, unique = true, length = 120)
    private String storageKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Attachment() {}

    public Attachment(Ticket ticket, UserAccount uploadedBy, String originalName, long sizeBytes,
        String contentType, String storageKey) {
        this.ticket = ticket;
        this.uploadedBy = uploadedBy;
        this.originalName = originalName;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.storageKey = storageKey;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public UserAccount getUploadedBy() { return uploadedBy; }
    public String getOriginalName() { return originalName; }
    public long getSizeBytes() { return sizeBytes; }
    public String getContentType() { return contentType; }
    public String getStorageKey() { return storageKey; }
    public Instant getCreatedAt() { return createdAt; }
}
