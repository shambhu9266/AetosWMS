import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface DialogConfig {
  title: string;
  message: string;
  type?: 'info' | 'success' | 'warning' | 'error' | 'confirm' | 'prompt';
  confirmText?: string;
  cancelText?: string;
  inputLabel?: string;
  inputPlaceholder?: string;
  inputValue?: string;
  showCancel?: boolean;
}

export interface DialogResult {
  confirmed: boolean;
  value?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DialogService {
  private dialogSubject = new BehaviorSubject<DialogConfig | null>(null);
  private resultSubject = new BehaviorSubject<DialogResult | null>(null);

  public dialog$ = this.dialogSubject.asObservable();
  public result$ = this.resultSubject.asObservable();

  constructor() {}

  // Simple alert dialog
  alert(title: string, message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info'): Promise<void> {
    return new Promise((resolve) => {
      this.dialogSubject.next({
        title,
        message,
        type,
        confirmText: 'OK',
        showCancel: false
      });

      const subscription = this.result$.subscribe(result => {
        if (result) {
          subscription.unsubscribe();
          this.dialogSubject.next(null);
          this.resultSubject.next(null);
          resolve();
        }
      });
    });
  }

  // Confirmation dialog
  confirm(title: string, message: string, confirmText: string = 'Confirm', cancelText: string = 'Cancel'): Promise<boolean> {
    return new Promise((resolve) => {
      this.dialogSubject.next({
        title,
        message,
        type: 'confirm',
        confirmText,
        cancelText,
        showCancel: true
      });

      const subscription = this.result$.subscribe(result => {
        if (result !== null) {
          subscription.unsubscribe();
          this.dialogSubject.next(null);
          this.resultSubject.next(null);
          resolve(result.confirmed);
        }
      });
    });
  }

  // Prompt dialog (for input)
  prompt(title: string, message: string, inputLabel: string = 'Enter value', inputPlaceholder: string = '', inputValue: string = ''): Promise<string | null> {
    return new Promise((resolve) => {
      this.dialogSubject.next({
        title,
        message,
        type: 'prompt',
        inputLabel,
        inputPlaceholder,
        inputValue,
        confirmText: 'OK',
        cancelText: 'Cancel',
        showCancel: true
      });

      const subscription = this.result$.subscribe(result => {
        if (result !== null) {
          subscription.unsubscribe();
          this.dialogSubject.next(null);
          this.resultSubject.next(null);
          resolve(result.confirmed ? result.value || null : null);
        }
      });
    });
  }

  // Close dialog
  close(result?: DialogResult): void {
    this.resultSubject.next(result || { confirmed: false });
  }

  // Get current dialog
  getCurrentDialog(): DialogConfig | null {
    return this.dialogSubject.value;
  }
}
