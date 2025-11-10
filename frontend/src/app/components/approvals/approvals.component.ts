import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Requisition, RequisitionItem } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { AlertService } from '../../services/alert.service';

interface Budget {
  id: number;
  department: string;
  totalBudget: number;
  usedBudget: number;
  remainingBudget: number;
}

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './approvals.component.html',
  styleUrls: ['./approvals.component.css']
})
export class ApprovalsComponent implements OnInit {
  constructor(
    private apiService: ApiService, 
    public authService: AuthService,
    private alertService: AlertService
  ) {}

  protected readonly activeTab = signal<string>('department');
  protected readonly departmentPendings = signal<Requisition[]>([]);
  protected readonly itPendings = signal<Requisition[]>([]);
  protected readonly finPendings = signal<Requisition[]>([]);
  protected readonly budgets = signal<Budget[]>([]);
  protected readonly budgetLoading = signal<boolean>(false);
  

  ngOnInit() {
    console.log('DEBUG ApprovalsComponent: Initializing approvals component');
    console.log('DEBUG ApprovalsComponent: Current user:', this.authService.getCurrentUser());
    console.log('DEBUG ApprovalsComponent: canAccessITApprovals():', this.canAccessITApprovals());
    console.log('DEBUG ApprovalsComponent: canAccessFinanceApprovals():', this.canAccessFinanceApprovals());
    console.log('DEBUG ApprovalsComponent: isDepartmentManager():', this.isDepartmentManager());
    
    this.initializeActiveTab();
    this.loadApprovals();
    this.loadBudgets();
  }

  private initializeActiveTab() {
    // Set default tab based on user role
    if (this.authService.isDepartmentManager() && !this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      this.activeTab.set('department');
    } else if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      this.activeTab.set('it');
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      this.activeTab.set('finance');
    } else {
      // SUPERADMIN or users with multiple permissions - default to department
      this.activeTab.set('department');
    }
  }

  canAccessITApprovals(): boolean {
    return this.authService.canAccessITApprovals();
  }

  canAccessFinanceApprovals(): boolean {
    return this.authService.canAccessFinanceApprovals();
  }

  isDepartmentManager(): boolean {
    return this.authService.isDepartmentManager();
  }

  private loadApprovals() {
    // Load Department pending approvals if user is a Department Manager
    if (this.authService.isDepartmentManager()) {
      const sessionId = this.authService.getToken();
      if (sessionId) {
        this.apiService.getPendingDepartmentApprovals().subscribe({
          next: (response) => {
            console.log('DEBUG: Department approvals response:', response);
            if (response.success) {
              this.departmentPendings.set(response.requisitions || []);
            }
          },
          error: (error) => {
            console.error('Error loading Department approvals:', error);
          }
        });
      }
    }

    // Load IT pending approvals only if user has IT approval access
    if (this.authService.canAccessITApprovals()) {
      this.apiService.getPendingItApprovals().subscribe({
        next: (response) => {
          if (response.success) {
            this.itPendings.set(response.requisitions || []);
          }
        },
        error: (error) => {
          console.error('Error loading IT approvals:', error);
        }
      });
    }

    // Load Finance pending approvals only if user has Finance approval access
    if (this.authService.canAccessFinanceApprovals()) {
      this.apiService.getPendingFinanceApprovals().subscribe({
        next: (response) => {
          if (response.success) {
            this.finPendings.set(response.requisitions || []);
          }
        },
        error: (error) => {
          console.error('Error loading Finance approvals:', error);
        }
      });
    }
  }

  setActiveTab(tab: string) {
    this.activeTab.set(tab);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  itDecision(id: number, decision: string) {
    this.apiService.makeItDecision(id, decision).subscribe({
      next: (response) => {
        if (response.success) {
          console.log('IT Decision successful:', response);
          this.loadApprovals(); // Reload data
        } else {
          this.alertService.showError(response.message || 'Failed to process IT decision');
        }
      },
      error: (error) => {
        console.error('Error making IT decision:', error);
        this.alertService.showError('Failed to process IT decision. Please try again.');
      }
    });
  }

  financeDecision(id: number, decision: string) {
    this.apiService.makeFinanceDecision(id, decision).subscribe({
      next: (response) => {
        if (response.success) {
          console.log('Finance Decision successful:', response);
          this.loadApprovals(); // Reload data
          // Reload budget to show updated amounts after approval
          if (decision === 'APPROVE') {
            this.loadBudgets();
          }
        } else {
          this.alertService.showError(response.message || 'Failed to process Finance decision');
        }
      },
      error: (error) => {
        console.error('Error making Finance decision:', error);
        this.alertService.showError('Failed to process Finance decision. Please try again.');
      }
    });
  }

  departmentDecision(id: number, decision: string) {
    const sessionId = this.authService.getToken();
    if (!sessionId) {
      this.alertService.showError('No active session');
      return;
    }

    this.apiService.departmentDecision(id, decision).subscribe({
      next: (response) => {
        if (response.success) {
          console.log('Department Decision successful:', response);
          this.loadApprovals(); // Reload data
        } else {
          this.alertService.showError(response.message || 'Failed to process Department decision');
        }
      },
      error: (error) => {
        console.error('Error making Department decision:', error);
        this.alertService.showError('Failed to process Department decision. Please try again.');
      }
    });
  }

  // Helper methods for displaying requisition data
  getTotalQuantity(requisition: Requisition): number {
    if (requisition.items && requisition.items.length > 0) {
      return requisition.items.reduce((total, item) => total + item.quantity, 0);
    }
    return requisition.quantity || 0;
  }

  getRequisitionTotalAmount(requisition: Requisition): number {
    if (requisition.items && requisition.items.length > 0) {
      return requisition.items.reduce((total, item) => total + (item.quantity * item.price), 0);
    }
    return requisition.price || 0;
  }

  // Budget methods
  private loadBudgets() {
    this.budgetLoading.set(true);
    this.apiService.getBudgets().subscribe({
      next: (response) => {
        if (response.success) {

          this.budgets.set(response.budgets || []);
        }
        this.budgetLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading budgets:', error);
        this.budgetLoading.set(false);
      }
    });
  }

  getBudgetProgressPercentage(budget: Budget): number {
    if (budget.totalBudget === 0) return 0;
    return (budget.usedBudget / budget.totalBudget) * 100;
  }

  getBudgetStatusClass(budget: Budget): string {
    const percentage = this.getBudgetProgressPercentage(budget);
    if (percentage >= 90) return 'critical';
    if (percentage >= 75) return 'warning';
    return 'healthy';
  }

}