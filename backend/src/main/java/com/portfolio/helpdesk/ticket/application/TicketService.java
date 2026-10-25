package com.portfolio.helpdesk.ticket.application;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.portfolio.helpdesk.auth.domain.RoleName;
import com.portfolio.helpdesk.auth.domain.UserAccount;
import com.portfolio.helpdesk.auth.infrastructure.UserAccountRepository;
import com.portfolio.helpdesk.category.domain.Category;
import com.portfolio.helpdesk.category.infrastructure.CategoryRepository;
import com.portfolio.helpdesk.ticket.api.TicketDtos.AddCommentRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.AssignTicketRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.CategorySummary;
import com.portfolio.helpdesk.ticket.api.TicketDtos.ChangePriorityRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.ChangeStatusRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.CommentResponse;
import com.portfolio.helpdesk.ticket.api.TicketDtos.CreateTicketRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.HistoryResponse;
import com.portfolio.helpdesk.ticket.api.TicketDtos.TicketResponse;
import com.portfolio.helpdesk.ticket.api.TicketDtos.UserSummary;
import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.domain.TicketComment;
import com.portfolio.helpdesk.ticket.domain.TicketHistoryEntry;
import com.portfolio.helpdesk.ticket.domain.TicketPriority;
import com.portfolio.helpdesk.ticket.domain.TicketStatus;
import com.portfolio.helpdesk.ticket.infrastructure.TicketCommentRepository;
import com.portfolio.helpdesk.ticket.infrastructure.TicketHistoryRepository;
import com.portfolio.helpdesk.ticket.infrastructure.TicketRepository;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserAccountRepository userRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketHistoryRepository historyRepository;

    public TicketService(TicketRepository ticketRepository, CategoryRepository categoryRepository,
        UserAccountRepository userRepository, TicketCommentRepository commentRepository,
        TicketHistoryRepository historyRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, Authentication authentication) {
        UserAccount reporter = currentUser(authentication);
        Category category = categoryRepository.findById(request.categoryId())
            .filter(Category::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        Ticket ticket = new Ticket(request.title().trim(), request.description().trim(), category, reporter);
        if (request.priority() != null) ticket.changePriority(request.priority());
        Ticket saved = ticketRepository.save(ticket);
        historyRepository.save(new TicketHistoryEntry(saved, reporter, "TICKET_CREATED", null, saved.getStatus().name()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> list(Authentication authentication) {
        UserAccount user = currentUser(authentication);
        List<Ticket> tickets = hasRole(user, RoleName.ADMIN)
            ? ticketRepository.findAll()
            : hasRole(user, RoleName.TECHNICIAN)
                ? ticketRepository.findByTechnicianIdOrderByCreatedAtDesc(user.getId())
                : ticketRepository.findByReporterIdOrderByCreatedAtDesc(user.getId());
        return tickets.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> queue(Authentication authentication) {
        UserAccount user = currentUser(authentication);
        if (!hasRole(user, RoleName.TECHNICIAN) && !hasRole(user, RoleName.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar la cola de trabajo");
        }
        return ticketRepository.findByTechnicianIsNullOrderByCreatedAtAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, Authentication authentication) {
        Ticket ticket = authorizedTicket(id, authentication);
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Ticket authorizedTicket(Long id, Authentication authentication) {
        Ticket ticket = find(id);
        assertCanRead(ticket, currentUser(authentication));
        return ticket;
    }

    @Transactional
    public TicketResponse changeStatus(Long id, ChangeStatusRequest request, Authentication authentication) {
        Ticket ticket = find(id);
        UserAccount actor = currentUser(authentication);
        assertCanManage(ticket, actor);
        TicketStatus oldStatus = ticket.getStatus();
        try {
            ticket.changeStatus(request.status());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
        historyRepository.save(new TicketHistoryEntry(ticket, actor, "STATUS_CHANGED", oldStatus.name(), request.status().name()));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse changePriority(Long id, ChangePriorityRequest request, Authentication authentication) {
        Ticket ticket = find(id);
        UserAccount actor = currentUser(authentication);
        assertCanManage(ticket, actor);
        TicketPriority oldPriority = ticket.getPriority();
        ticket.changePriority(request.priority());
        historyRepository.save(new TicketHistoryEntry(ticket, actor, "PRIORITY_CHANGED", oldPriority.name(), request.priority().name()));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse assign(Long id, AssignTicketRequest request, Authentication authentication) {
        Ticket ticket = find(id);
        UserAccount actor = currentUser(authentication);
        if (!hasRole(actor, RoleName.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes asignar incidencias");
        }
        UserAccount technician = userRepository.findById(request.technicianId())
            .filter(user -> hasRole(user, RoleName.TECHNICIAN))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado"));
        Long previousId = ticket.getTechnician() == null ? null : ticket.getTechnician().getId();
        ticket.assignTo(technician);
        historyRepository.save(new TicketHistoryEntry(ticket, actor, "TECHNICIAN_ASSIGNED",
            previousId == null ? null : previousId.toString(), technician.getId().toString()));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse claim(Long id, Authentication authentication) {
        Ticket ticket = find(id);
        UserAccount actor = currentUser(authentication);
        if (!hasRole(actor, RoleName.TECHNICIAN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un técnico puede reclamar incidencias");
        }
        if (ticket.getTechnician() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La incidencia ya está asignada a un técnico");
        }
        ticket.assignTo(actor);
        String actorReference = actor.getId() == null ? actor.getEmail() : actor.getId().toString();
        historyRepository.save(new TicketHistoryEntry(ticket, actor, "TECHNICIAN_CLAIMED", null, actorReference));
        return toResponse(ticket);
    }

    @Transactional
    public CommentResponse addComment(Long id, AddCommentRequest request, Authentication authentication) {
        Ticket ticket = find(id);
        UserAccount author = currentUser(authentication);
        assertCanRead(ticket, author);
        TicketComment comment = commentRepository.save(new TicketComment(ticket, author, request.body().trim()));
        historyRepository.save(new TicketHistoryEntry(ticket, author, "COMMENT_ADDED", null, "comment:" + comment.getId()));
        return toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long id, Authentication authentication) {
        Ticket ticket = find(id);
        assertCanRead(ticket, currentUser(authentication));
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(id).stream().map(this::toCommentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long id, Authentication authentication) {
        Ticket ticket = find(id);
        assertCanRead(ticket, currentUser(authentication));
        return historyRepository.findByTicketIdOrderByCreatedAtAsc(id).stream().map(this::toHistoryResponse).toList();
    }

    private Ticket find(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada"));
    }

    private void assertCanRead(Ticket ticket, UserAccount user) {
        if (hasRole(user, RoleName.ADMIN) || Objects.equals(ticket.getReporter().getId(), user.getId())
            || ticket.getTechnician() != null && Objects.equals(ticket.getTechnician().getId(), user.getId())) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar esta incidencia");
    }

    private void assertCanManage(Ticket ticket, UserAccount user) {
        if (hasRole(user, RoleName.ADMIN) || ticket.getTechnician() != null && Objects.equals(ticket.getTechnician().getId(), user.getId())) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes gestionar esta incidencia");
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private boolean hasRole(UserAccount user, RoleName role) {
        return user.getRoles().stream().anyMatch(item -> item.getName() == role);
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getStatus(), ticket.getPriority(),
            new CategorySummary(ticket.getCategory().getId(), ticket.getCategory().getName()), toUserSummary(ticket.getReporter()),
            ticket.getTechnician() == null ? null : toUserSummary(ticket.getTechnician()), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    private CommentResponse toCommentResponse(TicketComment comment) {
        return new CommentResponse(comment.getId(), comment.getBody(), toUserSummary(comment.getAuthor()), comment.getCreatedAt());
    }

    private HistoryResponse toHistoryResponse(TicketHistoryEntry entry) {
        return new HistoryResponse(entry.getId(), entry.getAction(), entry.getOldValue(), entry.getNewValue(),
            toUserSummary(entry.getActor()), entry.getCreatedAt());
    }

    private UserSummary toUserSummary(UserAccount user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
