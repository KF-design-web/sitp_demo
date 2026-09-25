import { inject } from '@angular/core';
import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { API_URL } from './api-url';
import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);

  const withCreds = req.url.startsWith(API_URL)
    ? req.clone({ withCredentials: true })
    : req;

  return next(withCreds).pipe(
    catchError((err: HttpErrorResponse) => {
      if (
        req.url.startsWith(API_URL) &&
        err.status === 401 &&
        !req.url.endsWith('/auth/login') &&
        !req.url.endsWith('/auth/me')
      ) {
        session.clear();
      }
      return throwError(() => err);
    })
  );
};
