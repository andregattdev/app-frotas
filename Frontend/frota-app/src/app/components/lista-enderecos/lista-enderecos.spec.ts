import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListaEnderecos } from './lista-enderecos';

describe('ListaEnderecos', () => {
  let component: ListaEnderecos;
  let fixture: ComponentFixture<ListaEnderecos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaEnderecos],
    }).compileComponents();

    fixture = TestBed.createComponent(ListaEnderecos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
