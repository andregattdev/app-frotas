import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { EMPTY, of } from 'rxjs';
import { AppDataRefreshService } from '../../services/app-data-refresh.service';
import { AuthService } from '../../services/auth.service';
import { ListaChecklist } from './lista-checklist';

describe('ListaChecklist', () => {
  let component: ListaChecklist;
  let fixture: ComponentFixture<ListaChecklist>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaChecklist],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: of({ get: (key: string) => key === 'veiculo' ? '12' : null }) } },
        { provide: AppDataRefreshService, useValue: { refreshRequested$: EMPTY } },
        { provide: AuthService, useValue: { hasAnyRole: () => false, isAdmin: true } },
        { provide: HttpClient, useValue: { get: () => of([]), post: () => EMPTY } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ListaChecklist);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('starts every inspection item unchecked', () => {
    expect(component.itens.every(([campo]) => component.checklistForm.controls[campo].value === false)).toBe(true);
  });

});
