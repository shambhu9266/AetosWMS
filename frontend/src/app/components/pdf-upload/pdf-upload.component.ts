import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, VendorPdf, Requisition } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-pdf-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pdf-upload.component.html',
  styleUrls: ['./pdf-upload.component.css']
})
export class PdfUploadComponent implements OnInit {
  constructor(private apiService: ApiService, private authService: AuthService) {}

  protected readonly uploadedPdfs = signal<VendorPdf[]>([]);
  protected readonly availableRequisitions = signal<Requisition[]>([]);
  
  // PDF upload properties
  selectedFiles: File[] = [];
  pdfDescription: string = '';
  selectedRequisitionId: number | null = null;
  isUploading = false;
  uploadSuccess = false;
  uploadError = '';
  uploadProgress = 0;
  currentUploadingFile = '';

  ngOnInit() {
    this.loadPdfs();
    this.loadAvailableRequisitions();
  }

  private loadPdfs() {
    this.apiService.getPdfs().subscribe({
      next: (response) => {
        if (response.success) {
          this.uploadedPdfs.set(response.pdfs || []);
        }
      },
      error: (error) => {
        console.error('Error loading PDFs:', error);
      }
    });
  }

  private loadAvailableRequisitions() {
    // Load pending IT approvals for IT managers to link PDFs
    this.apiService.getPendingItApprovals().subscribe({
      next: (response) => {
        if (response.success) {
          this.availableRequisitions.set(response.requisitions || []);
        }
      },
      error: (error) => {
        console.error('Error loading requisitions:', error);
      }
    });
  }

  onFileSelected(event: any) {
    const files = Array.from(event.target.files) as File[];
    const validFiles: File[] = [];
    const invalidFiles: string[] = [];

    files.forEach(file => {
      if (file.type === 'application/pdf') {
        validFiles.push(file);
      } else {
        invalidFiles.push(file.name);
      }
    });

    if (validFiles.length > 0) {
      this.selectedFiles = validFiles;
      this.uploadError = '';
      this.uploadSuccess = false;
      
      if (invalidFiles.length > 0) {
        this.uploadError = `Invalid files skipped: ${invalidFiles.join(', ')}. Only PDF files are allowed.`;
      }
    } else {
      this.uploadError = 'Please select valid PDF files.';
      event.target.value = '';
      this.selectedFiles = [];
    }
  }

  uploadPdf() {
    if (this.selectedFiles.length === 0) {
      this.uploadError = 'Please select PDF files to upload.';
      return;
    }

    this.isUploading = true;
    this.uploadError = '';
    this.uploadSuccess = false;
    this.uploadProgress = 0;

    this.uploadFilesSequentially(0);
  }

  private uploadFilesSequentially(index: number) {
    if (index >= this.selectedFiles.length) {
      // All files uploaded successfully
      this.uploadSuccess = true;
      this.uploadError = '';
      this.isUploading = false;
      this.uploadProgress = 100;
      this.resetForm();
      this.loadPdfs(); // Reload PDFs list
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        this.uploadSuccess = false;
      }, 3000);
      return;
    }

    const file = this.selectedFiles[index];
    this.currentUploadingFile = file.name;
    this.uploadProgress = Math.round((index / this.selectedFiles.length) * 100);

    this.apiService.uploadPdf(file, this.pdfDescription, this.selectedRequisitionId || undefined).subscribe({
      next: (response) => {
        if (response.success) {
          // Upload next file
          this.uploadFilesSequentially(index + 1);
        } else {
          this.uploadError = `Failed to upload ${file.name}: ${response.message || 'Unknown error'}`;
          this.isUploading = false;
        }
      },
      error: (error) => {
        console.error('Upload error for', file.name, ':', error);
        this.uploadError = `Failed to upload ${file.name}. Please try again.`;
        this.isUploading = false;
      }
    });
  }

  downloadPdf(pdf: VendorPdf) {
    this.apiService.downloadPdf(pdf.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = pdf.originalFileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error downloading PDF:', error);
        this.uploadError = 'Failed to download PDF. Please try again.';
      }
    });
  }

  deletePdf(pdf: VendorPdf) {
    if (confirm(`Are you sure you want to delete "${pdf.originalFileName}"? This action cannot be undone.`)) {
      this.apiService.deletePdf(pdf.id).subscribe({
        next: (response) => {
          if (response.success) {
            alert('PDF deleted successfully!');
            this.loadPdfs(); // Reload PDFs list
          } else {
            alert('Failed to delete PDF: ' + response.message);
          }
        },
        error: (error) => {
          console.error('Error deleting PDF:', error);
          alert('Failed to delete PDF. Please try again.');
        }
      });
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  resetForm() {
    this.selectedFiles = [];
    this.pdfDescription = '';
    this.selectedRequisitionId = null;
    this.uploadProgress = 0;
    this.currentUploadingFile = '';
  }

  getTotalFileSize(): string {
    const totalBytes = this.selectedFiles.reduce((sum, file) => sum + file.size, 0);
    return this.formatFileSize(totalBytes);
  }

  canAccessITApprovals(): boolean {
    return this.authService.canAccessITApprovals();
  }
}
