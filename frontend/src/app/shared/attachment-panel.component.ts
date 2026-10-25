import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject, input, OnInit } from '@angular/core';
import { finalize } from 'rxjs';
import { AdminService } from '../core/admin/admin.service';
import { Attachment } from '../core/admin/admin.models';
import { apiErrorMessage } from '../core/http/http-error';

@Component({ selector: 'app-attachment-panel', standalone: true, imports: [CommonModule], template: `
  <article class="panel"><div class="panel-header"><div><h2>Adjuntos</h2><p class="text-muted mb-0">PDF, PNG, JPEG, TXT o CSV · máximo 10 MB</p></div><label class="btn btn-outline-primary btn-sm" [class.disabled]="uploading">＋ Subir archivo<input class="visually-hidden" type="file" accept=".pdf,.png,.jpg,.jpeg,.txt,.csv" (change)="upload($event)" [disabled]="uploading" /></label></div><div *ngIf="error" class="alert alert-danger" role="alert">{{ error }}</div><div *ngIf="attachments.length === 0 && !loading" class="empty-inline">No hay archivos adjuntos.</div><div *ngIf="loading" class="skeleton skeleton-line" aria-busy="true"></div><ul *ngIf="attachments.length > 0" class="attachment-list"><li *ngFor="let attachment of attachments"><div><strong>{{ attachment.originalName }}</strong><small>{{ formatSize(attachment.sizeBytes) }} · {{ attachment.uploadedBy }} · {{ attachment.createdAt | date:'dd/MM/yyyy HH:mm' }}</small></div><button class="btn btn-sm btn-link" (click)="download(attachment)">Descargar</button></li></ul>
  </article>
` })
export class AttachmentPanelComponent implements OnInit {
  readonly ticketId = input.required<number>(); private readonly service = inject(AdminService); private readonly cdr = inject(ChangeDetectorRef);
  attachments: Attachment[] = []; loading = true; uploading = false; error = '';
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.service.attachments(this.ticketId()).pipe(finalize(() => { this.loading = false; this.cdr.markForCheck(); })).subscribe({ next: data => this.attachments = data, error: error => this.error = apiErrorMessage(error, 'No se han podido cargar los adjuntos.') }); }
  upload(event: Event): void { const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return; this.uploading = true; this.error = ''; this.service.uploadAttachment(this.ticketId(), file).pipe(finalize(() => { this.uploading = false; input.value = ''; this.cdr.markForCheck(); })).subscribe({ next: attachment => this.attachments = [...this.attachments, attachment], error: error => this.error = apiErrorMessage(error, 'No se ha podido subir el archivo.') }); }
  download(attachment: Attachment): void { this.service.downloadAttachment(this.ticketId(), attachment.id).subscribe({ next: blob => { const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = attachment.originalName; anchor.click(); URL.revokeObjectURL(url); }, error: error => { this.error = apiErrorMessage(error, 'No se ha podido descargar el archivo.'); this.cdr.markForCheck(); } }); }
  formatSize(size: number): string { return size < 1024 * 1024 ? `${Math.ceil(size / 1024)} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`; }
}
