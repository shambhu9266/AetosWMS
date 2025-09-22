import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-grn',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="placeholder-page">
      <div class="placeholder-content">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M20 7L9 18L4 13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <h3>Goods Receipt Notes (GRN)</h3>
        <p>GRN management will be implemented here.</p>
      </div>
    </div>
  `,
  styles: [`
    .placeholder-page {
      padding: 2rem;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 400px;
    }
    .placeholder-content {
      text-align: center;
      color: #64748b;
    }
    .placeholder-content svg {
      color: #94a3b8;
      margin-bottom: 1rem;
    }
    .placeholder-content h3 {
      margin: 0 0 0.5rem 0;
      color: #1e293b;
      font-size: 1.5rem;
    }
  `]
})
export class GrnComponent {}
