import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { BudgetComponent } from './components/budget/budget.component';
import { RequisitionsComponent } from './components/requisitions/requisitions.component';
import { ApprovalsComponent } from './components/approvals/approvals.component';
import { PurchaseOrdersComponent } from './components/purchase-orders/purchase-orders.component';
import { GrnComponent } from './components/grn/grn.component';
import { NotificationsComponent } from './components/notifications/notifications.component';
import { PdfUploadComponent } from './components/pdf-upload/pdf-upload.component';
import { AuthGuard } from './guards/auth.guard';
import { BudgetGuard } from './guards/budget.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'budget', component: BudgetComponent, canActivate: [BudgetGuard] },
  { path: 'requisitions', component: RequisitionsComponent, canActivate: [AuthGuard] },
  { path: 'approvals', component: ApprovalsComponent, canActivate: [AuthGuard] },
  { path: 'pdf-upload', component: PdfUploadComponent, canActivate: [AuthGuard] },
  { path: 'purchase-orders', component: PurchaseOrdersComponent, canActivate: [AuthGuard] },
  { path: 'grn', component: GrnComponent, canActivate: [AuthGuard] },
  { path: 'notifications', component: NotificationsComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '/login' }
];
