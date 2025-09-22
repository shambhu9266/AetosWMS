import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginResponse } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  protected readonly username = signal<string>('');
  protected readonly password = signal<string>('');
  protected readonly isLoading = signal<boolean>(false);
  protected readonly errorMessage = signal<string>('');

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onLogin() {
    if (!this.username() || !this.password()) {
      this.errorMessage.set('Please enter both username and password');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.login(this.username(), this.password()).subscribe({
      next: (response: LoginResponse) => {
        this.isLoading.set(false);
        if (response.success) {
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage.set(response.message || 'Login failed');
        }
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set('Login failed. Please try again.');
        console.error('Login error:', error);
      }
    });
  }

}
