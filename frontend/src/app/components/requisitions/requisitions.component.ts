import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Requisition, RequisitionItem, CreateRequisitionRequest, CreateRequisitionWithItemsRequest, LineItem as ApiLineItem } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

export interface RequisitionLineItem {
  itemName: string;
  quantity: number;
  price: number;
}

@Component({
  selector: 'app-requisitions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './requisitions.component.html',
  styleUrls: ['./requisitions.component.css']
})
export class RequisitionsComponent implements OnInit {
  constructor(private apiService: ApiService, public authService: AuthService) {}
  protected readonly showCreatePR = signal<boolean>(false);
  protected readonly createdBy = signal<string>('');
  protected readonly department = signal<string>('');

  // Line items array - using regular property instead of signal for better form handling
  protected lineItems: RequisitionLineItem[] = [
    { itemName: '', quantity: 1, price: 0 }
  ];

  // PDF upload properties
  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly pdfDescription = signal<string>('');
  protected readonly isUploading = signal<boolean>(false);
  private lastCreatedRequisitionId: number = 0;

  protected readonly requisitions = signal<Requisition[]>([]);

  // Modal state
  protected readonly showViewModal = signal<boolean>(false);
  protected readonly showEditModal = signal<boolean>(false);
  protected readonly selectedRequisition = signal<Requisition | null>(null);
  
  // Edit form data
  protected editFormData: {
    createdBy: string;
    department: string;
    items: RequisitionLineItem[];
  } = {
    createdBy: '',
    department: '',
    items: []
  };

  ngOnInit() {
    // Initialize form with current user data
    this.resetForm();
    
    // Load requisitions for all users, but filter differently based on role
    this.loadRequisitions();
  }

  private loadRequisitions() {
    // Load all requisitions
    this.apiService.getRequisitions().subscribe({
      next: (response) => {
        if (response.success) {
          let allRequisitions = response.requisitions || [];
          
          // Filter requisitions based on user role
          if (this.authService.isEmployee()) {
            // For employees, only show their own requisitions
            const currentUser = this.authService.getCurrentUser();
            allRequisitions = allRequisitions.filter((req: Requisition) => 
              req.createdBy === currentUser?.username
            );
          } else if (this.authService.canAccessFinanceApprovals()) {
            // For finance managers, show requisitions that are pending finance approval or have been approved by finance
            console.log('DEBUG: Finance manager - all requisitions before filtering:', allRequisitions.map((req: Requisition) => ({ id: req.id, status: req.status })));
            allRequisitions = allRequisitions.filter((req: Requisition) => 
              req.status === 'PENDING_FINANCE_APPROVAL' || req.status === 'APPROVED'
            );
            console.log('DEBUG: Finance manager - filtered requisitions:', allRequisitions.map((req: Requisition) => ({ id: req.id, status: req.status })));
          }
          // For other managers/admins, show all requisitions (no filtering)
          
          this.requisitions.set(allRequisitions);
          console.log('DEBUG: Loaded requisitions:', allRequisitions);
        } else {
          console.error('Failed to load requisitions:', response.message);
        }
      },
      error: (error) => {
        console.error('Error loading requisitions:', error);
      }
    });
  }

  openCreatePR() {
    this.showCreatePR.set(true);
  }

  closeCreatePR() {
    this.showCreatePR.set(false);
    this.resetForm();
  }

