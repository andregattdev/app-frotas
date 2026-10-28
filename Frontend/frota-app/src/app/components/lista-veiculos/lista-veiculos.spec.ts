import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ListaVeiculos } from './lista-veiculos';

describe('ListaVeiculos', () => {
  let component: ListaVeiculos;
  let fixture: ComponentFixture<ListaVeiculos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaVeiculos],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ListaVeiculos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
