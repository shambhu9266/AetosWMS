import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService, Requisition, Notification, VendorPdf, UserPdfGroup, RequisitionPdfGroup } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  constructor(private apiService: ApiService, public authService: AuthService) {}

  // Real data from API
  protected readonly stats = signal({
    pendingApprovals: 0,
    approvedThisMonth: 0,
    activeOrders: 0,
    // Employee-specific stats
    myRequisitions: 0,
    pendingMyRequisitions: 0,
    approvedMyRequisitions: 0,
    rejectedMyRequisitions: 0
  });

  protected readonly recentRequisitions = signal<Requisition[]>([]);
  protected readonly pendingApprovals = signal<Requisition[]>([]);
  protected readonly notifications = signal<Notification[]>([]);
  protected readonly uploadedPdfs = signal<VendorPdf[]>([]);
  protected readonly userPdfGroups = signal<UserPdfGroup[]>([]);
  protected readonly requisitionPdfGroups = signal<RequisitionPdfGroup[]>([]);
  private expandedUserSlots = new Set<string>();
  private expandedRequisitionSlots = new Set<number>();

  // Modal properties
  protected readonly selectedRequisition = signal<Requisition | null>(null);
  protected readonly showRequisitionModal = signal<boolean>(false);
  
  // PDF upload modal properties
  protected readonly showAddPdfModal = signal<boolean>(false);
  protected readonly selectedRequisitionIdForPdf = signal<number | null>(null);
  protected readonly selectedFileForUpload = signal<File | null>(null);
  protected readonly pdfDescription = signal<string>('');
  protected readonly isUploading = signal<boolean>(false);

  // Computed properties for employee dashboard
  get myRecentRequisitions(): Requisition[] {
    // Show all recent requisitions (last 5, regardless of status)
    return this.recentRequisitions().slice(0, 5);
  }

  get myPendingRequisitions(): Requisition[] {
    // Show only pending/under-review requisitions
    return this.recentRequisitions().filter(req => 
      req.status === 'PENDING_DEPARTMENT_APPROVAL' || 
      req.status === 'PENDING_IT_APPROVAL' || 
      req.status === 'PENDING_FINANCE_APPROVAL' ||
      req.status === 'SENT_BACK'
    );
  }

  get myUploadedPdfs(): VendorPdf[] {
    return this.uploadedPdfs();
  }

  ngOnInit() {
    this.loadDashboardData();
  }

  private loadDashboardData() {
    // Load data based on user role
    if (this.authService.isEmployee()) {
      // Load employee-specific data
      this.loadEmployeeDashboardData();
    } else {
      // Load manager/admin data
      this.loadManagerDashboardData();
    }

      // Load notifications for all users
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

  private loadEmployeeDashboardData() {
    // Load employee's own requisitions
    this.apiService.getRequisitions().subscribe({
      next: (response) => {
        if (response.success) {
          const allRequisitions = response.requisitions || [];
          const currentUser = this.authService.getCurrentUser();
          
          // Filter to only show employee's own requisitions
          const myRequisitions = allRequisitions.filter((req: Requisition) => 
            req.createdBy === currentUser?.username
          );
          
          this.recentRequisitions.set(myRequisitions);
          
          // Calculate employee-specific stats
          console.log('DEBUG: All my requisitions:', myRequisitions);
          console.log('DEBUG: Requisition statuses:', myRequisitions.map((req: Requisition) => ({ id: req.id, status: req.status })));
          
          const pendingCount = myRequisitions.filter((req: Requisition) => 
            req.status === 'PENDING_DEPARTMENT_APPROVAL' || req.status === 'PENDING_IT_APPROVAL' || req.status === 'PENDING_FINANCE_APPROVAL' || req.status === 'SENT_BACK'
          ).length;
          
          const approvedCount = myRequisitions.filter((req: Requisition) => 
            req.status === 'APPROVED'
          ).length;
          
          const rejectedCount = myRequisitions.filter((req: Requisition) => 
            req.status === 'REJECTED'
          ).length;
          
          console.log('DEBUG: Pending count:', pendingCount);
          console.log('DEBUG: Approved count:', approvedCount);
          console.log('DEBUG: Rejected count:', rejectedCount);
          
          this.stats.update(current => ({
            ...current,
            myRequisitions: myRequisitions.length,
            pendingMyRequisitions: pendingCount,
            approvedMyRequisitions: approvedCount,
            rejectedMyRequisitions: rejectedCount
          }));
        }
      },
      error: (error) => {
        console.error('Error loading employee requisitions:', error);
      }
    });

    // Load employee's uploaded PDFs
    this.loadEmployeePdfs();
  }

  loadEmployeePdfs() {
    this.apiService.getPdfs().subscribe({
      next: (response) => {
        if (response.success) {
          const allPdfs = response.pdfs || [];
          const currentUser = this.authService.getCurrentUser();
          
          // Filter to only show employee's own uploaded PDFs
          const myPdfs = allPdfs.filter((pdf: VendorPdf) => 
            pdf.uploadedBy === currentUser?.username
          );
          
          this.uploadedPdfs.set(myPdfs);
          console.log('DEBUG: Employee PDFs loaded:', myPdfs.length);
          
          // Group PDFs by user for employee view (same as superadmin)
          const userGroups = this.apiService.groupPdfsByUser(myPdfs);
          this.userPdfGroups.set(userGroups);
          
          // Also load requisitions and group PDFs by requisition
          this.loadRequisitionsForEmployeeGrouping(myPdfs);
        }
      },
      error: (error) => {
        console.error('Error loading employee PDFs:', error);
      }
    });
  }

  private loadRequisitionsForEmployeeGrouping(pdfs: VendorPdf[]) {
    console.log('DEBUG: Loading requisitions for employee grouping. PDFs:', pdfs);
    this.apiService.getRequisitions().subscribe({
      next: (response: any) => {
        console.log('DEBUG: Requisitions response:', response);
        if (response.success) {
          const requisitions = response.requisitions || [];
          console.log('DEBUG: Available requisitions count:', requisitions.length);
          console.log('DEBUG: Available requisitions:', requisitions.map((req: Requisition) => ({
            id: req.id,
            itemName: req.itemName,
            department: req.department
          })));
          // Group PDFs by requisition
          const requisitionGroups = this.apiService.groupPdfsByRequisition(pdfs, requisitions);
          console.log('DEBUG: Requisition groups created count:', requisitionGroups.length);
          console.log('DEBUG: Requisition groups details:', requisitionGroups.map((group: RequisitionPdfGroup) => ({
            requisitionId: group.requisitionId,
            pdfCount: group.pdfs.length,
            itemName: group.requisitionInfo?.itemName || 'Unlinked'
          })));
          this.requisitionPdfGroups.set(requisitionGroups);
        }
      },
      error: (error: any) => {
        console.error('Error loading requisitions for employee grouping:', error);
      }
    });
  }

  private loadManagerDashboardData() {
    // Check if user is SUPERADMIN - they should see ALL data
    if (this.authService.hasRole('SUPERADMIN')) {
      this.loadSuperAdminData();
      return;
    }

    // Load approvals based on user role
    if (this.authService.canAccessITApprovals()) {
      // Load pending IT approvals for IT managers
      this.apiService.getPendingItApprovals().subscribe({
        next: (response) => {
          if (response.success) {
            this.pendingApprovals.set(response.requisitions || []);
            this.updateStats();
          }
        },
        error: (error) => {
          console.error('Error loading IT approvals:', error);
        }
      });
    }

    if (this.authService.canAccessFinanceApprovals()) {
      // Load pending Finance approvals for Finance managers
      this.apiService.getPendingFinanceApprovals().subscribe({
        next: (response) => {
          if (response.success) {
            // For Finance managers, only show Finance approvals
            if (this.authService.hasRole('FINANCE_MANAGER')) {
              this.pendingApprovals.set(response.requisitions || []);
            } else {
              // For SUPERADMIN, combine both IT and Finance approvals
              const currentApprovals = this.pendingApprovals();
              this.pendingApprovals.set([...currentApprovals, ...(response.requisitions || [])]);
            }
            this.updateStats();
          }
        },
        error: (error) => {
          console.error('Error loading Finance approvals:', error);
        }
      });
    }

    // Load department manager data
    if (this.authService.isDepartmentManager()) {
      // Load pending department approvals
      this.apiService.getPendingDepartmentApprovals().subscribe({
        next: (response) => {
          if (response.success) {
            this.pendingApprovals.set(response.requisitions || []);
            this.updateStats();
          }
        },
        error: (error) => {
          console.error('Error loading department approvals:', error);
        }
      });

      // Load recent requisitions for department manager
      this.apiService.getRecentRequisitions().subscribe({
        next: (response) => {
          if (response.success) {
            this.recentRequisitions.set(response.requisitions || []);
          }
        },
        error: (error) => {
          console.error('Error loading recent requisitions:', error);
        }
      });
    }

    // Load approved this month
    this.apiService.getApprovedThisMonth().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats.update(current => ({
            ...current,
            approvedThisMonth: response.requisitions?.length || 0
          }));
        }
      },
      error: (error) => {
        console.error('Error loading approved this month:', error);
      }
    });

    // Load active orders
    this.apiService.getActiveOrders().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats.update(current => ({
            ...current,
            activeOrders: response.requisitions?.length || 0
          }));
        }
      },
      error: (error) => {
        console.error('Error loading active orders:', error);
      }
    });

    // Load PDFs if user is Finance Manager, IT Manager, Department Manager, or SUPERADMIN
    if (this.authService.canAccessFinanceApprovals() || this.authService.canAccessITApprovals() || this.authService.isDepartmentManager() || this.authService.hasRole('SUPERADMIN')) {
      this.loadPdfs();
    }
  }

  private loadSuperAdminData() {
    console.log('DEBUG: Loading SUPERADMIN data - comprehensive data loading');
    console.log('DEBUG: Current user role:', this.authService.getCurrentUser()?.role);
    console.log('DEBUG: Is SUPERADMIN:', this.authService.hasRole('SUPERADMIN'));
    
    // Load ALL pending approvals (Department, IT, and Finance)
    this.loadAllPendingApprovals();
    
    // Load recent requisitions
    console.log('DEBUG: Calling getRecentRequisitions...');
    this.apiService.getRecentRequisitions().subscribe({
      next: (response) => {
        console.log('DEBUG: Recent requisitions response:', response);
        if (response.success) {
          this.recentRequisitions.set(response.requisitions || []);
          console.log('DEBUG: SUPERADMIN recent requisitions loaded:', response.requisitions?.length || 0);
        } else {
          console.error('DEBUG: Recent requisitions failed:', response.message);
        }
      },
      error: (error) => {
        console.error('Error loading recent requisitions for SUPERADMIN:', error);
      }
    });

    // Load approved this month
    this.apiService.getApprovedThisMonth().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats.update(current => ({
            ...current,
            approvedThisMonth: response.requisitions?.length || 0
          }));
        }
      },
      error: (error) => {
        console.error('Error loading approved this month:', error);
      }
    });

    // Load active orders
    this.apiService.getActiveOrders().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats.update(current => ({
            ...current,
            activeOrders: response.requisitions?.length || 0
          }));
        }
      },
      error: (error) => {
        console.error('Error loading active orders:', error);
      }
    });

    // Load PDFs (SUPERADMIN sees all PDFs)
    this.loadPdfs();
  }

  private loadAllPendingApprovals() {
    console.log('DEBUG: Loading ALL pending approvals for SUPERADMIN');
    console.log('DEBUG: Session ID:', this.authService.getToken());
    let allApprovals: Requisition[] = [];
    let completedRequests = 0;
    const totalRequests = 3; // Department, IT, Finance

    const checkCompletion = () => {
      completedRequests++;
      if (completedRequests === totalRequests) {
        console.log('DEBUG: All approvals loaded for SUPERADMIN:', allApprovals.length);
        this.pendingApprovals.set(allApprovals);
        this.updateStats();
      }
    };

    // Load Department approvals
    console.log('DEBUG: Calling getPendingDepartmentApprovals...');
    this.apiService.getPendingDepartmentApprovals().subscribe({
      next: (response) => {
        console.log('DEBUG: Department approvals response:', response);
        if (response.success) {
          allApprovals = [...allApprovals, ...(response.requisitions || [])];
          console.log('DEBUG: Department approvals loaded:', response.requisitions?.length || 0);
        } else {
          console.error('DEBUG: Department approvals failed:', response.message);
        }
        checkCompletion();
      },
      error: (error) => {
        console.error('Error loading department approvals:', error);
        checkCompletion();
      }
    });

    // Load IT approvals
    console.log('DEBUG: Calling getPendingItApprovals...');
    this.apiService.getPendingItApprovals().subscribe({
      next: (response) => {
        console.log('DEBUG: IT approvals response:', response);
        if (response.success) {
          allApprovals = [...allApprovals, ...(response.requisitions || [])];
          console.log('DEBUG: IT approvals loaded:', response.requisitions?.length || 0);
        } else {
          console.error('DEBUG: IT approvals failed:', response.message);
        }
        checkCompletion();
      },
      error: (error) => {
        console.error('Error loading IT approvals:', error);
        checkCompletion();
      }
    });

    // Load Finance approvals
    console.log('DEBUG: Calling getPendingFinanceApprovals...');
    this.apiService.getPendingFinanceApprovals().subscribe({
      next: (response) => {
        console.log('DEBUG: Finance approvals response:', response);
        if (response.success) {
          allApprovals = [...allApprovals, ...(response.requisitions || [])];
          console.log('DEBUG: Finance approvals loaded:', response.requisitions?.length || 0);
        } else {
          console.error('DEBUG: Finance approvals failed:', response.message);
        }
        checkCompletion();
      },
      error: (error) => {
        console.error('Error loading Finance approvals:', error);
        checkCompletion();
      }
    });
  }

  loadPdfs() {
    console.log('DEBUG: Loading PDFs...');
    
    // Use role-specific PDF loading
    if (this.authService.isDepartmentManager() && !this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      // Department Manager - load ALL PDFs (including approved ones) to track progress
      this.apiService.getAllDepartmentPdfs().subscribe({
        next: (response) => {
          console.log('DEBUG: All Department PDFs response:', response);
          if (response.success) {
            const pdfs = response.pdfs || [];
            console.log('DEBUG: Loaded all Department PDFs count:', pdfs.length);
            this.uploadedPdfs.set(pdfs);
            this.loadRequisitionsForGrouping(pdfs);
          }
        },
        error: (error) => {
          console.error('Error loading all Department PDFs:', error);
        }
      });
    } else if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      // IT Manager - load ALL PDFs (including approved ones) to track progress
      this.apiService.getAllItPdfs().subscribe({
        next: (response) => {
          console.log('DEBUG: All IT PDFs response:', response);
          if (response.success) {
            const pdfs = response.pdfs || [];
            console.log('DEBUG: Loaded all IT PDFs count:', pdfs.length);
            this.uploadedPdfs.set(pdfs);
            this.loadRequisitionsForGrouping(pdfs);
          }
        },
        error: (error) => {
          console.error('Error loading all IT PDFs:', error);
        }
      });
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      // Finance Manager - load ALL PDFs but filter to only show IT-approved ones
      this.apiService.getPdfs().subscribe({
        next: (response) => {
          console.log('DEBUG: All PDFs response for Finance Manager:', response);
          if (response.success) {
            const allPdfs = response.pdfs || [];
            console.log('DEBUG: Loaded all PDFs count for Finance Manager:', allPdfs.length);
            
            // Filter to only show PDFs that are IT-approved (FINANCE stage or APPROVED stage)
            const financeRelevantPdfs = allPdfs.filter((pdf: any) => {
              const stage = pdf.approvalStage;
              return stage === 'FINANCE' || stage === 'APPROVED';
            });
            
            console.log('DEBUG: Filtered to IT-approved PDFs count:', financeRelevantPdfs.length);
            this.uploadedPdfs.set(financeRelevantPdfs);
            this.loadRequisitionsForGrouping(financeRelevantPdfs);
          }
        },
        error: (error) => {
          console.error('Error loading all PDFs for Finance Manager:', error);
        }
      });
    } else {
      // SUPERADMIN or other roles - load all PDFs
      this.apiService.getPdfs().subscribe({
        next: (response) => {
          console.log('DEBUG: All PDFs response:', response);
          if (response.success) {
            const pdfs = response.pdfs || [];
            console.log('DEBUG: Loaded all PDFs count:', pdfs.length);
            this.uploadedPdfs.set(pdfs);
            this.loadRequisitionsForGrouping(pdfs);
          }
        },
        error: (error) => {
          console.error('Error loading all PDFs:', error);
        }
      });
    }
  }

  private loadRequisitionsForGrouping(pdfs: VendorPdf[]) {
    console.log('DEBUG: Loading requisitions for grouping. PDFs:', pdfs);
    this.apiService.getRequisitions().subscribe({
      next: (response: any) => {
        console.log('DEBUG: Requisitions response:', response);
        if (response.success) {
          const requisitions = response.requisitions || [];
          console.log('DEBUG: Available requisitions count:', requisitions.length);
          console.log('DEBUG: Available requisitions:', requisitions.map((req: Requisition) => ({
            id: req.id,
            itemName: req.itemName,
            department: req.department
          })));
          // Group PDFs by requisition
          const requisitionGroups = this.apiService.groupPdfsByRequisition(pdfs, requisitions);
          console.log('DEBUG: Requisition groups created count:', requisitionGroups.length);
          console.log('DEBUG: Requisition groups details:', requisitionGroups.map((group: RequisitionPdfGroup) => ({
            requisitionId: group.requisitionId,
            pdfCount: group.pdfs.length,
            itemName: group.requisitionInfo?.itemName || 'Unlinked'
          })));
          this.requisitionPdfGroups.set(requisitionGroups);
        }
      },
      error: (error: any) => {
        console.error('Error loading requisitions for grouping:', error);
      }
    });
  }

  private updateStats() {
    const pendingCount = this.pendingApprovals().length;
    this.stats.update(current => ({
      ...current,
      pendingApprovals: pendingCount
    }));
  }


  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  getPriorityClass(priority: string): string {
    return `priority-${priority.toLowerCase()}`;
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  getCurrentDate(): string {
    return new Date().toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  getCurrentTime(): string {
    return new Date().toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  quickApprove(id: number) {
    // Make decision based on user role
    if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      // IT Manager only
      this.apiService.makeItDecision(id, 'APPROVE').subscribe({
        next: (response) => {
          if (response.success) {
            this.loadDashboardData(); // Reload data
          }
        },
        error: (error) => {
          console.error('Error approving requisition:', error);
        }
      });
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      // Finance Manager only
      this.apiService.makeFinanceDecision(id, 'APPROVE').subscribe({
        next: (response) => {
          if (response.success) {
            this.loadDashboardData(); // Reload data
          }
        },
        error: (error) => {
          console.error('Error approving requisition:', error);
        }
      });
    } else if (this.authService.hasRole('SUPERADMIN')) {
      // SUPERADMIN - try IT first, then Finance
      this.apiService.makeItDecision(id, 'APPROVE').subscribe({
        next: (response) => {
          if (response.success) {
            this.loadDashboardData(); // Reload data
          }
        },
        error: () => {
          // If IT decision fails, try Finance decision
          this.apiService.makeFinanceDecision(id, 'APPROVE').subscribe({
            next: (response) => {
              if (response.success) {
                this.loadDashboardData(); // Reload data
              }
            },
            error: (error) => {
              console.error('Error approving requisition:', error);
            }
          });
        }
      });
    }
  }

  quickReject(id: number) {
    // Make decision based on user role
    if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      // IT Manager only
      this.apiService.makeItDecision(id, 'REJECT').subscribe({
        next: (response) => {
          if (response.success) {
            this.loadDashboardData(); // Reload data
          }
        },
        error: (error) => {
          console.error('Error rejecting requisition:', error);
        }
      });
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      // Finance Manager only
      this.apiService.makeFinanceDecision(id, 'REJECT').subscribe({
        next: (response) => {
          if (response.success) {
            this.loadDashboardData(); // Reload data
          }
        },
        error: (error) => {
          console.error('Error rejecting requisition:', error);
        }
      });
    } else if (this.authService.hasRole('SUPERADMIN')) {
      // SUPERADMIN - try IT first, then Finance
      this.apiService.makeItDecision(id, 'REJECT').subscribe({
        next: (response) => {
          if (response.success) {
            this.loadDashboardData(); // Reload data
          }
        },
        error: () => {
          // If IT decision fails, try Finance decision
          this.apiService.makeFinanceDecision(id, 'REJECT').subscribe({
            next: (response) => {
              if (response.success) {
                this.loadDashboardData(); // Reload data
              }
            },
            error: (error) => {
              console.error('Error rejecting requisition:', error);
            }
          });
        }
      });
    }
  }

  // PDF Management Methods for Finance Managers
  downloadPdf(pdf: VendorPdf) {
    console.log('DEBUG: Downloading PDF:', pdf.id, pdf.originalFileName);
    this.apiService.downloadPdf(pdf.id).subscribe({
      next: (blob) => {
        console.log('DEBUG: PDF download successful, blob size:', blob.size);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = pdf.originalFileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error downloading PDF:', error);
        alert('Failed to download PDF. Please try again.');
      }
    });
  }

  markPdfAsProcessed(pdf: VendorPdf) {
    console.log('DEBUG: Approving PDF:', pdf.id, pdf.originalFileName);
    console.log('DEBUG: User role check - isDepartmentManager:', this.authService.isDepartmentManager(), 'canAccessITApprovals:', this.authService.canAccessITApprovals(), 'canAccessFinanceApprovals:', this.authService.canAccessFinanceApprovals());
    
    // Use role-specific approval methods
    if (this.authService.isDepartmentManager() && !this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      // Department Manager approves PDF
      console.log('DEBUG: Using Department Manager approval');
      this.apiService.departmentApprovePdf(pdf.id).subscribe({
        next: (response) => {
          console.log('DEBUG: Department approval response:', response);
          if (response.success) {
            alert('PDF approved by Department Manager successfully!');
            // Reload PDFs to show updated list
            setTimeout(() => {
              this.loadPdfs();
            }, 1000);
          } else {
            alert('Failed to approve PDF: ' + (response.message || 'Unknown error'));
          }
        },
        error: (error) => {
          console.error('Error approving PDF:', error);
          alert('Failed to approve PDF. Please try again. Error: ' + error.message);
        }
      });
    } else if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      // IT Manager approves PDF
      console.log('DEBUG: Using IT Manager approval');
      this.apiService.itApprovePdf(pdf.id).subscribe({
        next: (response) => {
          console.log('DEBUG: IT approval response:', response);
          if (response.success) {
            console.log('DEBUG: PDF approved successfully, reloading dashboard...');
            alert('✅ PDF approved by IT Manager successfully!\n\n📋 The PDF has been moved to Finance Manager for review.\n\n🔄 Your dashboard will refresh to show the updated list.');
            // Reload PDFs to show updated list
            setTimeout(() => {
              console.log('DEBUG: Reloading PDFs after IT approval...');
              this.loadPdfs();
            }, 1000);
          } else {
            alert('Failed to approve PDF: ' + (response.message || 'Unknown error'));
          }
        },
        error: (error) => {
          console.error('Error approving PDF:', error);
          alert('Failed to approve PDF. Please try again. Error: ' + error.message);
        }
      });
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      // Finance Manager approves PDF
      this.apiService.financeApprovePdf(pdf.id).subscribe({
        next: (response) => {
          if (response.success) {
            alert('PDF approved by Finance Manager successfully!');
            this.loadPdfs();
          } else {
            alert(response.message || 'Failed to approve PDF');
          }
        },
        error: (error) => {
          console.error('Error approving PDF:', error);
          alert('Failed to approve PDF. Please try again.');
        }
      });
    } else {
      // Fallback to old method for SUPERADMIN
      this.apiService.markPdfAsProcessed(pdf.id).subscribe({
        next: (response) => {
          if (response.success) {
            alert('PDF marked as processed successfully!');
            this.loadPdfs();
          } else {
            alert('Failed to mark PDF as processed: ' + response.message);
          }
        },
        error: (error) => {
          console.error('Error marking PDF as processed:', error);
          alert('Failed to mark PDF as processed. Please try again.');
        }
      });
    }
  }

  rejectPdf(pdf: VendorPdf) {
    console.log('DEBUG: Rejecting PDF:', pdf.id, pdf.originalFileName);
    const rejectionReason = prompt('Please provide a reason for rejecting this PDF:');
    if (rejectionReason && rejectionReason.trim()) {
      console.log('DEBUG: Rejection reason:', rejectionReason.trim());
      this.apiService.rejectPdf(pdf.id, rejectionReason.trim()).subscribe({
        next: (response) => {
          console.log('DEBUG: Reject PDF response:', response);
          if (response.success) {
            alert('PDF rejected successfully!');
            this.loadPdfs(); // Reload PDFs list
          } else {
            alert('Failed to reject PDF: ' + response.message);
          }
        },
        error: (error) => {
          console.error('Error rejecting PDF:', error);
          alert('Failed to reject PDF. Please try again.');
        }
      });
    }
  }

  getApprovalButtonText(): string {
    if (this.authService.isDepartmentManager() && !this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      return 'Department Approve';
    } else if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      return 'IT Approve';
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      return 'Finance Approve';
    } else {
      return 'Approve';
    }
  }

  getApprovalButtonTitle(): string {
    if (this.authService.isDepartmentManager() && !this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      return 'Approve PDF for Department Manager';
    } else if (this.authService.canAccessITApprovals() && !this.authService.canAccessFinanceApprovals()) {
      return 'Approve PDF for IT Manager';
    } else if (this.authService.canAccessFinanceApprovals() && !this.authService.canAccessITApprovals()) {
      return 'Approve PDF for Finance Manager';
    } else {
      return 'Approve PDF';
    }
  }

  // Status methods for Department Managers
  getPdfStatusClass(pdf: any): string {
    if (pdf.rejected) {
      return 'status-rejected';
    }
    
    const stage = pdf.approvalStage || 'DEPARTMENT';
    switch (stage) {
      case 'DEPARTMENT':
        return 'status-pending';
      case 'IT':
        return 'status-approved';
      case 'FINANCE':
        return 'status-it-approved';
      case 'APPROVED':
        return 'status-final-approved';
      default:
        return 'status-pending';
    }
  }

  getStatusIcon(pdf: any): string {
    if (pdf.rejected) {
      return '❌';
    }
    
    const stage = pdf.approvalStage || 'DEPARTMENT';
    switch (stage) {
      case 'DEPARTMENT':
        return '⏳';
      case 'IT':
        return '✅';
      case 'FINANCE':
        return '🔍';
      case 'APPROVED':
        return '🎉';
      default:
        return '⏳';
    }
  }

  getStatusText(pdf: any): string {
    if (pdf.rejected) {
      return 'Rejected';
    }
    
    const stage = pdf.approvalStage || 'DEPARTMENT';
    switch (stage) {
      case 'DEPARTMENT':
        return 'Pending Department Approval';
      case 'IT':
        return 'Approved by Department - With IT Manager';
      case 'FINANCE':
        return 'Approved by IT - With Finance Manager';
      case 'APPROVED':
        return 'Fully Approved';
      default:
        return 'Pending Department Approval';
    }
  }

  // Status methods for IT Managers
  getItPdfStatusClass(pdf: any): string {
    if (pdf.rejected) {
      return 'status-rejected';
    }
    
    const stage = pdf.approvalStage || 'IT';
    switch (stage) {
      case 'IT':
        return 'status-pending';
      case 'FINANCE':
        return 'status-approved';
      case 'APPROVED':
        return 'status-final-approved';
      default:
        return 'status-pending';
    }
  }

  getItStatusIcon(pdf: any): string {
    if (pdf.rejected) {
      return '❌';
    }

    const stage = pdf.approvalStage || 'IT';
    switch (stage) {
      case 'IT':
        return '⏳';
      case 'FINANCE':
        return '✅';
      case 'APPROVED':
        return '🎉';
      default:
        return '⏳';
    }
  }

  getItStatusText(pdf: any): string {
    if (pdf.rejected) {
      return 'Rejected';
    }

    const stage = pdf.approvalStage || 'IT';
    switch (stage) {
      case 'IT':
        return 'Pending IT Approval';
      case 'FINANCE':
        return 'Approved by IT - With Finance Manager';
      case 'APPROVED':
        return 'Fully Approved';
      default:
        return 'Pending IT Approval';
    }
  }

  deletePdf(pdf: VendorPdf) {
    console.log('DEBUG: Deleting PDF:', pdf.id, pdf.originalFileName);
    if (confirm(`Are you sure you want to delete "${pdf.originalFileName}"? This action cannot be undone.`)) {
      console.log('DEBUG: User confirmed deletion');
      this.apiService.deletePdf(pdf.id).subscribe({
        next: (response) => {
          console.log('DEBUG: Delete PDF response:', response);
          if (response.success) {
            alert('PDF deleted successfully!');
            // Reload PDFs based on user role
            if (this.authService.isEmployee()) {
              this.loadEmployeePdfs();
            } else {
              this.loadPdfs();
            }
          } else {
            alert('Failed to delete PDF: ' + response.message);
          }
        },
        error: (error) => {
          console.error('Error deleting PDF:', error);
          alert('Failed to delete PDF. Please try again.');
        }
      });
    }
  }

  canAccessFinanceApprovals(): boolean {
    return this.authService.canAccessFinanceApprovals();
  }

  toggleUserSlot(userName: string) {
    if (this.expandedUserSlots.has(userName)) {
      this.expandedUserSlots.delete(userName);
    } else {
      this.expandedUserSlots.add(userName);
    }
  }

  isUserSlotExpanded(userName: string): boolean {
    return this.expandedUserSlots.has(userName);
  }

  // Methods for expanding/collapsing requisition slots
  toggleRequisitionSlot(requisitionId: number) {
    if (this.expandedRequisitionSlots.has(requisitionId)) {
      this.expandedRequisitionSlots.delete(requisitionId);
    } else {
      this.expandedRequisitionSlots.add(requisitionId);
    }
  }

  isRequisitionSlotExpanded(requisitionId: number): boolean {
    return this.expandedRequisitionSlots.has(requisitionId);
  }

  getPendingPdfsCount(): number {
    return this.uploadedPdfs().filter(pdf => !pdf.processed && !pdf.rejected).length;
  }


  getPdfStatusText(pdf: VendorPdf): string {
    if (pdf.processed) return 'Approved';
    if (pdf.rejected) return 'Rejected';
    return 'Pending';
  }

  // Helper method to get item names for both single and multi-item requisitions
  getItemNames(requisition: Requisition): string {
    if (requisition.items && requisition.items.length > 0) {
      return requisition.items.map(item => item.itemName).join(', ');
    }
    return requisition.itemName || 'N/A';
  }

  // Helper method to get total quantity for multi-item requisitions
  getTotalQuantity(requisition: Requisition): number {
    if (requisition.items && requisition.items.length > 0) {
      return requisition.items.reduce((total, item) => total + item.quantity, 0);
    }
    return requisition.quantity || 0;
  }

  // Helper method to get total amount for multi-item requisitions
  getTotalAmount(requisition: Requisition): number {
    if (requisition.items && requisition.items.length > 0) {
      return requisition.items.reduce((total, item) => total + (item.price * item.quantity), 0);
    }
    return (requisition.price || 0) * (requisition.quantity || 0);
  }

  // Modal methods
  viewRequisitionDetails(requisition: Requisition) {
    this.selectedRequisition.set(requisition);
    this.showRequisitionModal.set(true);
  }

  closeRequisitionModal() {
    this.showRequisitionModal.set(false);
    this.selectedRequisition.set(null);
  }

  // Helper method to get status color class
  getStatusColorClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'pending':
      case 'submitted':
        return 'status-pending';
      case 'pending_it_approval':
      case 'pending_finance_approval':
        return 'status-review';
      case 'approved':
        return 'status-approved';
      case 'rejected':
        return 'status-rejected';
      default:
        return 'status-default';
    }
  }

  // Helper method to get status display text
  getStatusDisplayText(status: string): string {
    switch (status.toLowerCase()) {
      case 'pending':
        return 'Pending';
      case 'submitted':
        return 'Submitted';
      case 'pending_it_approval':
        return 'Pending IT Approval';
      case 'pending_finance_approval':
        return 'Pending Finance Approval';
      case 'approved':
        return 'Approved';
      case 'rejected':
        return 'Rejected';
      default:
        return status;
    }
  }

  // PDF upload modal methods
  openAddPdfModal(requisitionId: number) {
    this.selectedRequisitionIdForPdf.set(requisitionId);
    this.showAddPdfModal.set(true);
    this.selectedFileForUpload.set(null);
    this.pdfDescription.set('');
  }

  closeAddPdfModal() {
    this.showAddPdfModal.set(false);
    this.selectedRequisitionIdForPdf.set(null);
    this.selectedFileForUpload.set(null);
    this.pdfDescription.set('');
    this.isUploading.set(false);
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file && file.type === 'application/pdf') {
      this.selectedFileForUpload.set(file);
    } else {
      alert('Please select a valid PDF file.');
      event.target.value = '';
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  uploadPdfToRequisition() {
    const file = this.selectedFileForUpload();
    const requisitionId = this.selectedRequisitionIdForPdf();
    const description = this.pdfDescription();

    if (!file || !requisitionId) {
      alert('Please select a file and ensure requisition ID is valid.');
      return;
    }

    this.isUploading.set(true);

    this.apiService.uploadPdf(file, description, requisitionId).subscribe({
      next: (response) => {
        this.isUploading.set(false);
        if (response.success) {
          alert('PDF uploaded successfully!');
          this.closeAddPdfModal();
          this.loadPdfs(); // Reload PDFs to show the new upload
        } else {
          alert('Failed to upload PDF: ' + response.message);
        }
      },
      error: (error) => {
        this.isUploading.set(false);
        console.error('Error uploading PDF:', error);
        alert('Failed to upload PDF. Please try again.');
      }
    });
  }

}