  createPR() {
    const items = this.lineItems;
    const createdBy = this.createdBy();
    const department = this.department();
    
    // Validate that all items have required fields
    const invalidItems = items.filter(item => !item.itemName.trim() || item.quantity <= 0 || item.price < 0);
    if (invalidItems.length > 0) {
      alert('Please fill in all required fields for all items (Item Name, Quantity > 0, Price >= 0)');
      return;
    }

    this.isUploading.set(true);
    
    // Convert LineItem to ApiLineItem
    const apiItems: ApiLineItem[] = items.map(item => ({
      itemName: item.itemName,
      quantity: item.quantity,
      price: item.price
    }));

    const request: CreateRequisitionWithItemsRequest = {
      createdBy: createdBy,
      department: department,
      items: apiItems
    };

    this.apiService.createRequisitionWithItems(request).subscribe({
      next: (response) => {
        if (response.success) {
          console.log('PR with multiple items created successfully:', response);
          this.lastCreatedRequisitionId = response.requisitionId || response.requisition.id;
          
          // If there are selected files, upload them
          if (this.selectedFiles().length > 0) {
            this.uploadPdfsForRequisition(this.lastCreatedRequisitionId);
          } else {
            this.isUploading.set(false);
            this.closeCreatePR();
            this.loadRequisitions();
            this.resetForm();
            // Show success message for employees
            if (this.authService.isEmployee()) {
              alert('✅ Purchase Requisition created successfully! Your request has been submitted for approval.');
            }
          }
        } else {
          this.isUploading.set(false);
          alert(response.message || 'Failed to create PR');
        }
      },
      error: (error) => {
        this.isUploading.set(false);
        console.error('Error creating PR with multiple items:', error);
        alert('Failed to create PR. Please try again.');
      }
    });
  }


  private uploadPdfsForRequisition(requisitionId: number) {
    if (this.selectedFiles().length === 0) return;

    this.isUploading.set(true);
    
    // Upload files sequentially
    this.uploadFilesSequentially(requisitionId, 0);
  }

  private uploadFilesSequentially(requisitionId: number, fileIndex: number) {
    if (fileIndex >= this.selectedFiles().length) {
      // All files uploaded successfully
      this.isUploading.set(false);
      this.closeCreatePR();
      this.loadRequisitions();
      this.resetForm();
      // Show success message for employees
      if (this.authService.isEmployee()) {
        alert('✅ Purchase Requisition created successfully with documents! Your request has been submitted for approval.');
      }
      return;
    }

    const file = this.selectedFiles()[fileIndex];
    
    this.apiService.uploadPdf(
      file, 
      this.pdfDescription() || undefined, 
      requisitionId
    ).subscribe({
      next: (response) => {
        if (response.success) {
          console.log(`PDF ${fileIndex + 1} uploaded successfully:`, response);
          // Upload next file
          this.uploadFilesSequentially(requisitionId, fileIndex + 1);
        } else {
          this.isUploading.set(false);
          alert(`PR created but PDF ${fileIndex + 1} upload failed: ` + (response.message || 'Unknown error'));
          this.closeCreatePR();
          this.loadRequisitions();
          this.resetForm();
        }
      },
      error: (error) => {
        this.isUploading.set(false);
        console.error(`Error uploading PDF ${fileIndex + 1}:`, error);
        alert(`PR created but PDF ${fileIndex + 1} upload failed. Please try uploading again later.`);
        this.closeCreatePR();
        this.loadRequisitions();
        this.resetForm();
      }
    });
  }

  private resetForm() {
    // Set current user information for employees
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      this.createdBy.set(currentUser.username);
      this.department.set(currentUser.department || 'IT');
    } else {
      this.createdBy.set('user1');
      this.department.set('IT');
    }
    
    this.lineItems = [{ itemName: '', quantity: 1, price: 0 }];
    this.selectedFiles.set([]);
    this.pdfDescription.set('');
    this.isUploading.set(false);
    this.lastCreatedRequisitionId = 0;
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  // Input handlers
  onCreatedByInput(event: Event) {
    const v = (event.target as HTMLInputElement).value;
    this.createdBy.set(v);
  }
  
  onDepartmentInput(event: Event) {
    const v = (event.target as HTMLInputElement).value;
    this.department.set(v);
  }

  // Line items management
  addItem() {
    this.lineItems.push({ itemName: '', quantity: 1, price: 0 });
  }

  removeItem(index: number) {
    if (this.lineItems.length > 1) {
      this.lineItems.splice(index, 1);
    }
  }

