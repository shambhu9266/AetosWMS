import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PurchaseOrder } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-purchase-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './purchase-orders.component.html',
  styleUrls: ['./purchase-orders.component.css']
})
export class PurchaseOrdersComponent implements OnInit {
  constructor(
    private apiService: ApiService, 
    private authService: AuthService,
    private alertService: AlertService
  ) {}

  protected readonly purchaseOrders = signal<PurchaseOrder[]>([]);
  protected readonly activeTab = signal<string>('create');
  
  // PO Creation Form
  poForm = {
    // Bill To Information
    billToCompany: 'M J Warehousing Pvt. Ltd.',
    billToAddress: 'Harichand Melaram Complex Village Mandoli, Delhi - 110093',
    billToPAN: 'AACCM3232B',
    billToGSTIN: '07AACCM3232B2Z3',
    
    // Vendor Information
    vendorName: '',
    vendorAddress: '',
    vendorContactPerson: '',
    vendorMobileNo: '',
    
    // Ship To Information
    shipToAddress: '',
    
    // Order Details
    scopeOfOrder: '',
    shippingMethod: 'By Road',
    shippingTerms: 'N.A.',
    dateOfCompletion: 'Within 7 Days',
    
    // Line Items
    lineItems: [
      { srNo: 1, quantity: '1', unit: 'Nos', description: '', unitPrice: 0, amount: 0 }
    ],
    
    // Financial Details
    freightCharges: 0,
    gstRate: 18,
    
    // Terms & Conditions
    termsAndConditions: '',
    paymentTerms: '',
    warranty: 'NA',
    
    // System Fields
    department: ''
  };
  
  isCreating = false;
  createSuccess = false;
  createError = '';
  
  // Email functionality
  showEmailModal = false;
  selectedPO: any = null;
  vendorEmail = '';
  isSendingEmail = false;
  emailSuccess = false;
  emailError = '';

