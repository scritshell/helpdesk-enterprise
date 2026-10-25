package com.portfolio.helpdesk.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import com.portfolio.helpdesk.attachment.api.AttachmentDtos;
import com.portfolio.helpdesk.attachment.domain.Attachment;
import com.portfolio.helpdesk.attachment.infrastructure.AttachmentRepository;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;
import com.portfolio.helpdesk.category.domain.Category;
import com.portfolio.helpdesk.ticket.application.TicketService;
import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.infrastructure.TicketHistoryRepository;
import com.portfolio.helpdesk.attachment.application.AttachmentService;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @Mock AttachmentRepository attachmentRepository;
    @Mock UserAccountRepository userRepository;
    @Mock TicketService ticketService;
    @Mock TicketHistoryRepository historyRepository;
    @Mock Authentication authentication;
    @TempDir Path tempDir;
    private AttachmentService service;
    private Ticket ticket;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        service = new AttachmentService(attachmentRepository, userRepository, ticketService, historyRepository, tempDir.toString(), 10_000_000);
        user = new UserAccount("user@test.local", "hash", "Test", "User");
        ticket = new Ticket("Ticket", "Description", new Category("Correo", ""), user);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(ticketService.authorizedTicket(1L, authentication)).thenReturn(ticket);
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(attachmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesRandomInternalNameAndSanitizedOriginalMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "../../evidencia.pdf", "application/pdf", "%PDF-test".getBytes());

        AttachmentDtos.AttachmentResponse response = service.upload(1L, file, authentication);

        assertEquals("evidencia.pdf", response.originalName());
        try (var files = Files.list(tempDir)) {
            assertTrue(files.findAny().isPresent());
        }
    }
}
