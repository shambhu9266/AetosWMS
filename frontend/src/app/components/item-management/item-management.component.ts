import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-item-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './item-management.component.html',
  styleUrls: ['./item-management.component.css']
})
export class ItemManagementComponent implements OnInit {
  
  items: any[] = [];
  newItem = {
    name: '',
    description: '',
    category: '',
    unit: '',
    price: 0,
    supplier: '',
    stockQuantity: 0,
    minStockLevel: 0
  };
  showAddForm = false;
  editingItem: any = null;
  formData = {
    name: '',
    description: '',
    category: '',
    unit: '',
    price: 0,
    supplier: '',
    stockQuantity: 0,
    minStockLevel: 0
  };

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    this.loadItems();
  }

  loadItems() {
    // Mock data for now - replace with actual API call
    this.items = [
      { id: 1, name: 'Laptop - Dell Inspiron 15', description: '15.6" Laptop with Intel i5 processor', category: 'IT Equipment', unit: 'Piece', price: 899.99, supplier: 'Tech Solutions Inc.', stockQuantity: 25, minStockLevel: 5, active: true },
      { id: 2, name: 'Office Chair - Ergonomic', description: 'Adjustable ergonomic office chair', category: 'Furniture', unit: 'Piece', price: 299.99, supplier: 'Furniture World', stockQuantity: 15, minStockLevel: 3, active: true },
      { id: 3, name: 'A4 Paper - White', description: '80gsm white A4 paper, 500 sheets', category: 'Office Supplies', unit: 'Ream', price: 12.99, supplier: 'Office Supplies Co.', stockQuantity: 50, minStockLevel: 10, active: true },
      { id: 4, name: 'Printer - HP LaserJet', description: 'Black & white laser printer', category: 'IT Equipment', unit: 'Piece', price: 199.99, supplier: 'Tech Solutions Inc.', stockQuantity: 8, minStockLevel: 2, active: true },
      { id: 5, name: 'Desk Lamp - LED', description: 'Adjustable LED desk lamp', category: 'Office Supplies', unit: 'Piece', price: 45.99, supplier: 'Office Supplies Co.', stockQuantity: 30, minStockLevel: 5, active: true },
      { id: 6, name: 'Monitor - 24" LED', description: '24-inch LED monitor, Full HD', category: 'IT Equipment', unit: 'Piece', price: 249.99, supplier: 'Tech Solutions Inc.', stockQuantity: 12, minStockLevel: 3, active: true },
      { id: 7, name: 'Keyboard - Wireless', description: 'Wireless keyboard with USB receiver', category: 'IT Equipment', unit: 'Piece', price: 39.99, supplier: 'Tech Solutions Inc.', stockQuantity: 20, minStockLevel: 5, active: true },
      { id: 8, name: 'Mouse - Optical', description: 'USB optical mouse', category: 'IT Equipment', unit: 'Piece', price: 19.99, supplier: 'Tech Solutions Inc.', stockQuantity: 35, minStockLevel: 8, active: true },
      { id: 9, name: 'Notebook - Spiral Bound', description: 'A4 spiral bound notebook, 200 pages', category: 'Office Supplies', unit: 'Piece', price: 8.99, supplier: 'Office Supplies Co.', stockQuantity: 40, minStockLevel: 10, active: true },
      { id: 10, name: 'Pen Set - Blue Ink', description: 'Set of 12 blue ink pens', category: 'Office Supplies', unit: 'Set', price: 15.99, supplier: 'Office Supplies Co.', stockQuantity: 25, minStockLevel: 5, active: true },
      { id: 11, name: 'Desk - Executive', description: 'Executive wooden desk, 6ft', category: 'Furniture', unit: 'Piece', price: 599.99, supplier: 'Furniture World', stockQuantity: 5, minStockLevel: 1, active: true },
      { id: 12, name: 'Filing Cabinet - 2 Drawer', description: 'Metal filing cabinet, 2 drawers', category: 'Furniture', unit: 'Piece', price: 149.99, supplier: 'Furniture World', stockQuantity: 8, minStockLevel: 2, active: true },
      { id: 13, name: 'Cleaning Supplies Kit', description: 'Complete office cleaning supplies', category: 'Cleaning', unit: 'Kit', price: 89.99, supplier: 'Clean Solutions', stockQuantity: 10, minStockLevel: 2, active: true },
      { id: 14, name: 'Security Camera - IP', description: 'IP security camera with night vision', category: 'Security', unit: 'Piece', price: 199.99, supplier: 'Security Systems Ltd.', stockQuantity: 6, minStockLevel: 1, active: true },
      { id: 15, name: 'Coffee Machine - Commercial', description: 'Commercial coffee machine', category: 'Kitchen', unit: 'Piece', price: 899.99, supplier: 'Catering Express', stockQuantity: 2, minStockLevel: 1, active: true }
    ];
  }

  addItem() {
    if (this.formData.name && this.formData.category && this.formData.supplier) {
      const item = {
        id: this.items.length + 1,
        ...this.formData,
        active: true
      };
      this.items.push(item);
      this.resetForm();
    }
  }

  editItem(item: any) {
    this.editingItem = { ...item };
    this.formData = { ...item };
    this.showAddForm = true;
  }

  updateItem() {
    if (this.editingItem) {
      const index = this.items.findIndex(i => i.id === this.editingItem.id);
      if (index !== -1) {
        this.items[index] = { ...this.editingItem, ...this.formData };
        this.resetForm();
      }
    }
  }

  deleteItem(item: any) {
    if (confirm('Are you sure you want to delete this item?')) {
      this.items = this.items.filter(i => i.id !== item.id);
    }
  }

  toggleItemStatus(item: any) {
    item.active = !item.active;
  }

  resetForm() {
    this.formData = {
      name: '',
      description: '',
      category: '',
      unit: '',
      price: 0,
      supplier: '',
      stockQuantity: 0,
      minStockLevel: 0
    };
    this.showAddForm = false;
    this.editingItem = null;
  }

  getActiveItems() {
    return this.items.filter(item => item.active).length;
  }

  getLowStockItems() {
    return this.items.filter(item => item.stockQuantity <= item.minStockLevel).length;
  }

  getTotalValue() {
    return this.items.reduce((total, item) => total + (item.price * item.stockQuantity), 0);
  }

  getItemsByCategory() {
    const categories = [...new Set(this.items.map(item => item.category))];
    return categories.length;
  }

  getStockStatus(item: any) {
    if (item.stockQuantity <= item.minStockLevel) {
      return 'low';
    } else if (item.stockQuantity <= item.minStockLevel * 2) {
      return 'medium';
    } else {
      return 'good';
    }
  }
}
