package com.portfolio.helpdesk.attachment.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.helpdesk.attachment.domain.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    Optional<Attachment> findByIdAndTicketId(Long id, Long ticketId);
}
