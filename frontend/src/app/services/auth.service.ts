import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

export interface User {
  id: number;
  username: string;
  fullName: string;
  role: string;
  department: string;
}

export interface LoginResponse {
  success: boolean;
  token?: string;
  user?: User;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = '/api/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  private tokenSubject = new BehaviorSubject<string | null>(null);

  public currentUser$ = this.currentUserSubject.asObservable();
  public token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) {
    // Check if user is already logged in (from localStorage)
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('currentUser');
    
    if (savedToken && savedUser) {
      this.tokenSubject.next(savedToken);
      this.currentUserSubject.next(JSON.parse(savedUser));
    }
  }

  login(username: string, password: string): Observable<LoginResponse> 
  {
    const params = new HttpParams()
      .set('username', username)
      .set('password', password);

    console.log('DEBUG: Attempting login for user:', username);
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, null, { params })
      .pipe(
        tap(response => {
          console.log('DEBUG: Login response:', response);
          if (response.success && response.token && response.user) {
            this.tokenSubject.next(response.token);
            this.currentUserSubject.next(response.user);
            
            // Save to localStorage
            localStorage.setItem('token', response.token);
            localStorage.setItem('currentUser', JSON.stringify(response.user));
            console.log('DEBUG: Token saved:', response.token);
          } else {
            console.log('DEBUG: Login failed:', response.message);
          }
        })
      );
  }

  logout(): Observable<any> {
    const token = this.tokenSubject.value;
    
    // Always clear the token first
    this.clearSession();
    
    if (token) {
      const params = new HttpParams().set('token', token);
      return this.http.post(`${this.baseUrl}/logout`, null, { params })
        .pipe(
          tap(() => {
            console.log('Logout successful');
          }),
          catchError((error) => {
            console.error('Logout error:', error);
            // Even if backend logout fails, we've already cleared the token
            return of({ success: true });
          })
        );
    }
    return new Observable(observer => {
      observer.next({ success: true });
      observer.complete();
    });
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  getToken(): string | null {
    const token = this.tokenSubject.value;
    console.log('DEBUG: getToken called, returning:', token);
    return token;
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    console.log('DEBUG hasRole: Checking role:', role);
    console.log('DEBUG hasRole: User role:', user?.role);
    console.log('DEBUG hasRole: Direct comparison:', user?.role === role);
    console.log('DEBUG hasRole: Is SUPERADMIN:', user?.role === 'SUPERADMIN');
    
    const result = user?.role === role || user?.role === 'SUPERADMIN';
    console.log('DEBUG hasRole: Final result:', result);
    return result;
  }

  canAccessITApprovals(): boolean {
    const result = this.hasRole('IT_MANAGER') || this.hasRole('SUPERADMIN');
    console.log('DEBUG canAccessITApprovals: hasRole(IT_MANAGER):', this.hasRole('IT_MANAGER'));
    console.log('DEBUG canAccessITApprovals: hasRole(SUPERADMIN):', this.hasRole('SUPERADMIN'));
    console.log('DEBUG canAccessITApprovals: result:', result);
    return result;
  }

  canAccessFinanceApprovals(): boolean {
    return this.hasRole('FINANCE_MANAGER') || this.hasRole('SUPERADMIN');
  }

  isDepartmentManager(): boolean {
    return this.hasRole('DEPARTMENT_MANAGER') || this.hasRole('SUPERADMIN');
  }

  canAccessBudget(): boolean {
    const user = this.getCurrentUser();
    console.log('DEBUG AuthService: Current user for budget check:', user);
    console.log('DEBUG AuthService: User role:', user?.role);
    console.log('DEBUG AuthService: Role type:', typeof user?.role);
    console.log('DEBUG AuthService: Is FINANCE_MANAGER?', user?.role === 'FINANCE_MANAGER');
    
    // Only FINANCE_MANAGER can access budget (SUPERADMIN excluded)
    const result = user?.role === 'FINANCE_MANAGER';
    console.log('DEBUG AuthService: Budget access result:', result);
    return result;
  }

  isEmployee(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'EMPLOYEE';
  }


  canCreateRequisitions(): boolean {
    // All logged-in users can create requisitions
    return this.isLoggedIn();
  }

  clearSession(): void {
    this.currentUserSubject.next(null);
    this.tokenSubject.next(null);
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
  }
}
