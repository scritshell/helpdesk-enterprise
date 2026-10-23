export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_USER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Category { id: number; name: string; description?: string; active?: boolean; }
export interface UserSummary { id: number; email: string; firstName: string; lastName: string; }
export interface Ticket {
  id: number; title: string; description: string; status: TicketStatus; priority: TicketPriority;
  category: { id: number; name: string }; reporter: UserSummary; technician: UserSummary | null;
  createdAt: string; updatedAt: string;
}
export interface TicketComment { id: number; body: string; author: UserSummary; createdAt: string; }
export interface TicketHistory { id: number; action: string; oldValue: string | null; newValue: string | null; actor: UserSummary; createdAt: string; }
export interface CreateTicketRequest { title: string; description: string; categoryId: number; priority?: TicketPriority; }
export interface Technician extends UserSummary {}

export const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Abierta', IN_PROGRESS: 'En progreso', WAITING_USER: 'Pendiente del usuario', RESOLVED: 'Resuelta', CLOSED: 'Cerrada'
};
export const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Baja', MEDIUM: 'Media', HIGH: 'Alta', CRITICAL: 'Crítica'
};
export const PRIORITY_CODES: Record<TicketPriority, string> = { LOW: 'P4', MEDIUM: 'P3', HIGH: 'P2', CRITICAL: 'P1' };
export const STATUS_VALUES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'WAITING_USER', 'RESOLVED', 'CLOSED'];
export const PRIORITY_VALUES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
