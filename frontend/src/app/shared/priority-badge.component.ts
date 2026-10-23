import { Component, input } from '@angular/core';
import { PRIORITY_CODES, PRIORITY_LABELS, TicketPriority } from '../core/tickets/ticket.models';

@Component({ selector: 'app-priority-badge', standalone: true, template: `<span class="priority-badge priority-{{ priority() }}"><strong>{{ code() }}</strong>{{ label() }}</span>` })
export class PriorityBadgeComponent {
  readonly priority = input.required<TicketPriority>();
  code(): string { return PRIORITY_CODES[this.priority()]; }
  label(): string { return PRIORITY_LABELS[this.priority()]; }
}
