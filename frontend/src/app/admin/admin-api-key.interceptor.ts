import { HttpInterceptorFn } from '@angular/common/http';

export const adminApiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  // Only attach the key to admin endpoints so it doesn't leak to public routes
  if (req.url.includes('/api/v1/admin')) {
    const clonedReq = req.clone({
      setHeaders: {
        'X-Admin-API-Key': 'X-Admin-API-Key'
      }
    });
    return next(clonedReq);
  }

  // Pass through for non-admin requests
  return next(req);
};
