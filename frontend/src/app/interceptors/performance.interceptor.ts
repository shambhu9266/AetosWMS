import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { PerformanceService } from '../services/performance.service';

@Injectable()
export class PerformanceInterceptor implements HttpInterceptor {
  
  constructor(private performanceService: PerformanceService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const startTime = Date.now();
    
    // Record API request
    this.performanceService.recordApiRequest();

    return next.handle(req).pipe(
      tap(
        (event: HttpEvent<any>) => {
          if (event instanceof HttpResponse) {
            const endTime = Date.now();
            const responseTime = endTime - startTime;
            
            // Record response time
            this.performanceService.recordResponseTime(responseTime);
            
            // Log slow requests (> 2 seconds)
            if (responseTime > 2000) {
              console.warn(`Slow API request: ${req.url} took ${responseTime}ms`);
            }
          }
        },
        (error) => {
          // Record API error
          this.performanceService.recordApiError();
          
          const endTime = Date.now();
          const responseTime = endTime - startTime;
          
          console.error(`API Error: ${req.url} failed after ${responseTime}ms`, error);
        }
      )
    );
  }
}
