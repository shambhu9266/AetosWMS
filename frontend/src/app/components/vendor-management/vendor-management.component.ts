import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-vendor-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vendor-management.component.html',
  styleUrls: ['./vendor-management.component.css']
})
export class VendorManagementComponent implements OnInit {
  
  vendors: any[] = [];
  newVendor = {
    name: '',
    contactPerson: '',
    email: '',
    phone: '',
    address: '',
    category: '',
    rating: 0
  };
  showAddForm = false;
  editingVendor: any = null;
  formData = {
    name: '',
    contactPerson: '',
    email: '',
    phone: '',
    address: '',
    category: '',
    rating: 0
  };

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    this.loadVendors();
  }

  loadVendors() {
    // Mock data for now - replace with actual API call
    this.vendors = [
      { id: 1, name: 'Tech Solutions Inc.', contactPerson: 'John Smith', email: 'john@techsolutions.com', phone: '+1-555-0123', address: '123 Tech Street, Silicon Valley', category: 'IT Equipment', rating: 4.5, active: true },
      { id: 2, name: 'Office Supplies Co.', contactPerson: 'Sarah Johnson', email: 'sarah@officesupplies.com', phone: '+1-555-0456', address: '456 Office Ave, Business District', category: 'Office Supplies', rating: 4.2, active: true },
      { id: 3, name: 'Furniture World', contactPerson: 'Mike Davis', email: 'mike@furnitureworld.com', phone: '+1-555-0789', address: '789 Furniture Blvd, Design Center', category: 'Furniture', rating: 4.0, active: true },
      { id: 4, name: 'Clean Solutions', contactPerson: 'Lisa Brown', email: 'lisa@cleansolutions.com', phone: '+1-555-0321', address: '321 Clean Street, Service Area', category: 'Cleaning Services', rating: 4.8, active: true },
      { id: 5, name: 'Security Systems Ltd.', contactPerson: 'David Wilson', email: 'david@securitysystems.com', phone: '+1-555-0654', address: '654 Security Way, Safety Zone', category: 'Security', rating: 4.3, active: true },
      { id: 6, name: 'Catering Express', contactPerson: 'Emma Taylor', email: 'emma@cateringexpress.com', phone: '+1-555-0987', address: '987 Food Court, Culinary District', category: 'Catering', rating: 4.6, active: true },
      { id: 7, name: 'Transportation Pro', contactPerson: 'Robert Lee', email: 'robert@transportationpro.com', phone: '+1-555-0147', address: '147 Transport Lane, Logistics Hub', category: 'Transportation', rating: 4.1, active: true },
      { id: 8, name: 'Maintenance Masters', contactPerson: 'Jennifer Garcia', email: 'jennifer@maintenancemasters.com', phone: '+1-555-0258', address: '258 Repair Road, Service Center', category: 'Maintenance', rating: 4.4, active: true },
      { id: 9, name: 'Marketing Media', contactPerson: 'Alex Chen', email: 'alex@marketingmedia.com', phone: '+1-555-0369', address: '369 Media Street, Creative District', category: 'Marketing', rating: 4.7, active: true },
      { id: 10, name: 'Legal Services', contactPerson: 'Maria Rodriguez', email: 'maria@legalservices.com', phone: '+1-555-0741', address: '741 Legal Lane, Justice Plaza', category: 'Legal', rating: 4.9, active: true },
      { id: 11, name: 'Accounting Plus', contactPerson: 'James Anderson', email: 'james@accountingplus.com', phone: '+1-555-0852', address: '852 Finance Ave, Business Center', category: 'Accounting', rating: 4.5, active: true },
      { id: 12, name: 'Consulting Experts', contactPerson: 'Susan White', email: 'susan@consultingexperts.com', phone: '+1-555-0963', address: '963 Consult Blvd, Professional Plaza', category: 'Consulting', rating: 4.3, active: true }
    ];
  }

  addVendor() {
    if (this.formData.name && this.formData.contactPerson && this.formData.email) {
      const vendor = {
        id: this.vendors.length + 1,
        ...this.formData,
        active: true
      };
      this.vendors.push(vendor);
      this.resetForm();
    }
  }

  editVendor(vendor: any) {
    this.editingVendor = { ...vendor };
    this.formData = { ...vendor };
    this.showAddForm = true;
  }

  updateVendor() {
    if (this.editingVendor) {
      const index = this.vendors.findIndex(v => v.id === this.editingVendor.id);
      if (index !== -1) {
        this.vendors[index] = { ...this.editingVendor, ...this.formData };
        this.resetForm();
      }
    }
  }

  deleteVendor(vendor: any) {
    if (confirm('Are you sure you want to delete this vendor?')) {
      this.vendors = this.vendors.filter(v => v.id !== vendor.id);
    }
  }

  toggleVendorStatus(vendor: any) {
    vendor.active = !vendor.active;
  }

  resetForm() {
    this.formData = {
      name: '',
      contactPerson: '',
      email: '',
      phone: '',
      address: '',
      category: '',
      rating: 0
    };
    this.showAddForm = false;
    this.editingVendor = null;
  }

  getActiveVendors() {
    return this.vendors.filter(vendor => vendor.active).length;
  }

  getAverageRating() {
    const activeVendors = this.vendors.filter(v => v.active);
    if (activeVendors.length === 0) return 0;
    const totalRating = activeVendors.reduce((sum, vendor) => sum + vendor.rating, 0);
    return (totalRating / activeVendors.length).toFixed(1);
  }

  getVendorsByCategory() {
    const categories = [...new Set(this.vendors.map(v => v.category))];
    return categories.length;
  }
}
