import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('keeps successful indicators when one endpoint fails', () => {
    component.carregarResumo();
    const requests = httpMock.match(request => request.url.startsWith('http://localhost:8080/api/'));

    expect(requests).toHaveLength(4);
    requests.find(request => request.request.url.endsWith('/api/veiculos'))!.flush([{ id: 1, placa: 'ABC1234', modelo: 'Van', marca: 'Marca' }]);
    requests.find(request => request.request.url.endsWith('/api/motoristas'))!.flush([{ id: 1, nome: 'Ana' }]);
    requests.find(request => request.request.url.endsWith('/api/enderecos'))!.flush([{ id: 1 }]);
    requests.find(request => request.request.url.endsWith('/api/checklists'))!.flush('falha', { status: 500, statusText: 'Erro' });

    expect(component.veiculos).toHaveLength(1);
    expect(component.motoristas).toHaveLength(1);
    expect(component.enderecos).toHaveLength(1);
    expect(component.checklists).toHaveLength(0);
    expect(component.erroCarregamento).toBe(true);
    expect(component.carregando).toBe(false);
  });

  it('shows vehicles without a checklist and vehicles with issues in their latest checklist', () => {
    component.veiculos = [
      { id: 1, placa: 'ABC1234', modelo: 'Van', marca: 'Marca', frota: '10' },
      { id: 2, placa: 'DEF5678', modelo: 'Caminhão', marca: 'Marca', frota: '20' },
      { id: 3, placa: 'GHI9012', modelo: 'Utilitário', marca: 'Marca', frota: '30' },
    ];
    component.checklists = [
      { veiculo: { id: 1 }, dataHora: '2026-10-03T10:00:00', pneus: false },
      { veiculo: { id: 1 }, dataHora: '2026-10-04T10:00:00', pneus: true },
      { veiculo: { id: 3 }, dataHora: '2026-10-04T11:00:00', freios: false, observacoes: 'Ruído ao frear' },
    ];

    expect(component.veiculosAtencaoChecklist).toEqual([
      { veiculo: component.veiculos[1], motivo: 'Checklist ainda não realizado' },
      { veiculo: component.veiculos[2], motivo: 'Freios, Há observações registradas' },
    ]);
  });
});
