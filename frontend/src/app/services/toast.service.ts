import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ToastConfig {
  id: string;
  title: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
  position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' | 'top-center' | 'bottom-center';
  showCloseButton?: boolean;
  showProgress?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastConfig[]>([]);
  public toasts$ = this.toastsSubject.asObservable();

  constructor() {}

  // Show success toast
  success(message: string, title: string = 'Success', duration: number = 5000): string {
    return this.show({
      id: this.generateId(),
      title,
      message,
      type: 'success',
      duration,
      position: 'top-right',
      showCloseButton: true,
      showProgress: true
    });
  }

  // Show error toast
  error(message: string, title: string = 'Error', duration: number = 7000): string {
    return this.show({
      id: this.generateId(),
      title,
      message,
      type: 'error',
      duration,
      position: 'top-right',
      showCloseButton: true,
      showProgress: true
    });
  }

  // Show warning toast
  warning(message: string, title: string = 'Warning', duration: number = 6000): string {
    return this.show({
      id: this.generateId(),
      title,
      message,
      type: 'warning',
      duration,
      position: 'top-right',
      showCloseButton: true,
      showProgress: true
    });
  }

  // Show info toast
  info(message: string, title: string = 'Information', duration: number = 5000): string {
    return this.show({
      id: this.generateId(),
      title,
      message,
      type: 'info',
      duration,
      position: 'top-right',
      showCloseButton: true,
      showProgress: true
    });
  }

  // Show custom toast
  show(config: ToastConfig): string {
    const toasts = this.toastsSubject.value;
    toasts.push(config);
    this.toastsSubject.next([...toasts]);

    // Auto remove after duration
    if (config.duration && config.duration > 0) {
      setTimeout(() => {
        this.remove(config.id);
      }, config.duration);
    }

    return config.id;
  }

  // Remove specific toast
  remove(id: string): void {
    const toasts = this.toastsSubject.value.filter(toast => toast.id !== id);
    this.toastsSubject.next(toasts);
  }

  // Clear all toasts
  clear(): void {
    this.toastsSubject.next([]);
  }

  // Get current toasts
  getToasts(): ToastConfig[] {
    return this.toastsSubject.value;
  }

  private generateId(): string {
    return 'toast_' + Math.random().toString(36).substr(2, 9);
  }
}
