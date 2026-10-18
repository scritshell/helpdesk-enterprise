package com.portfolio.helpdesk.ticket.application;

import java.util.List;

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
import com.portfolio.helpdesk.ticket.api.TicketDtos.AssignTicketRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.CategorySummary;
import com.portfolio.helpdesk.ticket.api.TicketDtos.ChangePriorityRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.ChangeStatusRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.CreateTicketRequest;
import com.portfolio.helpdesk.ticket.api.TicketDtos.TicketResponse;
import com.portfolio.helpdesk.ticket.api.TicketDtos.UserSummary;
import com.portfolio.helpdesk.ticket.domain.Ticket;
import com.portfolio.helpdesk.ticket.domain.TicketPriority;
import com.portfolio.helpdesk.ticket.infrastructure.TicketRepository;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserAccountRepository userRepository;

    public TicketService(TicketRepository ticketRepository, CategoryRepository categoryRepository, UserAccountRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, Authentication authentication) {
        UserAccount reporter = currentUser(authentication);
        Category category = categoryRepository.findById(request.categoryId())
            .filter(Category::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        Ticket ticket = new Ticket(request.title().trim(), request.description().trim(), category, reporter);
        if (request.priority() != null) ticket.changePriority(request.priority());
        return toResponse(ticketRepository.save(ticket));
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
    public TicketResponse get(Long id, Authentication authentication) {
        Ticket ticket = find(id);
        assertCanRead(ticket, currentUser(authentication));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse changeStatus(Long id, ChangeStatusRequest request, Authentication authentication) {
        Ticket ticket = find(id);
        assertCanManage(ticket, currentUser(authentication));
        ticket.changeStatus(request.status());
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse changePriority(Long id, ChangePriorityRequest request, Authentication authentication) {
        Ticket ticket = find(id);
        assertCanManage(ticket, currentUser(authentication));
        ticket.changePriority(request.priority());
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse assign(Long id, AssignTicketRequest request, Authentication authentication) {
        Ticket ticket = find(id);
        UserAccount actor = currentUser(authentication);
        if (!hasRole(actor, RoleName.ADMIN) && !hasRole(actor, RoleName.TECHNICIAN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes asignar incidencias");
        }
        UserAccount technician = userRepository.findById(request.technicianId())
            .filter(user -> hasRole(user, RoleName.TECHNICIAN))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado"));
        ticket.assignTo(technician);
        return toResponse(ticket);
    }

    private Ticket find(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada"));
    }

    private void assertCanRead(Ticket ticket, UserAccount user) {
        if (hasRole(user, RoleName.ADMIN) || ticket.getReporter().getId().equals(user.getId())
            || ticket.getTechnician() != null && ticket.getTechnician().getId().equals(user.getId())) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar esta incidencia");
    }

    private void assertCanManage(Ticket ticket, UserAccount user) {
        if (hasRole(user, RoleName.ADMIN) || ticket.getTechnician() != null && ticket.getTechnician().getId().equals(user.getId())) return;
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
        return new TicketResponse(
            ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getStatus(), ticket.getPriority(),
            new CategorySummary(ticket.getCategory().getId(), ticket.getCategory().getName()),
            toUserSummary(ticket.getReporter()),
            ticket.getTechnician() == null ? null : toUserSummary(ticket.getTechnician()),
            ticket.getCreatedAt(), ticket.getUpdatedAt()
        );
    }

    private UserSummary toUserSummary(UserAccount user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
