import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Requisition {
  id: number;
  itemName: string;
  quantity: number;
  price: number;
  createdBy: string;
  department: string;
  status: string;
  createdAt?: string;
  items?: RequisitionItem[];
}

export interface RequisitionItem {
  id: number;
  itemName: string;
  quantity: number;
  price: number;
}

export interface Notification {
  notificationId: number;
  message: string;
  userId: string;
  isRead: boolean;
  timestamp: string;
}

export interface CreateRequisitionRequest {
  createdBy: string;
  itemName: string;
  quantity: number;
  price: number;
  department: string;
}

export interface CreateRequisitionWithItemsRequest {
  createdBy: string;
  department: string;
  items: LineItem[];
}

export interface LineItem {
  itemName: string;
  quantity: number;
  price: number;
}

export interface VendorPdf {
  id: number;
  fileName: string;
  originalFileName: string;
  filePath: string;
  uploadedBy: string;
  description?: string;
  requisitionId?: number;
  uploadedAt: string;
  processed: boolean;
  rejected: boolean;
  rejectionReason?: string;
  approvalStage?: string;
}

export interface UserPdfGroup {
  uploadedBy: string;
  pdfs: VendorPdf[];
  totalPdfs: number;
  processedCount: number;
  pendingCount: number;
  lastUploadDate: string;
}

export interface RequisitionPdfGroup {
  requisitionId: number;
  requisitionInfo: Requisition | null;
  pdfs: VendorPdf[];
  totalPdfs: number;
  processedCount: number;
  pendingCount: number;
  lastUploadDate: string;
  uploadedBy: string;
}

export interface PurchaseOrderLineItem {
  srNo: number;
  quantity: string;
  unit: string;
  description: string;
  unitPrice: number;
  amount: number;
}

export interface PurchaseOrder {
  id: number;
  poNumber: string;
  billToCompany: string;
  billToAddress: string;
  billToPAN: string;
  billToGSTIN: string;
  vendorName: string;
  vendorAddress: string;
  vendorContactPerson: string;
  vendorMobileNo: string;
  shipToAddress: string;
  scopeOfOrder: string;
  shippingMethod: string;
  shippingTerms: string;
  dateOfCompletion: string;
  lineItems: PurchaseOrderLineItem[];
  subtotalAmount: number;
  freightCharges: number;
  gstRate: number;
  gstAmount: number;
  totalAmount: number;
  termsAndConditions?: string;
  paymentTerms?: string;
  warranty?: string;
  createdBy: string;
  department: string;
  status: string;
  poDate: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  // Use relative URL for production, or detect environment
  private baseUrl = this.getBaseUrl();
  
