import { Category } from '../tickets/ticket.models';

export type AdminRole = 'USER' | 'TECHNICIAN' | 'ADMIN';
export interface AdminUser { id: number; email: string; firstName: string; lastName: string; roles: AdminRole[]; enabled: boolean; createdAt: string; }
export interface UpdateAdminUser { roles: AdminRole[]; enabled: boolean; }
export interface AdminCategory extends Category { active: boolean; }
export interface CountItem { label: string; value: number; }
export interface DailyCount { date: string; created: number; resolved: number; closed: number; }
export interface Statistics { total: number; open: number; pending: number; resolved: number; closed: number; priorities: CountItem[]; categories: CountItem[]; evolution: DailyCount[]; days: number; }
export interface Attachment { id: number; originalName: string; sizeBytes: number; contentType: string; uploadedBy: string; createdAt: string; }
