package com.portfolio.helpdesk.attachment.api;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import com.portfolio.helpdesk.attachment.application.AttachmentService;
import com.portfolio.helpdesk.attachment.application.AttachmentService.DownloadedAttachment;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public List<AttachmentDtos.AttachmentResponse> list(@PathVariable Long ticketId, Authentication authentication) {
        return attachmentService.list(ticketId, authentication);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDtos.AttachmentResponse> upload(@PathVariable Long ticketId,
        @RequestParam("file") MultipartFile file, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(ticketId, file, authentication));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long ticketId, @PathVariable Long attachmentId,
        Authentication authentication) {
        DownloadedAttachment downloaded = attachmentService.download(ticketId, attachmentId, authentication);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(downloaded.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .contentType(MediaType.parseMediaType(downloaded.contentType())).body(downloaded.resource());
    }
}
