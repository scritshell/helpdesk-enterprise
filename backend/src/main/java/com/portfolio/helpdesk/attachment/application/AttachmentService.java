package com.portfolio.helpdesk.attachment.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.portfolio.helpdesk.attachment.api.AttachmentDtos;
import com.portfolio.helpdesk.attachment.domain.Attachment;
import com.portfolio.helpdesk.attachment.infrastructure.AttachmentRepository;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;
import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.domain.TicketHistoryEntry;
import com.portfolio.helpdesk.ticket.infrastructure.TicketHistoryRepository;
import com.portfolio.helpdesk.ticket.application.TicketService;

@Service
public class AttachmentService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "txt", "csv");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "image/png", "image/jpeg", "text/plain", "text/csv", "application/vnd.ms-excel");

    private final AttachmentRepository attachmentRepository;
    private final UserAccountRepository userRepository;
    private final TicketService ticketService;
    private final TicketHistoryRepository historyRepository;
    private final Path storageRoot;
    private final long maxSizeBytes;

    public AttachmentService(AttachmentRepository attachmentRepository, UserAccountRepository userRepository,
        TicketService ticketService, TicketHistoryRepository historyRepository,
        @Value("${app.attachments.storage-path:./data/attachments}") String storagePath,
        @Value("${app.attachments.max-size-bytes:10485760}") long maxSizeBytes) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.historyRepository = historyRepository;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    @Transactional(readOnly = true)
    public List<AttachmentDtos.AttachmentResponse> list(Long ticketId, Authentication authentication) {
        ticketService.authorizedTicket(ticketId, authentication);
        return attachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AttachmentDtos.AttachmentResponse upload(Long ticketId, MultipartFile file, Authentication authentication) {
        Ticket ticket = ticketService.authorizedTicket(ticketId, authentication);
        UserAccount uploader = userRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        validate(file, extension);
        String storageKey = UUID.randomUUID() + "." + extension;
        Path target = safePath(storageKey);
        try {
            Files.createDirectories(storageRoot);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            Attachment attachment = attachmentRepository.save(new Attachment(ticket, uploader, originalName,
                file.getSize(), normalizedContentType(file.getContentType()), storageKey));
            historyRepository.save(new TicketHistoryEntry(ticket, uploader, "ATTACHMENT_ADDED", null, originalName));
            return toResponse(attachment);
        } catch (IOException | RuntimeException exception) {
            deleteQuietly(target);
            if (exception instanceof ResponseStatusException responseStatusException) throw responseStatusException;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se ha podido guardar el adjunto", exception);
        }
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(Long ticketId, Long attachmentId, Authentication authentication) {
        ticketService.authorizedTicket(ticketId, authentication);
        Attachment attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adjunto no encontrado"));
        Path path = safePath(attachment.getStorageKey());
        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado en el almacenamiento");
        }
        return new DownloadedAttachment(resource, attachment.getOriginalName(), attachment.getContentType());
    }

    private void validate(MultipartFile file, String extension) {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        if (file.getSize() > maxSizeBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido");
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de archivo no permitido");
        String contentType = normalizedContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no permitido");
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            if ((extension.equals("pdf") && !startsWith(header, "%PDF-".getBytes(StandardCharsets.US_ASCII)))
                || (extension.equals("png") && !startsWith(header, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47}))
                || ((extension.equals("jpg") || extension.equals("jpeg")) && !(header.length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff))) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El contenido no coincide con la extensión");
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha podido leer el archivo", exception);
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false;
        return true;
    }

    private Path safePath(String storageKey) {
        Path path = storageRoot.resolve(storageKey).normalize();
        if (!path.startsWith(storageRoot)) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ruta de almacenamiento no válida");
        return path;
    }

    private String safeOriginalName(String originalName) {
        String name = originalName == null ? "archivo" : Paths.get(originalName).getFileName().toString();
        name = name.replaceAll("[\\p{Cntrl}]", "_").replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();
        if (name.isBlank() || name.equals(".") || name.equals("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo no válido");
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El archivo debe tener una extensión permitida");
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizedContentType(String contentType) {
        return contentType == null ? "application/octet-stream" : contentType.split(";", 2)[0].toLowerCase(Locale.ROOT);
    }

    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) { } }

    private AttachmentDtos.AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentDtos.AttachmentResponse(attachment.getId(), attachment.getOriginalName(), attachment.getSizeBytes(), attachment.getContentType(),
            attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName(), attachment.getCreatedAt());
    }

    public record DownloadedAttachment(Resource resource, String originalName, String contentType) {}
}
