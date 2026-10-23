import { Component, input } from '@angular/core';
import { STATUS_LABELS, TicketStatus } from '../core/tickets/ticket.models';

@Component({ selector: 'app-status-badge', standalone: true, template: `<span class="status-badge status-{{ status() }}"><span aria-hidden="true">●</span>{{ label() }}</span>` })
export class StatusBadgeComponent {
  readonly status = input.required<TicketStatus>();
  label(): string { return STATUS_LABELS[this.status()]; }
}
