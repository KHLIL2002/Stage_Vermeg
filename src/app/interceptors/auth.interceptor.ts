import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Only attach token for calls to our backend API
  if (!req.url.includes('/api/')) {
    return next(req);
  }

  // Obtain a valid token first (refreshes transparently if expired)
  return authService.getValidToken().pipe(
    switchMap(token => {
      const authReq = token
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;

      return next(authReq).pipe(
        catchError((err: HttpErrorResponse) => {
          // Token refusé par le serveur → un refresh puis on rejoue la requête une fois
          if (err.status === 401 && token) {
            return authService.forceRefresh().pipe(
              switchMap(newToken => {
                if (!newToken) return throwError(() => err);
                const retryReq = req.clone({
                  setHeaders: { Authorization: `Bearer ${newToken}` },
                });
                return next(retryReq);
              })
            );
          }
          return throwError(() => err);
        })
      );
    })
  );
};
