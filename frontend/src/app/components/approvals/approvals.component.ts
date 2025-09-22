import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Requisition, RequisitionItem } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './approvals.component.html',
  styleUrls: ['./approvals.component.css']
})
export class ApprovalsComponent implements OnInit {
  constructor(private apiService: ApiService, private authService: AuthService) {}

  protected readonly activeTab = signal<string>('it');
  protected readonly itPendings = signal<Requisition[]>([]);
  protected readonly finPendings = signal<Requisition[]>([]);
  

  ngOnInit() {
    this.initializeActiveTab();
    this.loadApprovals();
  }

  private initializeActiveTab() {
    // Set default tab based on user role
    if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      this.activeTab.set('it');
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      this.activeTab.set('finance');
    } else {
      // SUPERADMIN or users with both permissions - default to IT
      this.activeTab.set('it');
    }
  }

  canAccessITApprovals(): boolean {
    return this.authService.canAccessITApprovals();
  }

  canAccessFinanceApprovals(): boolean {
    return this.authService.canAccessFinanceApprovals();
  }

  private loadApprovals() {
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
          alert(response.message || 'Failed to process IT decision');
        }
      },
      error: (error) => {
        console.error('Error making IT decision:', error);
        alert('Failed to process IT decision. Please try again.');
      }
    });
  }

  financeDecision(id: number, decision: string) {
    this.apiService.makeFinanceDecision(id, decision).subscribe({
      next: (response) => {
        if (response.success) {
          console.log('Finance Decision successful:', response);
          this.loadApprovals(); // Reload data
        } else {
          alert(response.message || 'Failed to process Finance decision');
        }
      },
      error: (error) => {
        console.error('Error making Finance decision:', error);
        alert('Failed to process Finance decision. Please try again.');
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

}