  updateItemField(index: number, field: keyof RequisitionLineItem, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    
    if (field === 'quantity' || field === 'price') {
      const numValue = Number(value);
      this.lineItems[index][field] = field === 'quantity' 
        ? (Number.isFinite(numValue) && numValue > 0 ? Math.floor(numValue) : 1)
        : (Number.isFinite(numValue) && numValue >= 0 ? numValue : 0);
    } else {
      this.lineItems[index][field] = value;
    }
  }

  updateItem(index: number, field: keyof RequisitionLineItem, value: any) {
    if (field === 'quantity' || field === 'price') {
      const numValue = Number(value);
      this.lineItems[index][field] = field === 'quantity' 
        ? (Number.isFinite(numValue) && numValue > 0 ? Math.floor(numValue) : 1)
        : (Number.isFinite(numValue) && numValue >= 0 ? numValue : 0);
    } else {
      this.lineItems[index][field] = value;
    }
  }

  getTotalAmount(): number {
    return this.lineItems.reduce((total, item) => total + (item.quantity * item.price), 0);
  }

  trackByIndex(index: number, item: RequisitionLineItem): number {
    return index;
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

  getItemNames(requisition: Requisition): string {
    if (requisition.items && requisition.items.length > 0) {
      return requisition.items.map(item => item.itemName).join(', ');
    }
    return requisition.itemName || 'N/A';
  }

  // Action methods
  viewRequisition(requisition: Requisition) {
    console.log('Viewing requisition:', requisition);
    this.selectedRequisition.set(requisition);
    this.showViewModal.set(true);
  }

  editRequisition(requisition: Requisition) {
    console.log('Editing requisition:', requisition);
    this.selectedRequisition.set(requisition);
    
    // Initialize edit form data
    this.editFormData = {
      createdBy: requisition.createdBy,
      department: requisition.department,
      items: requisition.items && requisition.items.length > 0 
        ? requisition.items.map(item => ({
            itemName: item.itemName,
            quantity: item.quantity,
            price: item.price
          }))
        : [{
            itemName: requisition.itemName || '',
            quantity: requisition.quantity || 1,
            price: requisition.price || 0
          }]
    };
    
    this.showEditModal.set(true);
  }

  // Modal control methods
  closeViewModal() {
    this.showViewModal.set(false);
    this.selectedRequisition.set(null);
  }

  closeEditModal() {
    this.showEditModal.set(false);
    this.selectedRequisition.set(null);
    this.editFormData = {
      createdBy: '',
      department: '',
      items: []
    };
  }

  // Edit form methods
  addEditItem() {
    this.editFormData.items.push({
      itemName: '',
      quantity: 1,
      price: 0
    });
  }

  removeEditItem(index: number) {
    if (this.editFormData.items.length > 1) {
      this.editFormData.items.splice(index, 1);
    }
  }

  getEditTotalAmount(): number {
    return this.editFormData.items.reduce((total, item) => total + (item.quantity * item.price), 0);
  }

  saveRequisition() {
    console.log('Saving requisition changes:', this.editFormData);
    
    // Validate form data
    if (!this.isEditFormValid()) {
      alert('Please fill in all required fields.');
      return;
    }
    
    const selectedRequisition = this.selectedRequisition();
    if (!selectedRequisition) {
      alert('No requisition selected for editing.');
      return;
    }
    
    // Convert edit form data to API request format
    const apiItems: ApiLineItem[] = this.editFormData.items.map(item => ({
      itemName: item.itemName,
      quantity: item.quantity,
      price: item.price
    }));

    const request: CreateRequisitionWithItemsRequest = {
      createdBy: this.editFormData.createdBy,
      department: this.editFormData.department,
      items: apiItems
    };

    console.log('Calling update API with request:', request);
    
    this.apiService.updateRequisition(selectedRequisition.id, request).subscribe({
      next: (response) => {
        console.log('Update API response:', response);
        if (response.success) {
          alert('Requisition updated successfully!');
          this.closeEditModal();
          this.loadRequisitions(); // Reload the list to show updated data
        } else {
          alert(response.message || 'Failed to update requisition');
        }
      },
      error: (error) => {
        console.error('Error updating requisition:', error);
        alert('Failed to update requisition. Please try again.');
      }
    });
  }

  isEditFormValid(): boolean {
    if (!this.editFormData.department || this.editFormData.items.length === 0) {
      return false;
    }
    
    return this.editFormData.items.every(item => 
      item.itemName && item.itemName.trim() !== '' && 
      item.quantity > 0 && 
      item.price >= 0
    );
  }

  canDeleteRequisition(requisition: Requisition): boolean {
    // Check if user can delete this requisition
    if (this.authService.isEmployee()) {
      // Employees can only delete their own requisitions
      const currentUser = this.authService.getCurrentUser();
      if (requisition.createdBy !== currentUser?.username) {
        return false;
      }
      
      // Employees cannot delete approved or rejected requisitions
      if (requisition.status === 'APPROVED' || requisition.status === 'REJECTED') {
        return false;
      }
    }
    
    // Managers/admins can delete any requisition
    return true;
  }

  canEditRequisition(requisition: Requisition): boolean {
    // Check if user can edit this requisition
    if (this.authService.isEmployee()) {
      // Employees can only edit their own requisitions
      const currentUser = this.authService.getCurrentUser();
      if (requisition.createdBy !== currentUser?.username) {
        return false;
      }
      
      // Employees cannot edit approved or rejected requisitions
      if (requisition.status === 'APPROVED' || requisition.status === 'REJECTED') {
        return false;
      }
    }
    
    // Managers/admins can edit any requisition
    return true;
  }

  deleteRequisition(requisition: Requisition) {
    console.log('=== DELETE BUTTON CLICKED ===');
    console.log('Attempting to delete requisition:', requisition);
    console.log('Requisition ID:', requisition.id);
    console.log('Requisition status:', requisition.status);
    
    const confirmMessage = `Are you sure you want to delete requisition #${requisition.id}?\n\nThis action cannot be undone.`;
    
    if (confirm(confirmMessage)) {
      console.log('User confirmed deletion, calling API...');
      
      try {
        this.apiService.deleteRequisition(requisition.id).subscribe({
          next: (response) => {
            console.log('Delete API response:', response);
            if (response && response.success) {
              console.log('Requisition deleted successfully:', response);
              alert('Requisition deleted successfully');
              this.loadRequisitions(); // Reload the list
            } else {
              console.error('Delete failed:', response);
              alert(response?.message || 'Failed to delete requisition');
            }
          },
          error: (error) => {
            console.error('Error deleting requisition:', error);
            console.error('Error details:', error);
            alert('Failed to delete requisition. Please check console for details.');
          }
        });
      } catch (error) {
        console.error('Exception in deleteRequisition:', error);
        alert('Failed to delete requisition. Please try again.');
      }
    } else {
      console.log('User cancelled deletion');
    }
  }

  // Helper method for template
  Number = Number;

  // PDF upload methods
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const files = Array.from(input.files);
      const validFiles: File[] = [];
      
      for (const file of files) {
        // Validate file type
        if (file.type !== 'application/pdf') {
          alert(`File "${file.name}" is not a PDF. Please select PDF files only.`);
          continue;
        }
        
        // Validate file size (max 10MB)
        const maxSize = 10 * 1024 * 1024; // 10MB
        if (file.size > maxSize) {
          alert(`File "${file.name}" is too large. Maximum size is 10MB.`);
          continue;
        }
        
        validFiles.push(file);
      }
      
      if (validFiles.length > 0) {
        this.selectedFiles.set(validFiles);
      } else {
        input.value = '';
      }
    }
  }

  onPdfDescriptionInput(event: Event) {
    const v = (event.target as HTMLInputElement).value;
    this.pdfDescription.set(v);
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  removeSelectedFile() {
    this.selectedFiles.set([]);
    this.pdfDescription.set('');
    // Reset file input
    const fileInput = document.getElementById('pdfFile') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }

  removeFile(index: number) {
    const files = this.selectedFiles();
    files.splice(index, 1);
    this.selectedFiles.set([...files]);
  }
}
