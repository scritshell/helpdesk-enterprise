import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { AdminService } from '../../core/admin/admin.service';
import { AdminStatisticsComponent } from './admin-statistics.component';

describe('AdminStatisticsComponent', () => {
  it('carga estadísticas reales y calcula el ancho de las barras', async () => {
    const statistics = {
      total: 4, open: 1, pending: 1, resolved: 1, closed: 1,
      priorities: [{ label: 'HIGH', value: 4 }], categories: [], evolution: [], days: 30
    };
    const service = { statistics: vi.fn().mockReturnValue(of(statistics)) };
    TestBed.configureTestingModule({
      imports: [AdminStatisticsComponent],
      providers: [provideRouter([]), { provide: AdminService, useValue: service }]
    });
    const fixture = TestBed.createComponent(AdminStatisticsComponent);
    await fixture.whenStable();
    expect(fixture.componentInstance.stats?.total).toBe(4);
    expect(fixture.componentInstance.barWidth(statistics.priorities[0], statistics.priorities)).toBe(100);
  });
});
