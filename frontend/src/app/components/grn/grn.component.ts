import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PurchaseOrder, PurchaseOrderLineItem, GrnReceiveItem, GrnReceiveRequest } from '../../services/api.service';

@Component({
  selector: 'app-grn',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="grn-page">
      <h2>Goods Receipt (GRN)</h2>
      <p class="grn-description">Confirm receipt of goods for shipped/approved Purchase Orders.</p>
      <div class="toolbar">
        <label class="checkbox">
          <input type="checkbox" [(ngModel)]="includeCompletedFlag" (change)="recomputeDisplay()">
          <span>Show completed (DELIVERED) POs</span>
        </label>
      </div>
      <div *ngIf="loading()" class="loading">Loading purchase orders...</div>
      <div *ngIf="!loading() && getDisplayedPOs().length === 0" class="empty-state">No purchase orders to show.</div>
      <table class="grn-table" *ngIf="!loading() && getDisplayedPOs().length > 0">
        <thead>
          <tr>
            <th>PO Number</th>
            <th>Vendor</th>
            <th>Date</th>
            <th>Department</th>
            <th>Total</th>
            <th>Status</th>
            <th>Action</th>
            <th>PDF</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let po of getDisplayedPOs()">
            <td>{{ po.poNumber }}</td>
            <td>{{ po.vendorName }}</td>
            <td>{{ po.poDate | date: 'shortDate' }}</td>
            <td>{{ po.department }}</td>
            <td>{{ po.totalAmount | currency:'INR':'symbol-narrow' }}</td>
            <td><span class="status-label">{{ po.status }}</span></td>
            <td><button class="btn-primary" (click)="openReceiveDialog(po)">Receive</button></td>
            <td>
              <button class="btn-secondary" *ngIf="getLatestGrnIdForPo(po) as gid; else noGrn" (click)="downloadGrn(gid)">Download PDF</button>
              <ng-template #noGrn><span class="muted">—</span></ng-template>
            </td>
          </tr>
        </tbody>
      </table>
      <!-- Receive dialog -->
      <div *ngIf="showDialog()" class="modal-overlay" (click)="closeDialog()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>Receive Goods for PO #{{ selectedPO()?.poNumber }}</h3>
            <button class="modal-close" (click)="closeDialog()">×</button>
          </div>
          <form (ngSubmit)="submitReceipt()">
            <div class="line-items-list">
              <table>
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Ordered</th>
                    <th>Receive?</th>
                    <th>Receive Qty</th>
                    <th>UOM</th>
                    <th>Remarks</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of receiveForm.lineItems; let i = index">
                    <td>{{ item.description }}</td>
                    <td>{{ item.quantity }}</td>
                    <td>
                      <label class="checkbox">
                        <input type="checkbox" [(ngModel)]="item.received" name="received_{{i}}" (change)="onToggleReceived(item)">
                        <span>Yes</span>
                      </label>
                    </td>
                    <td>
                      <input type="number" [(ngModel)]="item.receiveQty" name="receiveQty_{{i}}" min="0" max="{{item.quantity}}" [required]="item.received" [disabled]="!item.received" class="input-sm">
                    </td>
                    <td>{{ item.unit }}</td>
                    <td>
                      <div class="chip-group">
                        <label class="chip" [class.active]="item.status==='OK'">
                          <input type="radio" [(ngModel)]="item.status" name="status_{{i}}" [value]="'OK'"> OK
                        </label>
                        <label class="chip warning" [class.active]="item.status==='SHORT'">
                          <input type="radio" [(ngModel)]="item.status" name="status_{{i}}" [value]="'SHORT'"> Short
                        </label>
                        <label class="chip danger" [class.active]="item.status==='DAMAGED'">
                          <input type="radio" [(ngModel)]="item.status" name="status_{{i}}" [value]="'DAMAGED'"> Damaged
                        </label>
                        <label class="chip muted" [class.active]="item.status==='REJECTED'">
                          <input type="radio" [(ngModel)]="item.status" name="status_{{i}}" [value]="'REJECTED'"> Rejected
                        </label>
                      </div>
                      <input type="text" [(ngModel)]="item.remarks" name="remarks_{{i}}" class="input-sm" placeholder="Remarks (optional)">
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="receiver-block">
              <label>Received By</label><input [(ngModel)]="receiveForm.receivedBy" name="receivedBy" type="text" class="input-sm" required>
              <label>Received Date</label><input [(ngModel)]="receiveForm.receivedDate" name="receivedDate" type="date" class="input-sm" required>
              <label>Remarks</label><input [(ngModel)]="receiveForm.overallRemarks" name="overallRemarks" type="text" class="input-sm">
            </div>
            <div class="modal-footer">
              <button type="button" class="btn-secondary" (click)="closeDialog()">Cancel</button>
              <button type="submit" class="btn-primary">Submit Receipt</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .grn-page { padding: 2rem; max-width: 1200px; margin: 0 auto; }
    .grn-description { color: #6b7280; margin-bottom: 1.5rem; }
    .grn-table { width: 100%; border-collapse: collapse; margin-bottom: 2rem; }
    .grn-table th, .grn-table td { padding: 0.75rem 1rem; border-bottom: 1px solid #e5e7eb; }
    .empty-state { color: #6b7280; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 0.75rem; padding: 1rem; margin-bottom: 1rem; }
    .toolbar { margin-bottom: 1rem; }
    .status-label { padding: 0.25rem 0.75rem; background: #fef3c7; color: #b45309; border-radius: 1rem; font-size: 0.9em; }
    .btn-primary { background: #059669; color: white; border: none; border-radius: 0.4rem; padding: 0.5rem 1.2rem; cursor: pointer; margin-right: 0.5rem; }
    .btn-secondary { background: #e5e7eb; color: #374151; border: none; border-radius: 0.4rem; padding: 0.5rem 1.2rem; cursor: pointer; }
    .btn-primary:hover { background: #047857; }
    .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background:rgba(0,0,0,0.3); display: flex; align-items:center; justify-content:center; z-index: 1200; }
    .modal-content { background: white; border-radius: 1rem; box-shadow: 0 8px 32px rgba(0,0,0,0.25); max-width: 650px; width: 99vw; padding: 2rem 2.5rem; position: relative; }
    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.2rem; }
    .modal-close { background: none; border: none; font-size: 2rem; line-height: 1; color: #6b7280; cursor: pointer; }
    .line-items-list table { width:100%; border-collapse: collapse; margin-bottom: 1.2rem; }
    .line-items-list th, .line-items-list td { padding: 0.5rem 1rem; border-bottom: 1px solid #f3f4f6; }
    .input-sm { padding: 0.35rem; font-size: 0.95em; border-radius: 0.3rem; border:1px solid #d1d5db; }
    .receiver-block label { display: inline-block; width: 110px; margin-bottom: 0.2rem;}
    .receiver-block input { margin-right: 2rem; margin-bottom: 0.5rem;}
    .loading { color: #6b7280; margin: 2rem 0; text-align: center;}
    .history h3 { margin: 1rem 0; }
    .chip-group { display: flex; gap: 0.25rem; margin-bottom: 0.35rem; }
    .chip { border: 1px solid #d1d5db; border-radius: 999px; padding: 0.15rem 0.6rem; font-size: 0.8em; cursor: pointer; color: #374151; background: #fff; display: inline-flex; align-items: center; gap: 0.25rem; }
    .chip input { display: none; }
    .chip.active { border-color: #10b981; background: #ecfdf5; color: #065f46; }
    .chip.warning.active { border-color: #f59e0b; background: #fffbeb; color: #92400e; }
    .chip.danger.active { border-color: #ef4444; background: #fef2f2; color: #991b1b; }
    .chip.muted.active { border-color: #9ca3af; background: #f3f4f6; color: #4b5563; }
  `]
})
export class GrnComponent implements OnInit {
  loading = signal(true);
  allPOs = signal<PurchaseOrder[]>([]);
  showDialog = signal(false);
  selectedPO = signal<PurchaseOrder|null>(null);
  receiveForm: any = { lineItems: [], receivedBy: '', receivedDate: '', overallRemarks: '' };
  history = signal<any[]>([]);
  private latestGrnByPoNumber = new Map<string, number>();
  includeCompletedFlag: boolean = true;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getPurchaseOrders().subscribe(res => {
      const all: PurchaseOrder[] = (res.pos || []).map((po: any) => {
        // Ensure lineItems are available even if backend sends JSON string
        if ((!po.lineItems || po.lineItems.length === 0) && po.lineItemsJson) {
          try {
            po.lineItems = JSON.parse(po.lineItemsJson);
          } catch {
            po.lineItems = [];
          }
        }
        return po as PurchaseOrder;
      });
      this.allPOs.set(all);
      this.loading.set(false);
    });
    this.loadHistory();
  }

  openReceiveDialog(po: PurchaseOrder) {
    this.selectedPO.set(po);
    // Flat copy with receive qty inputs defaulted to full ordered
    this.receiveForm.lineItems = po.lineItems.map((item: PurchaseOrderLineItem) => ({ ...item, receiveQty: Number(item.quantity), received: true, remarks: '' }));
    this.receiveForm.receivedBy = '';
    this.receiveForm.receivedDate = (new Date()).toISOString().substring(0,10);
    this.receiveForm.overallRemarks = '';
    this.showDialog.set(true);
  }

  closeDialog() { this.showDialog.set(false); this.selectedPO.set(null); }

  submitReceipt() {
    const po = this.selectedPO();
    if (!po) return;
    const items: GrnReceiveItem[] = this.receiveForm.lineItems.map((it: any) => ({
      description: it.description,
      orderedQty: Number(it.quantity) || 0,
      receiveQty: Number(it.receiveQty) || 0,
      unit: it.unit,
      received: !!it.received,
      status: it.status || (it.received ? 'OK' : 'REJECTED'),
      remarks: it.remarks
    }));
    const payload: GrnReceiveRequest = {
      poId: po.id,
      receivedBy: this.receiveForm.receivedBy,
      receivedDate: this.receiveForm.receivedDate,
      overallRemarks: this.receiveForm.overallRemarks,
      items
    };

    this.api.createGrn(payload).subscribe({
      next: (res) => {
        alert(res.message || 'GRN recorded successfully');
        // Optimistic: mark latest GRN available for this PO so PDF button appears
        const poNum = this.selectedPO()!.poNumber;
        if (res.grnId) {
          this.latestGrnByPoNumber.set(poNum, res.grnId);
        }
        this.closeDialog();
        // Re-fetch history silently to update latest GRN mapping
        this.loadHistory();
      },
      error: (err) => {
        alert('Failed to record GRN: ' + (err.error?.message || err.message));
      }
    });
  }

  private refreshPOs() {
    this.loading.set(true);
    this.api.getPurchaseOrders().subscribe(res => {
      const all: PurchaseOrder[] = (res.pos || []).map((po: any) => {
        if ((!po.lineItems || po.lineItems.length === 0) && po.lineItemsJson) {
          try { po.lineItems = JSON.parse(po.lineItemsJson); } catch { po.lineItems = []; }
        }
        return po as PurchaseOrder;
      });
      this.allPOs.set(all);
      this.loading.set(false);
    });
    this.loadHistory();
  }

  private loadHistory() {
    this.api.getGrnHistory().subscribe({
      next: (res) => {
        if (res.success) {
          const list = res.grns || [];
          this.history.set(list);
          // build index of latest GRN per PO number
          this.latestGrnByPoNumber.clear();
          list.forEach((g: any) => {
            const key = g.poNumber;
            const id = g.id;
            if (!this.latestGrnByPoNumber.has(key) || (this.latestGrnByPoNumber.get(key)! < id)) {
              this.latestGrnByPoNumber.set(key, id);
            }
          });
        }
      },
      error: () => {}
    });
  }

  downloadGrn(id: number) {
    this.api.downloadGrnPdf(id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `GRN_${id}.pdf`;
        document.body.appendChild(a); a.click(); document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => { alert('Failed to download GRN PDF'); }
    });
  }

  getLatestGrnIdForPo(po: PurchaseOrder): number | null {
    return this.latestGrnByPoNumber.get(po.poNumber) ?? null;
  }

  recomputeDisplay() { /* trigger change detection via bound flag */ }

  getDisplayedPOs(): PurchaseOrder[] {
    const list = this.allPOs();
    if (this.includeCompletedFlag) {
      return list.filter(po => po.status !== 'CANCELLED');
    }
    const allowed = ['SENT_TO_VENDOR','ACKNOWLEDGED','IN_PRODUCTION','SHIPPED'];
    return list.filter(po => allowed.includes(po.status));
  }

  onToggleReceived(item: any) {
    if (!item.received) {
      item.receiveQty = 0;
    } else if (!item.receiveQty || item.receiveQty === 0) {
      // default to ordered qty when toggled on
      item.receiveQty = Number(item.quantity) || 0;
    }
  }
}

function defaultReceiptToast() { alert('Receipt processing not yet implemented (stub). Form values captured.'); }
