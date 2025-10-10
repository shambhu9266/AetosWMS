import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

interface Requisition {
  id: number;
  itemName: string;
  quantity: number;
  price: number;
  createdBy: string;
  department: string;
  status: string;
  createdAt: string;
  items?: any[];
}

@Component({
  selector: 'app-department-manager-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './department-manager-dashboard.component.html',
  styleUrls: ['./department-manager-dashboard.component.css']
})
export class DepartmentManagerDashboardComponent implements OnInit {
  pendingRequisitions: Requisition[] = [];
  pendingPdfs: any[] = [];
  loading = false;
  error: string | null = null;
  selectedRequisition: Requisition | null = null;
  decision = '';
  comments = '';
  activeTab = 'requisitions'; // 'requisitions' or 'pdfs'

  constructor(
    private apiService: ApiService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.loadPendingRequisitions();
    this.loadPendingPdfs();
  }

  loadPendingRequisitions() {
    this.loading = true;
    this.error = null;
    
    const sessionId = this.authService.getToken();
    console.log('DEBUG: Loading pending requisitions with sessionId:', sessionId);
    if (!sessionId) {
      this.error = 'No active session';
      this.loading = false;
      return;
    }

    this.apiService.getPendingDepartmentRequisitions().subscribe({
      next: (response: any) => {
        console.log('DEBUG: Department manager API response:', response);
        if (response.success) {
          this.pendingRequisitions = response.requisitions;
          console.log('DEBUG: Loaded pending requisitions:', this.pendingRequisitions);
        } else {
          this.error = response.message || 'Failed to load pending requisitions';
          console.error('DEBUG: API returned error:', response.message);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading pending requisitions:', error);
        this.error = 'Error loading pending requisitions';
        this.loading = false;
      }
    });
  }

  selectRequisition(requisition: Requisition) {
    this.selectedRequisition = requisition;
    this.decision = '';
    this.comments = '';
  }

  makeDecision() {
    if (!this.selectedRequisition || !this.decision) {
      this.error = 'Please select a decision';
      return;
    }

    this.loading = true;
    const sessionId = this.authService.getToken();
    
    this.apiService.departmentDecision(
      this.selectedRequisition.id,
      this.decision,
      this.comments
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          // Reload the list
          this.loadPendingRequisitions();
          this.selectedRequisition = null;
          this.decision = '';
          this.comments = '';
        } else {
          this.error = response.message || 'Failed to process decision';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error making decision:', error);
        this.error = 'Error processing decision';
        this.loading = false;
      }
    });
  }

  getItemDescription(requisition: Requisition | null): string {
    if (!requisition) return '';
    if (requisition.items && requisition.items.length > 0) {
      return `${requisition.items.length} items (${requisition.items.map((item: any) => item.itemName).join(', ')})`;
    }
    return `${requisition.quantity} ${requisition.itemName}`;
  }

  getTotalAmount(requisition: Requisition | null): number {
    if (!requisition) return 0;
    if (requisition.items && requisition.items.length > 0) {
      return requisition.items.reduce((total: number, item: any) => total + (item.price * item.quantity), 0);
    }
    return requisition.price * requisition.quantity;
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  loadPendingPdfs() {
    this.loading = true;
    this.error = null;
    
    this.apiService.getDepartmentPendingPdfs().subscribe({
      next: (response: any) => {
        console.log('DEBUG: Department pending PDFs response:', response);
        if (response.success) {
          this.pendingPdfs = response.pdfs || [];
          console.log('DEBUG: Loaded pending PDFs:', this.pendingPdfs);
        } else {
          this.error = response.message || 'Failed to load pending PDFs';
          console.error('DEBUG: API returned error:', response.message);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading pending PDFs:', error);
        this.error = 'Error loading pending PDFs';
        this.loading = false;
      }
    });
  }

  approvePdf(pdfId: number) {
    this.loading = true;
    this.error = null;
    
    this.apiService.departmentApprovePdf(pdfId).subscribe({
      next: (response: any) => {
        if (response.success) {
          console.log('PDF approved successfully:', response);
          this.loadPendingPdfs(); // Reload PDFs
        } else {
          this.error = response.message || 'Failed to approve PDF';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error approving PDF:', error);
        this.error = 'Error approving PDF';
        this.loading = false;
      }
    });
  }

  rejectPdf(pdfId: number, rejectionReason: string) {
    this.loading = true;
    this.error = null;
    
    this.apiService.rejectPdf(pdfId, rejectionReason).subscribe({
      next: (response: any) => {
        if (response.success) {
          console.log('PDF rejected successfully:', response);
          this.loadPendingPdfs(); // Reload PDFs
        } else {
          this.error = response.message || 'Failed to reject PDF';
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error rejecting PDF:', error);
        this.error = 'Error rejecting PDF';
        this.loading = false;
      }
    });
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  downloadPdf(pdfId: number) {
    this.apiService.downloadPdf(pdfId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `pdf-${pdfId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error downloading PDF:', error);
        this.error = 'Error downloading PDF';
      }
    });
  }
}
