import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Login } from './login';

describe('Login', () => {
  it('allows fleet assistants and redirects them to the assistant panel', async () => {
    const auth = {
      login: vi.fn().mockReturnValue(of({
        token: 'token',
        username: 'assistente',
        roles: ['ASSISTENTE_FROTA']
      })),
      logout: vi.fn()
    };
    const router = { navigateByUrl: vi.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: auth },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { has: () => false, get: () => null } } }
        },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(Login);
    const component = fixture.componentInstance;
    component.form.setValue({ username: 'assistente', password: 'senha-segura' });

    component.entrar();

    expect(auth.logout).not.toHaveBeenCalled();
    expect(component.erro).toBe('');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/assistente-frota');
  });
});
