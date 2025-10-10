import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

export interface User {
  id: number;
  username: string;
  fullName: string;
  role: string;
  department: string;
  isActive: boolean;
}

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  showCreateForm = false;
  loading = false;
  hasPermission = false;
  
  // Form data for creating new user
  newUser = {
    username: '',
    password: '',
    fullName: '',
    role: 'EMPLOYEE',
    department: ''
  };
  
  // Available roles and departments
  roles = [
    { value: 'EMPLOYEE', label: 'Employee' },
    { value: 'DEPARTMENT_MANAGER', label: 'Department Manager' },
    { value: 'IT_MANAGER', label: 'IT Manager' },
    { value: 'FINANCE_MANAGER', label: 'Finance Manager' },
    { value: 'SUPERADMIN', label: 'Super Admin' }
  ];
  
  departments = [
    'IT', 'Sales', 'Finance', 'Marketing', 'HR', 'Operations'
  ];

  constructor(
    private apiService: ApiService,
    public authService: AuthService
  ) {}

  ngOnInit() {
    // Check if user has SUPERADMIN role
    const currentUser = this.authService.getCurrentUser();
    this.hasPermission = currentUser?.role === 'SUPERADMIN';
    
    if (this.hasPermission) {
      this.loadUsers();
    } else {
      console.log('DEBUG: User does not have SUPERADMIN role, cannot access user management');
    }
  }

  loadUsers() {
    this.loading = true;
    
    // Debug: Check current user and role
    const currentUser = this.authService.getCurrentUser();
    console.log('DEBUG: Current user:', currentUser);
    console.log('DEBUG: User role:', currentUser?.role);
    console.log('DEBUG: Is SUPERADMIN?', currentUser?.role === 'SUPERADMIN');
    
    this.apiService.getAllUsers().subscribe({
      next: (response) => {
        console.log('DEBUG: Users response:', response);
        if (response.success) {
          this.users = response.users || [];
          console.log('DEBUG: Loaded users count:', this.users.length);
        } else {
          console.error('DEBUG: Failed to load users:', response.message);
          alert('Failed to load users: ' + (response.message || 'Unknown error'));
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        console.error('Error details:', error);
        alert('Error loading users: ' + (error.message || 'Network error'));
        this.loading = false;
      }
    });
  }

  openCreateForm() {
    this.showCreateForm = true;
    this.resetForm();
  }

  closeCreateForm() {
    this.showCreateForm = false;
    this.resetForm();
  }

  resetForm() {
    this.newUser = {
      username: '',
      password: '',
      fullName: '',
      role: 'EMPLOYEE',
      department: ''
    };
  }

  createUser() {
    if (!this.newUser.username || !this.newUser.password || !this.newUser.fullName || !this.newUser.department) {
      alert('Please fill in all required fields');
      return;
    }

    this.loading = true;
    this.apiService.createUser(
      this.newUser.username,
      this.newUser.password,
      this.newUser.fullName,
      this.newUser.role,
      this.newUser.department
    ).subscribe({
      next: (response) => {
        console.log('DEBUG: Create user response:', response);
        if (response.success) {
          alert('✅ User created successfully!');
          this.closeCreateForm();
          this.loadUsers(); // Reload the user list
        } else {
          alert('Failed to create user: ' + (response.message || 'Unknown error'));
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error creating user:', error);
        alert('Error creating user: ' + error.message);
        this.loading = false;
      }
    });
  }

  toggleUserStatus(user: User) {
    const action = user.isActive ? 'deactivate' : 'activate';
    if (confirm(`Are you sure you want to ${action} user "${user.username}"?`)) {
      this.loading = true;
      this.apiService.updateUserStatus(user.id, !user.isActive).subscribe({
        next: (response) => {
          console.log('DEBUG: Update user status response:', response);
          if (response.success) {
            user.isActive = !user.isActive;
            alert(`User ${action}d successfully!`);
          } else {
            alert('Failed to update user status: ' + (response.message || 'Unknown error'));
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error updating user status:', error);
          alert('Error updating user status: ' + error.message);
          this.loading = false;
        }
      });
    }
  }

  getRoleLabel(role: string): string {
    const roleObj = this.roles.find(r => r.value === role);
    return roleObj ? roleObj.label : role;
  }

  getStatusClass(isActive: boolean): string {
    return isActive ? 'status-active' : 'status-inactive';
  }

  getStatusText(isActive: boolean): string {
    return isActive ? 'Active' : 'Inactive';
  }
}
