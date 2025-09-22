import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ChartConfiguration, ChartData, ChartType, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

// Register Chart.js components
import Chart from 'chart.js/auto';
Chart.register(...registerables);

interface Budget {
  id: number;
  department: string;
  totalBudget: number;
  remainingBudget: number;
}

// Removed BudgetSummary interface as we don't need it anymore

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './budget.component.html',
  styleUrls: ['./budget.component.css']
})
export class BudgetComponent implements OnInit {
  budgets: Budget[] = [];
  loading = false;
  error: string | null = null;

  // Chart configurations
  public chartType: ChartType = 'doughnut';
  
  // Doughnut Chart - Remaining Budget
  public doughnutChartData: ChartData<'doughnut'> = {
    labels: [],
    datasets: [{
      data: [],
      backgroundColor: [
        '#3B82F6', // Blue for IT
        '#10B981', // Green for Sales  
        '#F59E0B'  // Orange for Management
      ],
      borderColor: [
        '#1E40AF',
        '#059669', 
        '#D97706'
      ],
      borderWidth: 2,
      hoverBackgroundColor: [
        '#2563EB',
        '#047857',
        '#B45309'
      ]
    }]
  };

  // Bar Chart - Remaining Budget by Department
  public barChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        label: 'Remaining Budget',
        data: [],
        backgroundColor: [
          'rgba(59, 130, 246, 0.8)',   // IT - Blue
          'rgba(16, 185, 129, 0.8)',   // Sales - Green
          'rgba(245, 158, 11, 0.8)'    // Management - Orange
        ],
        borderColor: [
          '#3B82F6',
          '#10B981',
          '#F59E0B'
        ],
        borderWidth: 2
      }
    ]
  };

  // Pie Chart - Remaining Budget Distribution
  public pieChartData: ChartData<'pie'> = {
    labels: [],
    datasets: [{
      data: [],
      backgroundColor: [
        'rgba(59, 130, 246, 0.8)',   // IT - Blue
        'rgba(16, 185, 129, 0.8)',   // Sales - Green
        'rgba(245, 158, 11, 0.8)'    // Management - Orange
      ],
      borderColor: [
        '#3B82F6',
        '#10B981',
        '#F59E0B'
      ],
      borderWidth: 2
    }]
  };

  // Line Chart - Remaining Budget Trend (Simulated)
  public lineChartData: ChartData<'line'> = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    datasets: [
      {
        label: 'IT Department',
        data: [120000, 115000, 110000, 105000, 100000, 120000],
        borderColor: '#3B82F6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        tension: 0.4,
        fill: true
      },
      {
        label: 'Sales Department',
        data: [85000, 80000, 75000, 70000, 65000, 85000],
        borderColor: '#10B981',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        tension: 0.4,
        fill: true
      },
      {
        label: 'Management Department',
        data: [45000, 42000, 40000, 38000, 36000, 45000],
        borderColor: '#F59E0B',
        backgroundColor: 'rgba(245, 158, 11, 0.1)',
        tension: 0.4,
        fill: true
      }
    ]
  };


  // Chart Options
  public doughnutChartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '60%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          padding: 20,
          usePointStyle: true,
          font: {
            size: 14,
            weight: 'bold'
          }
        }
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        callbacks: {
          label: (context: any) => {
            const label = context.label || '';
            const value = context.parsed;
            const total = context.dataset.data.reduce((a: any, b: any) => (a || 0) + (b || 0), 0);
            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
            return `${label}: ${this.formatCurrency(value)} (${percentage}%)`;
          }
        }
      }
    }
  };

  public barChartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        labels: {
          padding: 20,
          usePointStyle: true,
          font: {
            size: 14,
            weight: 'bold'
          }
        }
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        callbacks: {
          label: (context: any) => {
            const label = context.dataset.label || '';
            const value = context.parsed.y;
            return `${label}: ${this.formatCurrency(value)}`;
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (value: any) => this.formatCurrency(value)
        }
      }
    }
  };

  public pieChartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          padding: 20,
          usePointStyle: true,
          font: {
            size: 14,
            weight: 'bold'
          }
        }
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        callbacks: {
          label: (context: any) => {
            const label = context.label || '';
            const value = context.parsed;
            const total = context.dataset.data.reduce((a: any, b: any) => (a || 0) + (b || 0), 0);
            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
            return `${label}: ${this.formatCurrency(value)} (${percentage}%)`;
          }
        }
      }
    }
  };

  public lineChartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        labels: {
          padding: 20,
          usePointStyle: true,
          font: {
            size: 14,
            weight: 'bold'
          }
        }
      },
      tooltip: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'white',
        bodyColor: 'white',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        borderWidth: 1,
        callbacks: {
          label: (context: any) => {
            const label = context.dataset.label || '';
            const value = context.parsed.y;
            return `${label}: ${this.formatCurrency(value)}`;
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (value: any) => this.formatCurrency(value)
        }
      }
    }
  };


  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    // Check if user has access to budget
    if (!this.authService.canAccessBudget()) {
      this.router.navigate(['/dashboard']);
      return;
    }
    
    // Initialize chart with fallback data
    this.updateChartData();
    
    this.loadBudgets();
  }

  loadBudgets() {
    this.loading = true;
    this.error = null;
    
    console.log('DEBUG: Loading budgets...');
    this.apiService.getBudgets().subscribe({
      next: (response) => {
        console.log('DEBUG: Budget response:', response);
        if (response.success) {
          this.budgets = response.budgets || [];
          console.log('DEBUG: Budgets loaded:', this.budgets);
          this.calculateSummary();
        } else {
          this.error = response.message || 'Failed to load budgets';
          console.log('DEBUG: Budget loading failed:', this.error);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('DEBUG: Budget error:', error);
        this.error = 'Failed to load budgets';
        this.loading = false;
      }
    });
  }

  calculateSummary() {
    // Update chart data
    this.updateChartData();
  }

  updateChartData() {
    console.log('DEBUG: Updating chart data with budgets:', this.budgets);
    
    if (this.budgets.length > 0) {
      const labels = this.budgets.map(budget => budget.department);
      const totalBudgets = this.budgets.map(budget => budget.totalBudget);
      const remainingBudgets = this.budgets.map(budget => budget.remainingBudget);
      const spentBudgets = this.budgets.map(budget => budget.totalBudget - budget.remainingBudget);
      
      // Doughnut Chart - Remaining Budget
      this.doughnutChartData.labels = labels;
      this.doughnutChartData.datasets[0].data = remainingBudgets;
      
      // Bar Chart - Remaining Budget by Department
      this.barChartData.labels = labels;
      this.barChartData.datasets[0].data = remainingBudgets;
      
      // Pie Chart - Remaining Budget Distribution by Department
      this.pieChartData.labels = labels;
      this.pieChartData.datasets[0].data = remainingBudgets;
    } else {
      // Fallback data for testing
      const labels = ['IT', 'Sales', 'Management'];
      const totalBudgets = [150000, 200000, 100000];
      const remainingBudgets = [120000, 85000, 45000];
      const spentBudgets = [30000, 115000, 55000];
      
      // Doughnut Chart
      this.doughnutChartData.labels = labels;
      this.doughnutChartData.datasets[0].data = remainingBudgets;
      
      // Bar Chart - Remaining Budget by Department
      this.barChartData.labels = labels;
      this.barChartData.datasets[0].data = remainingBudgets;
      
      // Pie Chart - Remaining Budget Distribution by Department
      this.pieChartData.labels = labels;
      this.pieChartData.datasets[0].data = remainingBudgets;
    }
    
    console.log('DEBUG: All chart data updated');
  }

  // Removed unnecessary methods as we only need the chart

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  }

  getCurrentTime(): string {
    return new Date().toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  refreshBudgets() {
    this.loadBudgets();
  }
}
