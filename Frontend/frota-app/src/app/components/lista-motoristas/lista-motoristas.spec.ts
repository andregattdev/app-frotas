import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListaMotoristas } from './lista-motoristas';

describe('ListaMotoristas', () => {
  let component: ListaMotoristas;
  let fixture: ComponentFixture<ListaMotoristas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaMotoristas],
    }).compileComponents();

    fixture = TestBed.createComponent(ListaMotoristas);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