  // PO Status Options
  statusOptions = [
    { value: 'DRAFT', label: 'Draft' },
    { value: 'SENT_TO_VENDOR', label: 'Sent to Vendor' },
    { value: 'ACKNOWLEDGED', label: 'Acknowledged' },
    { value: 'IN_PRODUCTION', label: 'In Production' },
    { value: 'SHIPPED', label: 'Shipped' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' }
  ];

  ngOnInit() {
    this.loadPurchaseOrders();
  }

  private loadPurchaseOrders() {
    this.apiService.getPurchaseOrders().subscribe({
      next: (response) => {
        if (response.success) {
          this.purchaseOrders.set(response.pos || []);
        }
      },
      error: (error) => {
        console.error('Error loading purchase orders:', error);
      }
    });
  }

  setActiveTab(tab: string) {
    this.activeTab.set(tab);
  }

  createPurchaseOrder() {
    if (!this.validateForm()) {
      return;
    }

    this.isCreating = true;
    this.createError = '';
    this.createSuccess = false;

    // Calculate amounts
    const subtotalAmount = this.getSubtotalAmount();
    const gstAmount = this.getGstAmount();
    const totalAmount = this.getTotalAmount();

    // Prepare PO data with calculated amounts
    const poData = {
      ...this.poForm,
      subtotalAmount: subtotalAmount,
      gstAmount: gstAmount,
      totalAmount: totalAmount
    };

    this.apiService.createPurchaseOrder(poData).subscribe({
      next: (response) => {
        if (response.success) {
          this.createSuccess = true;
          this.createError = '';
          this.resetForm();
          this.loadPurchaseOrders(); // Reload PO list
          
          // Clear success message after 3 seconds
          setTimeout(() => {
            this.createSuccess = false;
          }, 3000);
        } else {
          this.createError = response.message || 'Failed to create Purchase Order.';
        }
        this.isCreating = false;
      },
      error: (error) => {
        console.error('Error creating purchase order:', error);
        this.createError = 'Failed to create Purchase Order. Please try again.';
        this.isCreating = false;
      }
    });
  }

  private validateForm(): boolean {
    if (!this.poForm.billToCompany.trim()) {
      this.createError = 'Bill To Company is required.';
      return false;
    }
    if (!this.poForm.billToPAN.trim()) {
      this.createError = 'Bill To PAN is required.';
      return false;
    }
    if (!this.validatePAN(this.poForm.billToPAN)) {
      this.createError = 'Invalid PAN format. Please enter a valid 10-digit PAN (e.g., ABCDE1234F).';
      return false;
    }
    if (!this.poForm.billToGSTIN.trim()) {
      this.createError = 'Bill To GSTIN is required.';
      return false;
    }
    if (!this.validateGSTIN(this.poForm.billToGSTIN)) {
      this.createError = 'Invalid GSTIN format. Please enter a valid 15-digit GSTIN (e.g., 07ABCDE1234F1Z5).';
      return false;
    }
    if (!this.poForm.billToAddress.trim()) {
      this.createError = 'Bill To Address is required.';
      return false;
    }
    if (!this.poForm.vendorName.trim()) {
      this.createError = 'Vendor name is required.';
      return false;
    }
    if (!this.poForm.vendorAddress.trim()) {
      this.createError = 'Vendor address is required.';
      return false;
    }
    if (!this.poForm.vendorContactPerson.trim()) {
      this.createError = 'Vendor contact person is required.';
      return false;
    }
    if (!this.poForm.vendorMobileNo.trim()) {
      this.createError = 'Vendor mobile number is required.';
      return false;
    }
    if (!this.poForm.shipToAddress.trim()) {
      this.createError = 'Ship To address is required.';
      return false;
    }
    if (!this.poForm.scopeOfOrder.trim()) {
      this.createError = 'Scope of order is required.';
      return false;
    }
    if (!this.poForm.department.trim()) {
      this.createError = 'Department is required.';
      return false;
    }
    if (this.poForm.lineItems.length === 0) {
      this.createError = 'At least one line item is required.';
      return false;
    }
    if (this.poForm.gstRate < 0 || this.poForm.gstRate > 100) {
      this.createError = 'GST rate must be between 0% and 100%.';
      return false;
    }
    return true;
  }

  private validatePAN(pan: string): boolean {
    const panRegex = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/;
    return panRegex.test(pan.toUpperCase());
  }

  private validateGSTIN(gstin: string): boolean {
    const gstinRegex = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[A-Z0-9]{1}[Z]{1}[A-Z0-9]{1}$/;
    return gstinRegex.test(gstin.toUpperCase());
  }

  formatPAN(event: any) {
    const value = event.target.value.toUpperCase();
    event.target.value = value;
    this.poForm.billToPAN = value;
  }

  formatGSTIN(event: any) {
    const value = event.target.value.toUpperCase();
    event.target.value = value;
    this.poForm.billToGSTIN = value;
  }

  onGstRateChange() {
    // GST amount will be automatically recalculated due to getGstAmount() method
    // This method can be used for any additional logic when GST rate changes
  }

  // Email functionality
  openEmailModal(po: any) {
    this.selectedPO = po;
    this.vendorEmail = '';
    this.showEmailModal = true;
    this.emailSuccess = false;
    this.emailError = '';
  }

  closeEmailModal() {
    this.showEmailModal = false;
    this.selectedPO = null;
    this.vendorEmail = '';
    this.emailSuccess = false;
    this.emailError = '';
  }

  sendEmail() {
    if (!this.vendorEmail.trim()) {
      this.emailError = 'Please enter vendor email address';
      return;
    }

    if (!this.selectedPO) {
      this.emailError = 'No purchase order selected';
      return;
    }

    this.isSendingEmail = true;
    this.emailError = '';

    this.apiService.sendPurchaseOrderEmail(this.selectedPO.id, this.vendorEmail).subscribe({
      next: (response) => {
        this.isSendingEmail = false;
        if (response.success) {
          this.emailSuccess = true;
          this.emailError = '';
          // Refresh the PO list to update status
          this.loadPurchaseOrders();
          // Close modal after 2 seconds
          setTimeout(() => {
            this.closeEmailModal();
          }, 2000);
        } else {
          this.emailError = response.message || 'Failed to send email';
        }
      },
      error: (error) => {
        this.isSendingEmail = false;
        this.emailError = 'Error sending email: ' + (error.error?.message || error.message);
        console.error('Email error:', error);
      }
    });
  }

  resetForm() {
    this.poForm = {
      // Bill To Information
      billToCompany: 'M J Warehousing Pvt. Ltd.',
      billToAddress: 'Harichand Melaram Complex Village Mandoli, Delhi - 110093',
      billToPAN: 'AACCM3232B',
      billToGSTIN: '07AACCM3232B2Z3',
      
      // Vendor Information
      vendorName: '',
      vendorAddress: '',
      vendorContactPerson: '',
      vendorMobileNo: '',
      
      // Ship To Information
      shipToAddress: '',
      
      // Order Details
      scopeOfOrder: '',
      shippingMethod: 'By Road',
      shippingTerms: 'N.A.',
      dateOfCompletion: 'Within 7 Days',
      
      // Line Items
      lineItems: [
        { srNo: 1, quantity: '1', unit: 'Nos', description: '', unitPrice: 0, amount: 0 }
      ],
      
      // Financial Details
      freightCharges: 0,
      gstRate: 18,
      
      // Terms & Conditions
      termsAndConditions: '',
      paymentTerms: '',
      warranty: 'NA',
      
      // System Fields
      department: ''
    };
  }

  // Line Item Management
  addLineItem() {
    const newSrNo = this.poForm.lineItems.length + 1;
    this.poForm.lineItems.push({
      srNo: newSrNo,
      quantity: '1',
      unit: 'Nos',
      description: '',
      unitPrice: 0,
      amount: 0
    });
  }

  removeLineItem(index: number) {
    if (this.poForm.lineItems.length > 1) {
      this.poForm.lineItems.splice(index, 1);
      // Update serial numbers
      this.poForm.lineItems.forEach((item, idx) => {
        item.srNo = idx + 1;
      });
    }
  }

  updateLineItemAmount(index: number) {
    const item = this.poForm.lineItems[index];
    const qty = parseFloat(item.quantity) || 0;
    const price = item.unitPrice || 0;
    item.amount = qty * price;
  }

  // Calculations
  getSubtotalAmount(): number {
    return this.poForm.lineItems.reduce((sum, item) => sum + item.amount, 0);
  }

  getGstAmount(): number {
    const subtotal = this.getSubtotalAmount();
    return (subtotal * this.poForm.gstRate) / 100;
  }

  getTotalAmount(): number {
    return this.getSubtotalAmount() + this.poForm.freightCharges + this.getGstAmount();
  }

  updatePOStatus(po: PurchaseOrder, status: string) {
    this.apiService.updatePOStatus(po.id, status).subscribe({
      next: (response) => {
        if (response.success) {
          this.alertService.showError('PO status updated successfully!');
          this.loadPurchaseOrders(); // Reload PO list
        } else {
          this.alertService.showError('Failed to update PO status: ' + response.message);
        }
      },
      error: (error) => {
        console.error('Error updating PO status:', error);
        this.alertService.showError('Failed to update PO status. Please try again.');
      }
    });
  }

  downloadPDF(po: PurchaseOrder) {
    if (!po.id) {
      this.alertService.showError('PO ID not found');
      return;
    }

    this.apiService.downloadPurchaseOrderPdf(po.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `PO_${po.poNumber}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        
        this.alertService.showError('PDF downloaded successfully!');
      },
      error: (error) => {
        console.error('Error downloading PDF:', error);
        this.alertService.showError('Error downloading PDF. Please try again.');
      }
    });
  }

  deletePurchaseOrder(po: PurchaseOrder) {
    if (confirm(`Are you sure you want to delete PO ${po.poNumber}? This action cannot be undone.`)) {
      this.apiService.deletePurchaseOrder(po.id).subscribe({
        next: (response) => {
          if (response.success) {
            this.alertService.showError('Purchase Order deleted successfully!');
            this.loadPurchaseOrders(); // Reload PO list
          } else {
            this.alertService.showError('Failed to delete Purchase Order: ' + response.message);
          }
        },
        error: (error) => {
          console.error('Error deleting purchase order:', error);
          this.alertService.showError('Failed to delete Purchase Order. Please try again.');
        }
      });
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    }).format(amount);
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase().replace('_', '-')}`;
  }

  canAccessFinanceApprovals(): boolean {
    return this.authService.canAccessFinanceApprovals();
  }
}
