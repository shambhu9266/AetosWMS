import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ApiService, Notification } from '../../services/api.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  protected readonly notifications = signal<Notification[]>([]);

              menuItems = [
                {
                  path: '/dashboard',
                  icon: 'M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z M9 22V12H15V22',
                  label: 'Dashboard',
                  description: 'Overview and analytics'
                },
                {
                  path: '/budget',
                  icon: 'M12 2V22M17 5H9.5C8.11929 5 7 6.11929 7 7.5S8.11929 10 9.5 10H14.5C15.8807 10 17 11.1193 17 12.5S15.8807 15 14.5 15H7',
                  label: 'Budget',
                  description: 'Financial overview'
                },
                {
                  path: '/requisitions',
                  icon: 'M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z M14 2V8H20 M16 13H8 M16 17H8 M10 9H8',
                  label: 'Requisitions',
                  description: 'Purchase requests'
                },
                {
                  path: '/approvals',
                  icon: 'M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z',
                  label: 'Approvals',
                  description: 'Review and approve'
                },
                {
      path: '/pdf-upload',
      icon: 'M14 2H6C4.89543 2 4 2.89543 4 4V20C4 21.1046 4.89543 22 6 22H18C19.1046 22 20 21.1046 20 20V8L14 2Z M14 2V8H20 M16 13H8 M16 17H8 M10 9H8',
      label: 'PDF Upload',
      description: 'Upload vendor documents'
    },
    {
      path: '/purchase-orders',
      icon: 'M9 5H7C5.89543 5 5 5.89543 5 7V19C5 20.1046 5.89543 21 7 21H17C18.1046 21 19 20.1046 19 19V7C19 5.89543 18.1046 5 17 5H15M9 5C9 6.10457 9.89543 7 11 7H13C14.1046 7 15 6.10457 15 5M9 5C9 3.89543 9.89543 3 11 3H13C14.1046 3 15 3.89543 15 5M12 12H15M12 16H15M9 12H9.01M9 16H9.01',
      label: 'Purchase Orders',
      description: 'Create and manage POs'
    },
    {
      path: '/grn',
      icon: 'M19 13V19C19 20.1046 18.1046 21 17 21H7C5.89543 21 5 20.1046 5 19V13M7 10L12 15L17 10M12 15V3',
      label: 'Receiver',
      description: 'Goods Receipt/Receiving'
    },
    {
      path: '/master',
      icon: 'M10.325 4.317C10.751 4.105 11.245 4 11.75 4H12.25C12.755 4 13.249 4.105 13.675 4.317L15.5 5.25C15.5 5.25 16.5 5.75 16.5 6.75V7.25C16.5 8.25 15.5 8.75 15.5 8.75L13.675 9.683C13.249 9.895 12.755 10 12.25 10H11.75C11.245 10 10.751 9.895 10.325 9.683L8.5 8.75C8.5 8.75 7.5 8.25 7.5 7.25V6.75C7.5 5.75 8.5 5.25 8.5 5.25L10.325 4.317ZM12 13.5C12.8284 13.5 13.5 12.8284 13.5 12C13.5 11.1716 12.8284 10.5 12 10.5C11.1716 10.5 10.5 11.1716 10.5 12C10.5 12.8284 11.1716 13.5 12 13.5Z',
      label: 'Master',
      description: 'Master data management'
    }
  ];

  constructor(
    private authService: AuthService,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit() {
    this.loadNotifications();
  }

  private loadNotifications() {
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
      },
      error: (error) => {
        console.error('Error loading notifications:', error);
      }
    });
  }


  getCurrentUser() {
    return this.authService.getCurrentUser();
  }

  getFilteredMenuItems() {
    const currentUser = this.getCurrentUser();
    console.log('DEBUG: Current user:', currentUser);
    console.log('DEBUG: Can access budget:', this.authService.canAccessBudget());
    
    // For employees, only show dashboard and requisitions (PR creation)
    if (this.authService.isEmployee()) {
      return this.menuItems.filter(item => 
        item.path === '/dashboard' || item.path === '/requisitions'
      );
    }
    
    return this.menuItems.filter(item => {
      if (item.path === '/budget') {
        const canAccess = this.authService.canAccessBudget();
        console.log('DEBUG: Budget access check:', canAccess);
        return canAccess;
      }
      if (item.path === '/pdf-upload') {
        // PDF Upload removed from IT Manager sidebar - no access for any role
        const canAccess = false;
        console.log('DEBUG: PDF Upload access check:', canAccess, 'User role:', currentUser?.role);
        return canAccess;
      }
      if (item.path === '/purchase-orders') {
        // Only FINANCE_MANAGER role can access Purchase Orders
        const canAccess = currentUser?.role === 'FINANCE_MANAGER';
        console.log('DEBUG: Purchase Orders access check:', canAccess, 'User role:', currentUser?.role);
        return canAccess;
      }
      if (item.path === '/user-management') {
        // Only SUPERADMIN role can access User Management
        const canAccess = currentUser?.role === 'SUPERADMIN';
        console.log('DEBUG: User Management access check:', canAccess, 'User role:', currentUser?.role);
        return canAccess;
      }
      if (item.path === '/master') {
        // Only SUPERADMIN role can access Master
        const canAccess = currentUser?.role === 'SUPERADMIN';
        console.log('DEBUG: Master access check:', canAccess, 'User role:', currentUser?.role);
        return canAccess;
      }
      return true; // Show all other menu items
    });
  }

  logout() {
    // Clear the session immediately
    this.authService.clearSession();
    
    // Navigate to login page
    this.router.navigate(['/login']);
    
    // Also try to logout from backend (but don't wait for it)
    this.authService.logout().subscribe({
      next: () => {
        console.log('Backend logout successful');
      },
      error: (error) => {
        console.error('Backend logout error:', error);
        // Don't worry about backend errors, we've already cleared the session
      }
    });
  }
}
