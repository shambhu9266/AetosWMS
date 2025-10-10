import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-master',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './master.component.html',
  styleUrls: ['./master.component.css']
})
export class MasterComponent implements OnInit {
  
  masterModules = [
    {
      id: 'users',
      title: 'User Management',
      description: 'Manage system users, roles, and permissions',
      icon: 'M16 7C16 9.20914 14.2091 11 12 11C9.79086 11 8 9.20914 8 7C8 4.79086 9.79086 3 12 3C14.2091 3 16 4.79086 16 7ZM12 14C8.13401 14 5 17.134 5 21H19C19 17.134 15.866 14 12 14Z',
      route: '/user-management',
      color: 'blue',
      priority: 'critical',
      stats: { total: 24, active: 22 }
    },
    {
      id: 'departments',
      title: 'Department Management',
      description: 'Manage organizational structure and departments',
      icon: 'M19 21V19C19 16.7909 17.2091 15 15 15H9C6.79086 15 5 16.7909 5 19V21M16 7C16 9.20914 14.2091 11 12 11C9.79086 11 8 9.20914 8 7C8 4.79086 9.79086 3 12 3C14.2091 3 16 4.79086 16 7ZM12 14C8.13401 14 5 17.134 5 21H19C19 17.134 15.866 14 12 14Z',
      route: '/master/departments',
      color: 'green',
      priority: 'critical',
      stats: { total: 6, active: 6 }
    },
    {
      id: 'vendors',
      title: 'Vendor Management',
      description: 'Manage approved vendors and suppliers',
      icon: 'M12 2L13.09 8.26L19 7L14.74 12.26L21 13.09L15.74 18.26L17 24L12 19L7 24L8.26 18.26L3 13.09L9.26 12.26L5 7L10.91 8.26L12 2Z',
      route: '/master/vendors',
      color: 'red',
      priority: 'critical',
      stats: { total: 12, active: 10 }
    },
    {
      id: 'items',
      title: 'Item Management',
      description: 'Manage procurement items and categories',
      icon: 'M7 4V2C7 1.45 7.45 1 8 1H16C16.55 1 17 1.45 17 2V4H20C20.55 4 21 4.45 21 5S20.55 6 20 6H19V19C19 20.1 18.1 21 17 21H7C5.9 21 5 20.1 5 19V6H4C3.45 6 3 5.55 3 5S3.45 4 4 4H7ZM9 3V4H15V3H9ZM7 6V19H17V6H7Z',
      route: '/master/items',
      color: 'orange',
      priority: 'important',
      stats: { total: 45, active: 42 }
    },
    {
      id: 'approvals',
      title: 'Approval Workflow Management',
      description: 'Configure approval chains and thresholds',
      icon: 'M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z',
      route: '/master/approvals',
      color: 'indigo',
      priority: 'important',
      stats: { total: 5, active: 5 }
    },
    {
      id: 'budget',
      title: 'Budget Management',
      description: 'Manage department budgets and allocations',
      icon: 'M12 2V22M17 5H9.5C8.11929 5 7 6.11929 7 7.5S8.11929 10 9.5 10H14.5C15.8807 10 17 11.1193 17 12.5S15.8807 15 14.5 15H7',
      route: '/budget',
      color: 'purple',
      priority: 'important',
      stats: { total: 6, active: 6 }
    }
  ];

  constructor(private router: Router) {}

  ngOnInit() {
    this.loadMasterData();
  }

  loadMasterData() {
    // Load statistics for each module
    console.log('Loading master data...');
  }

  navigateToModule(module: any) {
    if (module.route) {
      this.router.navigate([module.route]);
    }
  }

  getModuleIcon(icon: string) {
    return icon;
  }

  getModuleColorClass(color: string) {
    return `module-${color}`;
  }

}