  private getBaseUrl(): string {
    // In production, use direct IP address with port 8080
    // In development, use localhost
    if (typeof window !== 'undefined') {
      const hostname = window.location.hostname;
      
      if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'http://localhost:8080/api';
      } else {
        // Production: use direct IP address
        return 'http://20.57.79.136:8080/api';
      }
    }
    return 'http://localhost:8080/api';
  }

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getAuthParams(): HttpParams {
    const token = this.authService.getToken();
    if (!token) {
      throw new Error('No active token');
    }
    return new HttpParams().set('sessionId', token);
  }

  // Requisition methods
  getRequisitions(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/requisitions`, { params: authParams });
  }

  createRequisition(request: CreateRequisitionRequest): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: Creating requisition with JWT token');

    const params = authParams
      .set('itemName', request.itemName)
      .set('quantity', request.quantity.toString())
      .set('price', request.price.toString())
      .set('department', request.department);
    
    console.log('DEBUG: Making API call to create requisition with params:', params.toString());
    return this.http.post<any>(`${this.baseUrl}/requisitions`, null, { params });
  }

  createRequisitionWithItems(request: CreateRequisitionWithItemsRequest): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: ===== API SERVICE: createRequisitionWithItems called =====');
    console.log('DEBUG: API Service - request:', request);
    
    console.log('DEBUG: API Service - Making HTTP POST call to /requisitions/multiple');
    console.log('DEBUG: API Service - request body:', request);
    return this.http.post<any>(`${this.baseUrl}/requisitions/multiple`, request, { params: authParams });
  }

  getPendingItApprovals(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/requisitions/pending/it`, { params: authParams });
  }

  getPendingFinanceApprovals(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/requisitions/pending/finance`, { params: authParams });
  }

  makeItDecision(id: number, decision: string, comments?: string): Observable<any> {
    const authParams = this.getAuthParams();
    let params = authParams.set('decision', decision);
    
    if (comments) {
      params = params.set('comments', comments);
    }
    
    return this.http.post<any>(`${this.baseUrl}/requisitions/${id}/it-decision`, null, { params });
  }

  makeFinanceDecision(id: number, decision: string, comments?: string): Observable<any> {
    const authParams = this.getAuthParams();
    let params = authParams.set('decision', decision);
    
    if (comments) {
      params = params.set('comments', comments);
    }
    
    return this.http.post<any>(`${this.baseUrl}/requisitions/${id}/finance-decision`, null, { params });
  }

  // Notification methods
  getNotifications(): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: Getting notifications with JWT token');
    console.log('DEBUG: Making API call to:', `${this.baseUrl}/notifications`);
    return this.http.get<any>(`${this.baseUrl}/notifications`, { params: authParams });
  }

  deleteNotification(notificationId: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.delete<any>(`${this.baseUrl}/notifications/${notificationId}`, { params: authParams });
  }

  // Budget methods
  getBudgets(): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: Getting budgets with JWT token');
    console.log('DEBUG: Making API call to:', `${this.baseUrl}/budgets`);
    return this.http.get<any>(`${this.baseUrl}/budgets`, { params: authParams });
  }

  // Dashboard methods
  getApprovedThisMonth(): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: Getting approved this month with JWT token');
    console.log('DEBUG: Making API call to:', `${this.baseUrl}/requisitions/approved-this-month`);
    return this.http.get<any>(`${this.baseUrl}/requisitions/approved-this-month`, { params: authParams });
  }

  // PDF methods
  uploadPdf(file: File, description?: string, requisitionId?: number): Observable<any> {
    const token = this.authService.getToken();
    if (!token) {
      throw new Error('No active token');
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('sessionId', token);
    if (description) {
      formData.append('description', description);
    }
    if (requisitionId) {
      formData.append('requisitionId', requisitionId.toString());
    }

    return this.http.post<any>(`${this.baseUrl}/pdf/upload`, formData);
  }

  getPdfs(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/pdf/list`, { params: authParams });
  }

  downloadPdf(pdfId: number): Observable<Blob> {
    const authParams = this.getAuthParams();
    return this.http.get(`${this.baseUrl}/pdf/download/${pdfId}`, { 
      params: authParams, 
      responseType: 'blob' 
    });
  }

  markPdfAsProcessed(pdfId: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.post<any>(`${this.baseUrl}/pdf/mark-processed/${pdfId}`, null, { params: authParams });
  }

  rejectPdf(pdfId: number, rejectionReason: string): Observable<any> {
    const authParams = this.getAuthParams();
    const params = authParams
      .set('rejectionReason', rejectionReason);

    return this.http.post<any>(`${this.baseUrl}/pdf/reject/${pdfId}`, null, { params });
  }

  deletePdf(pdfId: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.delete<any>(`${this.baseUrl}/pdf/${pdfId}`, { params: authParams });
  }

  // Purchase Order methods
  createPurchaseOrder(poData: any): Observable<any> {
    const authParams = this.getAuthParams();

    const params = authParams
      .set('billToCompany', poData.billToCompany)
      .set('billToAddress', poData.billToAddress)
      .set('billToPAN', poData.billToPAN || '')
      .set('billToGSTIN', poData.billToGSTIN || '')
      .set('vendorName', poData.vendorName)
      .set('vendorAddress', poData.vendorAddress)
      .set('vendorContactPerson', poData.vendorContactPerson)
      .set('vendorMobileNo', poData.vendorMobileNo)
      .set('shipToAddress', poData.shipToAddress)
      .set('scopeOfOrder', poData.scopeOfOrder)
      .set('shippingMethod', poData.shippingMethod)
      .set('shippingTerms', poData.shippingTerms)
      .set('dateOfCompletion', poData.dateOfCompletion)
      .set('lineItemsJson', JSON.stringify(poData.lineItems))
      .set('subtotalAmount', poData.subtotalAmount.toString())
      .set('freightCharges', poData.freightCharges.toString())
      .set('gstRate', poData.gstRate.toString())
      .set('gstAmount', poData.gstAmount.toString())
      .set('totalAmount', poData.totalAmount.toString())
      .set('department', poData.department);
    
    if (poData.termsAndConditions) {
      params.set('termsAndConditions', poData.termsAndConditions);
    }
    if (poData.paymentTerms) {
      params.set('paymentTerms', poData.paymentTerms);
    }
    if (poData.warranty) {
      params.set('warranty', poData.warranty);
    }

    return this.http.post<any>(`${this.baseUrl}/po/create`, null, { params });
  }

  getPurchaseOrders(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/po/list`, { params: authParams });
  }

  getPurchaseOrderById(id: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/po/${id}`, { params: authParams });
  }

  updatePurchaseOrder(id: number, poData: any): Observable<any> {
    const authParams = this.getAuthParams();

    const params = authParams
      .set('vendorName', poData.vendorName)
      .set('vendorAddress', poData.vendorAddress)
      .set('vendorContact', poData.vendorContact)
      .set('itemName', poData.itemName)
      .set('quantity', poData.quantity.toString())
      .set('unitPrice', poData.unitPrice.toString());
    
    if (poData.description) {
      params.set('description', poData.description);
    }
    if (poData.termsAndConditions) {
      params.set('termsAndConditions', poData.termsAndConditions);
    }

    return this.http.put<any>(`${this.baseUrl}/po/${id}`, null, { params });
  }

  updatePOStatus(id: number, status: string): Observable<any> {
    const authParams = this.getAuthParams();

    const params = authParams
      .set('status', status);

    return this.http.put<any>(`${this.baseUrl}/po/${id}/status`, null, { params });
  }

  deletePurchaseOrder(id: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.delete<any>(`${this.baseUrl}/po/${id}`, { params: authParams });
  }

  downloadPurchaseOrderPdf(poId: number): Observable<Blob> {
    const authParams = this.getAuthParams();
    return this.http.get(`${this.baseUrl}/po/${poId}/pdf`, {
      params: authParams,
      responseType: 'blob'
    });
  }

  sendPurchaseOrderEmail(poId: number, vendorEmail: string): Observable<any> {
    const authParams = this.getAuthParams();

    const params = authParams
      .set('vendorEmail', vendorEmail);

    return this.http.post<any>(`${this.baseUrl}/po/${poId}/send-email`, null, { params });
  }

  sendRequisitionEmail(requisitionId: number, vendorEmail: string, vendorName: string): Observable<any> {
    const authParams = this.getAuthParams();

    const params = authParams
      .set('vendorEmail', vendorEmail)
      .set('vendorName', vendorName);

    return this.http.post<any>(`${this.baseUrl}/requisitions/${requisitionId}/send-email`, null, { params });
  }

  updateRequisition(requisitionId: number, request: CreateRequisitionWithItemsRequest): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: Update requisition - requisitionId:', requisitionId);
    console.log('DEBUG: Update requisition - request:', request);
    
    console.log('DEBUG: Making PUT request to:', `${this.baseUrl}/requisitions/${requisitionId}`);
    console.log('DEBUG: With body:', request);
    
    return this.http.put<any>(`${this.baseUrl}/requisitions/${requisitionId}`, request, { params: authParams });
  }

  deleteRequisition(requisitionId: number): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: Delete requisition - requisitionId:', requisitionId);

    console.log('DEBUG: Making DELETE request to:', `${this.baseUrl}/requisitions/${requisitionId}`);
    
    return this.http.delete<any>(`${this.baseUrl}/requisitions/${requisitionId}`, { params: authParams });
  }

  // Helper method to group PDFs by user
  groupPdfsByUser(pdfs: VendorPdf[]): UserPdfGroup[] {
    const userGroups = new Map<string, VendorPdf[]>();
    
    // Group PDFs by uploadedBy
    pdfs.forEach(pdf => {
      if (!userGroups.has(pdf.uploadedBy)) {
        userGroups.set(pdf.uploadedBy, []);
      }
      userGroups.get(pdf.uploadedBy)!.push(pdf);
    });
    
    // Convert to UserPdfGroup array
    return Array.from(userGroups.entries()).map(([uploadedBy, userPdfs]) => {
      const processedCount = userPdfs.filter(pdf => pdf.processed).length;
      const pendingCount = userPdfs.length - processedCount;
      const lastUploadDate = userPdfs.reduce((latest, pdf) => 
        new Date(pdf.uploadedAt) > new Date(latest) ? pdf.uploadedAt : latest, 
        userPdfs[0].uploadedAt
      );
      
      return {
        uploadedBy,
        pdfs: userPdfs.sort((a, b) => new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime()),
        totalPdfs: userPdfs.length,
        processedCount,
        pendingCount,
        lastUploadDate
      };
    }).sort((a, b) => new Date(b.lastUploadDate).getTime() - new Date(a.lastUploadDate).getTime());
  }

  // Helper method to group PDFs by requisition
  groupPdfsByRequisition(pdfs: VendorPdf[], requisitions: Requisition[]): RequisitionPdfGroup[] {
    console.log('DEBUG: Grouping PDFs by requisition. PDFs:', pdfs);
    console.log('DEBUG: Available requisitions:', requisitions);
    console.log('DEBUG: Total PDFs to group:', pdfs.length);
    
    const requisitionGroups = new Map<number, VendorPdf[]>();
    const unlinkedPdfs: VendorPdf[] = [];
    
    // Group PDFs by requisitionId (only PDFs that have a requisitionId)
    pdfs.forEach((pdf, index) => {
      console.log(`DEBUG: Processing PDF ${index + 1}/${pdfs.length}:`, {
        id: pdf.id,
        fileName: pdf.originalFileName,
        requisitionId: pdf.requisitionId,
        uploadedBy: pdf.uploadedBy
      });
      
      if (pdf.requisitionId && pdf.requisitionId > 0) {
        if (!requisitionGroups.has(pdf.requisitionId)) {
          requisitionGroups.set(pdf.requisitionId, []);
        }
        requisitionGroups.get(pdf.requisitionId)!.push(pdf);
        console.log(`DEBUG: Added PDF to requisition group ${pdf.requisitionId}`);
      } else {
        // PDFs without requisitionId go to a special "unlinked" group
        unlinkedPdfs.push(pdf);
        console.log(`DEBUG: Added PDF to unlinked group (requisitionId: ${pdf.requisitionId})`);
      }
    });
    
    console.log('DEBUG: Requisition groups map:', requisitionGroups);
    console.log('DEBUG: Unlinked PDFs count:', unlinkedPdfs.length);
    console.log('DEBUG: Number of requisition groups:', requisitionGroups.size);
    
    // Convert to RequisitionPdfGroup array
    const result: RequisitionPdfGroup[] = [];
    
    // Add requisition-linked groups
    Array.from(requisitionGroups.entries()).forEach(([requisitionId, requisitionPdfs]) => {
      const processedCount = requisitionPdfs.filter(pdf => pdf.processed).length;
      const pendingCount = requisitionPdfs.length - processedCount;
      const lastUploadDate = requisitionPdfs.reduce((latest, pdf) => 
        new Date(pdf.uploadedAt) > new Date(latest) ? pdf.uploadedAt : latest, 
        requisitionPdfs[0].uploadedAt
      );
      
      // Find the requisition info
      const requisitionInfo = requisitions.find(req => req.id === requisitionId) || null;
      
      // Get the most common uploader for this requisition
      const uploaderCounts = new Map<string, number>();
      requisitionPdfs.forEach(pdf => {
        uploaderCounts.set(pdf.uploadedBy, (uploaderCounts.get(pdf.uploadedBy) || 0) + 1);
      });
      const uploadedBy = Array.from(uploaderCounts.entries())
        .sort((a, b) => b[1] - a[1])[0][0];
      
      result.push({
        requisitionId,
        requisitionInfo,
        pdfs: requisitionPdfs.sort((a, b) => new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime()),
        totalPdfs: requisitionPdfs.length,
        processedCount,
        pendingCount,
        lastUploadDate,
        uploadedBy
      });
    });
    
    // Add unlinked PDFs as a special group
    if (unlinkedPdfs.length > 0) {
      const processedCount = unlinkedPdfs.filter(pdf => pdf.processed).length;
      const pendingCount = unlinkedPdfs.length - processedCount;
      const lastUploadDate = unlinkedPdfs.reduce((latest, pdf) => 
        new Date(pdf.uploadedAt) > new Date(latest) ? pdf.uploadedAt : latest, 
        unlinkedPdfs[0].uploadedAt
      );
      
      // Get the most common uploader for unlinked PDFs
      const uploaderCounts = new Map<string, number>();
      unlinkedPdfs.forEach(pdf => {
        uploaderCounts.set(pdf.uploadedBy, (uploaderCounts.get(pdf.uploadedBy) || 0) + 1);
      });
      const uploadedBy = Array.from(uploaderCounts.entries())
        .sort((a, b) => b[1] - a[1])[0][0];
      
      result.push({
        requisitionId: 0, // Special ID for unlinked PDFs
        requisitionInfo: null,
        pdfs: unlinkedPdfs.sort((a, b) => new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime()),
        totalPdfs: unlinkedPdfs.length,
        processedCount,
        pendingCount,
        lastUploadDate,
        uploadedBy
      });
    }
    
    // Sort by last upload date
    result.sort((a, b) => new Date(b.lastUploadDate).getTime() - new Date(a.lastUploadDate).getTime());
    
    console.log('DEBUG: Final requisition groups result:', result);
    return result;
  }

  // Department Manager Methods
  getPendingDepartmentRequisitions(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/requisitions/pending/department`, { params: authParams });
  }

  departmentDecision(requisitionId: number, decision: string, comments?: string): Observable<any> {
    const authParams = this.getAuthParams();
    let params = authParams.set('decision', decision);
    if (comments) {
      params = params.set('comments', comments);
    }
    return this.http.post<any>(`${this.baseUrl}/requisitions/${requisitionId}/department-decision`, null, { params });
  }

  // PDF approval methods
  getDepartmentPendingPdfs(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/pdf/department-pending`, { params: authParams });
  }

  getAllDepartmentPdfs(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/pdf/department-all`, { params: authParams });
  }

  getItPendingPdfs(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/pdf/it-pending`, { params: authParams });
  }

  getAllItPdfs(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/pdf/it-all`, { params: authParams });
  }

  getFinancePendingPdfs(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/pdf/finance-pending`, { params: authParams });
  }

  departmentApprovePdf(pdfId: number): Observable<any> {
    const authParams = this.getAuthParams();
    console.log('DEBUG: departmentApprovePdf called with pdfId:', pdfId);
    const url = `${this.baseUrl}/pdf/department-approve/${pdfId}`;
    console.log('DEBUG: Making API call to:', url);
    return this.http.post<any>(url, null, { params: authParams });
  }

  itApprovePdf(pdfId: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.post<any>(`${this.baseUrl}/pdf/it-approve/${pdfId}`, null, { params: authParams });
  }

  financeApprovePdf(pdfId: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.post<any>(`${this.baseUrl}/pdf/finance-approve/${pdfId}`, null, { params: authParams });
  }

  // Master APIs (SUPERADMIN only)
  createUser(username: string, password: string, fullName: string, role: string, department: string): Observable<any> {
    const authParams = this.getAuthParams();

    const params = authParams
      .set('username', username)
      .set('password', password)
      .set('fullName', fullName)
      .set('role', role)
      .set('department', department);

    return this.http.post<any>(`${this.baseUrl}/auth/create-user`, null, { params });
  }

  getAllUsers(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/auth/users`, { params: authParams });
  }

  updateUserStatus(userId: number, isActive: boolean): Observable<any> {
    const authParams = this.getAuthParams();

    const params = authParams
      .set('userId', userId.toString())
      .set('isActive', isActive.toString());

    return this.http.post<any>(`${this.baseUrl}/auth/update-user-status`, null, { params });
  }

  // Department Manager Dashboard Methods
  getPendingDepartmentApprovals(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/requisitions/pending/department`, { params: authParams });
  }

  getActiveOrders(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/requisitions/active-orders`, { params: authParams });
  }

  getRecentRequisitions(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/requisitions/recent`, { params: authParams });
  }

  // Department Management Methods
  getAllDepartments(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/departments`, { params: authParams });
  }

  getDepartmentById(id: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/departments/${id}`, { params: authParams });
  }

  createDepartment(name: string, description: string, managerName?: string, managerUsername?: string, budget?: number): Observable<any> {
    const authParams = this.getAuthParams();
    const params: any = { ...authParams, name, description };
    if (managerName) params['managerName'] = managerName;
    if (managerUsername) params['managerUsername'] = managerUsername;
    if (budget) params['budget'] = budget.toString();
    return this.http.post<any>(`${this.baseUrl}/departments`, null, { params });
  }

  updateDepartment(id: number, name?: string, description?: string, managerName?: string, managerUsername?: string, budget?: number): Observable<any> {
    const authParams = this.getAuthParams();
    const params: any = { ...authParams };
    if (name) params['name'] = name;
    if (description) params['description'] = description;
    if (managerName) params['managerName'] = managerName;
    if (managerUsername) params['managerUsername'] = managerUsername;
    if (budget) params['budget'] = budget.toString();
    return this.http.put<any>(`${this.baseUrl}/departments/${id}`, null, { params });
  }

  deleteDepartment(id: number): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.delete<any>(`${this.baseUrl}/departments/${id}`, { params: authParams });
  }

  getDepartmentStats(): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.get<any>(`${this.baseUrl}/departments/stats`, { params: authParams });
  }

  // GRN methods
  createGrn(request: GrnReceiveRequest): Observable<any> {
    const authParams = this.getAuthParams();
    return this.http.post<any>(`${this.baseUrl}/grn/receive`, request, { params: authParams });
    }

  getGrnHistory(poId?: number): Observable<any> {
    let params = this.getAuthParams();
    if (poId) params = params.set('poId', poId.toString());
    return this.http.get<any>(`${this.baseUrl}/grn/history`, { params });
  }

  downloadGrnPdf(grnId: number): Observable<Blob> {
    const authParams = this.getAuthParams();
    return this.http.get(`${this.baseUrl}/grn/${grnId}/pdf`, {
      params: authParams,
      responseType: 'blob'
    });
  }
}
// GRN API additions
export interface GrnReceiveItem {
  description: string;
  orderedQty: number;
  receiveQty: number;
  unit: string;
  received: boolean;
  status?: string;
  remarks?: string;
}

export interface GrnReceiveRequest {
  poId: number;
  receivedBy: string;
  receivedDate: string; // yyyy-MM-dd
  overallRemarks?: string;
  items: GrnReceiveItem[];
}

// Append GRN methods to existing ApiService class above