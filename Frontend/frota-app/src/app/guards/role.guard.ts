import { isPlatformServer } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  if (isPlatformServer(inject(PLATFORM_ID))) return true;

  const auth = inject(AuthService);
  const router = inject(Router);
  const allowedRoles = route.data['roles'] as string[];

  if (!auth.isAuthenticated) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  if (auth.hasAnyRole(...allowedRoles)) return true;
  if (auth.hasAnyRole('MOTORISTA')) return router.createUrlTree(['/minhas-rotas']);
  return router.createUrlTree(['/login'], { queryParams: { acesso: 'negado' } });
};
