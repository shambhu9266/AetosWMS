import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService, Notification } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css']
})
export class NotificationsComponent implements OnInit {
  protected readonly notifications = signal<Notification[]>([]);
  protected readonly loading = signal<boolean>(true);

  constructor(
    private apiService: ApiService,
    public authService: AuthService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadNotifications();
  }

  private loadNotifications() {
    this.loading.set(true);
    this.apiService.getNotifications().subscribe({
      next: (response) => {
        if (response.success) {
          let allNotifications = response.notifications || [];
          
          // Filter notifications based on user role
          if (this.authService.isEmployee()) {
            // For employees, only show notifications related to their requisitions and PDFs
            const currentUser = this.authService.getCurrentUser();
            allNotifications = allNotifications.filter((notification: Notification) => {
              return notification.message.includes(currentUser?.username || '') ||
                notification.message.includes('requisition') ||
                notification.message.includes('approval') ||
                notification.message.includes('PDF') ||
                notification.message.includes('pdf') ||
                notification.message.includes('rejected') ||
                notification.message.includes('approved');
            });
          }
          // For managers/admins, show all notifications (no filtering)
          
          this.notifications.set(allNotifications);
        }
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading notifications:', error);
        this.loading.set(false);
      }
    });
  }

  refreshNotifications() {
    this.loadNotifications();
  }

  markAsRead(notification: Notification) {
    // TODO: Implement mark as read functionality if needed
    console.log('Mark as read:', notification);
  }

  deleteNotification(notification: Notification) {
    if (confirm('Are you sure you want to delete this notification?')) {
      this.apiService.deleteNotification(notification.notificationId).subscribe({
        next: (response) => {
          if (response.success) {
            this.alertService.showError('Notification deleted successfully!');
            this.loadNotifications(); // Reload notifications list
          } else {
            this.alertService.showError('Failed to delete notification: ' + response.message);
          }
        },
        error: (error) => {
          console.error('Error deleting notification:', error);
          this.alertService.showError('Failed to delete notification. Please try again.');
        }
      });
    }
  }

  getNotificationIcon(message: string): string {
    const msg = message.toLowerCase();
    if (msg.includes('approve') || msg.includes('approved')) {
      return 'M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z';
    } else if (msg.includes('reject') || msg.includes('rejected')) {
      return 'M6 18L18 6M6 6L18 18';
    } else if (msg.includes('requisition') || msg.includes('pr')) {
      return 'M14 2H6C4.89543 2 4 2.89543 4 4V20C4 21.1046 4.89543 22 6 22H18C19.1046 22 20 21.1046 20 20V8L14 2Z M14 2V8H20';
    } else {
      return 'M18 8C18 6.4087 17.3679 4.88258 16.2426 3.75736C15.1174 2.63214 13.5913 2 12 2C10.4087 2 8.88258 2.63214 7.75736 3.75736C6.63214 4.88258 6 6.4087 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8Z M13.73 21C13.5542 21.3031 13.3019 21.5547 12.9982 21.7295C12.6946 21.9044 12.3504 21.9965 12 21.9965C11.6496 21.9965 11.3054 21.9044 11.0018 21.7295C10.6982 21.5547 10.4458 21.3031 10.27 21';
    }
  }

  getNotificationClass(message: string): string {
    const msg = message.toLowerCase();
    if (msg.includes('approve') || msg.includes('approved')) {
      return 'notification-approval';
    } else if (msg.includes('reject') || msg.includes('rejected')) {
      return 'notification-rejection';
    } else if (msg.includes('requisition') || msg.includes('pr')) {
      return 'notification-requisition';
    } else {
      return 'notification-default';
    }
  }

  getNotificationType(message: string): string {
    const msg = message.toLowerCase();
    if (msg.includes('approve') || msg.includes('approved')) {
      return 'Approval';
    } else if (msg.includes('reject') || msg.includes('rejected')) {
      return 'Rejection';
    } else if (msg.includes('requisition') || msg.includes('pr')) {
      return 'Requisition';
    } else {
      return 'General';
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60));
    
    if (diffInHours < 1) {
      return 'Just now';
    } else if (diffInHours < 24) {
      return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
    } else {
      const diffInDays = Math.floor(diffInHours / 24);
      if (diffInDays < 7) {
        return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
      } else {
        return date.toLocaleDateString('en-US', {
          month: 'short',
          day: 'numeric',
          year: 'numeric'
        });
      }
    }
  }
}
