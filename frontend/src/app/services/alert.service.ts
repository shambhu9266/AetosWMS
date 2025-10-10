import { Injectable } from '@angular/core';
import { ToastService } from './toast.service';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  constructor(private toastService: ToastService) {}

  // Show toast notifications instead of alerts
  showInfo(message: string, title: string = 'Information'): void {
    this.toastService.info(message, title);
  }

  showSuccess(message: string, title: string = 'Success'): void {
    this.toastService.success(message, title);
  }

  showError(message: string, title: string = 'Error'): void {
    this.toastService.error(message, title);
  }

  showWarning(message: string, title: string = 'Warning'): void {
    this.toastService.warning(message, title);
  }

  // Legacy methods for backward compatibility
  alert(message: string, title: string = 'Information'): void {
    this.showInfo(message, title);
  }

  success(message: string, title: string = 'Success'): void {
    this.showSuccess(message, title);
  }

  error(message: string, title: string = 'Error'): void {
    this.showError(message, title);
  }

  warning(message: string, title: string = 'Warning'): void {
    this.showWarning(message, title);
  }

  // For confirmations, we'll use browser confirm for now
  // In a full implementation, you'd want a custom modal
  confirm(message: string, title: string = 'Confirm'): Promise<boolean> {
    return Promise.resolve(confirm(`${title}\n\n${message}`));
  }

  // For prompts, we'll use browser prompt for now
  // In a full implementation, you'd want a custom modal
  prompt(message: string, title: string = 'Input Required', placeholder: string = ''): Promise<string | null> {
    const result = prompt(`${title}\n\n${message}`, placeholder);
    return Promise.resolve(result);
  }

  // Specialized methods for common use cases
  confirmDelete(itemName: string = 'item'): Promise<boolean> {
    return this.confirm(
      `Are you sure you want to delete this ${itemName}? This action cannot be undone.`,
      'Delete Confirmation'
    );
  }

  confirmReject(): Promise<boolean> {
    return this.confirm(
      'Are you sure you want to reject this item? This action cannot be undone.',
      'Reject Confirmation'
    );
  }

  promptRejectionReason(): Promise<string | null> {
    return this.prompt(
      'Please provide a reason for rejection:',
      'Rejection Reason',
      'Enter rejection reason...'
    );
  }

  promptComment(): Promise<string | null> {
    return this.prompt(
      'Please enter your comment:',
      'Add Comment',
      'Enter your comment...'
    );
  }
}
