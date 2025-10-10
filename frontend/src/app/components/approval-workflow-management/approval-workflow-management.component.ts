import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-approval-workflow-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './approval-workflow-management.component.html',
  styleUrls: ['./approval-workflow-management.component.css']
})
export class ApprovalWorkflowManagementComponent implements OnInit {
  
  workflows: any[] = [];
  selectedDepartment = '';
  departments = ['Sales', 'IT', 'Finance', 'HR', 'Operations'];
  availableApprovers = ['Sales Manager', 'IT Manager', 'Finance Manager', 'HR Manager', 'Operations Manager'];
  
  showAddForm = false;
  editingWorkflow: any = null;
  formData = {
    department: '',
    step1: '',
    step2: '',
    step3: '',
    isActive: true
  };

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    this.loadWorkflows();
  }

  loadWorkflows() {
    // Simplified department-based workflows
    this.workflows = [
      { 
        id: 1, 
        department: 'Sales', 
        step1: 'Sales Manager',
        step2: 'IT Manager', 
        step3: 'Finance Manager',
        isActive: true
      },
      { 
        id: 2, 
        department: 'IT', 
        step1: 'IT Department Manager',
        step2: 'IT Manager', 
        step3: 'Finance Manager',
        isActive: true
      },
      { 
        id: 3, 
        department: 'Finance', 
        step1: 'Finance Department Manager',
        step2: 'IT Manager', 
        step3: 'Finance Manager',
        isActive: true
      },
      { 
        id: 4, 
        department: 'HR', 
        step1: 'HR Manager',
        step2: 'IT Manager', 
        step3: 'Finance Manager',
        isActive: true
      },
      { 
        id: 5, 
        department: 'Operations', 
        step1: 'Operations Manager',
        step2: 'IT Manager', 
        step3: 'Finance Manager',
        isActive: true
      }
    ];
  }

  addWorkflow() {
    if (this.formData.department && this.formData.step1) {
      const workflow = {
        id: this.workflows.length + 1,
        ...this.formData
      };
      this.workflows.push(workflow);
      this.resetForm();
    }
  }

  editWorkflow(workflow: any) {
    this.editingWorkflow = { ...workflow };
    this.formData = { ...workflow };
    this.showAddForm = true;
  }

  updateWorkflow() {
    if (this.editingWorkflow) {
      const index = this.workflows.findIndex((w: any) => w.id === this.editingWorkflow.id);
      if (index !== -1) {
        this.workflows[index] = { 
          ...this.editingWorkflow,
          ...this.formData
        };
        this.resetForm();
      }
    }
  }

  deleteWorkflow(workflow: any) {
    if (confirm('Are you sure you want to delete this workflow?')) {
      this.workflows = this.workflows.filter((w: any) => w.id !== workflow.id);
    }
  }

  toggleWorkflowStatus(workflow: any) {
    workflow.isActive = !workflow.isActive;
  }

  resetForm() {
    this.formData = {
      department: '',
      step1: '',
      step2: '',
      step3: '',
      isActive: true
    };
    this.showAddForm = false;
    this.editingWorkflow = null;
  }

  getActiveWorkflows() {
    return this.workflows.filter((workflow: any) => workflow.isActive).length;
  }

  getWorkflowsByDepartment() {
    const departments = [...new Set(this.workflows.map((w: any) => w.department))];
    return departments.length;
  }

  getFormTitle() {
    return this.editingWorkflow ? 'Edit Workflow' : 'Add New Workflow';
  }

  getSubmitButtonText() {
    return this.editingWorkflow ? 'Update Workflow' : 'Add Workflow';
  }

  getApprovalSteps(workflow: any) {
    const steps = [];
    if (workflow.step1) steps.push(workflow.step1);
    if (workflow.step2) steps.push(workflow.step2);
    if (workflow.step3) steps.push(workflow.step3);
    return steps;
  }

  getSelectedWorkflow() {
    if (!this.selectedDepartment) return null;
    return this.workflows.find((w: any) => w.department === this.selectedDepartment);
  }

  onDepartmentChange() {
    const workflow = this.getSelectedWorkflow();
    if (workflow) {
      this.formData = { ...workflow };
    } else {
      this.formData = {
        department: this.selectedDepartment,
        step1: '',
        step2: '',
        step3: '',
        isActive: true
      };
    }
  }
}
