import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-department-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './department-management.component.html',
  styleUrls: ['./department-management.component.css']
})
export class DepartmentManagementComponent implements OnInit {
  
  departments: any[] = [];
  loading = false;
  showAddForm = false;
  editingDepartment: any = null;
  formData = {
    name: '',
    description: '',
    managerName: '',
    managerUsername: '',
    budget: 0
  };

  constructor(
    private apiService: ApiService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadDepartments();
  }

  loadDepartments() {
    this.loading = true;
    this.apiService.getAllDepartments().subscribe({
      next: (response) => {
        if (response.success) {
          this.departments = response.departments;
          this.toastService.success('Departments loaded successfully');
        } else {
          this.toastService.error(response.message || 'Failed to load departments');
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading departments:', error);
        this.toastService.error('Error loading departments');
        this.loading = false;
      }
    });
  }

  addDepartment() {
    if (!this.formData.name || !this.formData.description) {
      this.toastService.error('Name and description are required');
      return;
    }

    this.loading = true;
    this.apiService.createDepartment(
      this.formData.name,
      this.formData.description,
      this.formData.managerName || undefined,
      this.formData.managerUsername || undefined,
      this.formData.budget || undefined
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.toastService.success('Department created successfully');
          this.loadDepartments(); // Reload the list
          this.resetForm();
        } else {
          this.toastService.error(response.message || 'Failed to create department');
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error creating department:', error);
        this.toastService.error('Error creating department');
        this.loading = false;
      }
    });
  }

  editDepartment(department: any) {
    this.editingDepartment = { ...department };
    this.formData = {
      name: department.name,
      description: department.description,
      managerName: department.manager || '',
      managerUsername: department.managerUsername || '',
      budget: department.budget || 0
    };
    this.showAddForm = true;
  }

  updateDepartment() {
    if (!this.editingDepartment) {
      this.toastService.error('No department selected for editing');
      return;
    }

    if (!this.formData.name || !this.formData.description) {
      this.toastService.error('Name and description are required');
      return;
    }

    this.loading = true;
    this.apiService.updateDepartment(
      this.editingDepartment.id,
      this.formData.name,
      this.formData.description,
      this.formData.managerName || undefined,
      this.formData.managerUsername || undefined,
      this.formData.budget || undefined
    ).subscribe({
      next: (response) => {
        if (response.success) {
          this.toastService.success('Department updated successfully');
          this.loadDepartments(); // Reload the list
          this.resetForm();
        } else {
          this.toastService.error(response.message || 'Failed to update department');
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating department:', error);
        this.toastService.error('Error updating department');
        this.loading = false;
      }
    });
  }

  deleteDepartment(department: any) {
    if (confirm('Are you sure you want to delete this department?')) {
      this.loading = true;
      this.apiService.deleteDepartment(department.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.toastService.success('Department deleted successfully');
            this.loadDepartments(); // Reload the list
          } else {
            this.toastService.error(response.message || 'Failed to delete department');
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error deleting department:', error);
          this.toastService.error('Error deleting department');
          this.loading = false;
        }
      });
    }
  }

  resetForm() {
    this.formData = {
      name: '',
      description: '',
      managerName: '',
      managerUsername: '',
      budget: 0
    };
    this.showAddForm = false;
    this.editingDepartment = null;
  }

  getTotalBudget() {
    return this.departments.reduce((total, dept) => total + (dept.budget || 0), 0);
  }

  getActiveDepartments() {
    return this.departments.filter(dept => dept.active).length;
  }

  getFormTitle() {
    return this.editingDepartment ? 'Edit Department' : 'Add New Department';
  }

  getSubmitButtonText() {
    return this.editingDepartment ? 'Update Department' : 'Add Department';
  }
}
