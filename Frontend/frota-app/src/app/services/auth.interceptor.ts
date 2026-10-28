import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const apiPattern = /^http:\/\/localhost:8080\/api\//;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  const isApiRequest = apiPattern.test(request.url);
  const apiRequest = isApiRequest && isBrowser
    ? request.clone({
      url: request.url.replace(
        'http://localhost:8080',
        `${window.location.protocol}//${window.location.hostname}:8080`
      )
    })
    : request;
  const isLoginRequest = apiRequest.url.endsWith('/api/auth/login');
  const token = auth.token;

  const authenticatedRequest = isApiRequest && !isLoginRequest && token
    ? apiRequest.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : apiRequest;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (isBrowser && isApiRequest && !isLoginRequest && error instanceof HttpErrorResponse && error.status === 401) {
        auth.logout();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
