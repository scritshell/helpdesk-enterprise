import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AdminService } from '../core/admin/admin.service';
import { AttachmentPanelComponent } from './attachment-panel.component';

describe('AttachmentPanelComponent', () => {
  it('termina la carga y muestra el resultado del backend', async () => {
    const service = { attachments: vi.fn().mockReturnValue(of([])) };
    TestBed.configureTestingModule({ imports: [AttachmentPanelComponent], providers: [{ provide: AdminService, useValue: service }] });
    const fixture = TestBed.createComponent(AttachmentPanelComponent);
    fixture.componentRef.setInput('ticketId', 1);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.loading).toBe(false);
    expect(service.attachments).toHaveBeenCalledWith(1);
  });

  it('termina la carga y conserva el mensaje cuando falla', async () => {
    const service = { attachments: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 }))) };
    TestBed.configureTestingModule({ imports: [AttachmentPanelComponent], providers: [{ provide: AdminService, useValue: service }] });
    const fixture = TestBed.createComponent(AttachmentPanelComponent);
    fixture.componentRef.setInput('ticketId', 1);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.componentInstance.error).toContain('permisos');
  });
});
