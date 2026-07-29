import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  username = signal('');
  password = signal('');
  rememberMe = signal(true);
  showPassword = signal(false);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  currentYear = new Date().getFullYear();

  ngOnInit() {
    if (this.authService.checkAuth()) {
      this.router.navigate(['/']);
    }
  }

  async signIn() {
    if (this.isLoading()) return;

    const user = this.username().trim();
    const pass = this.password();

    if (!user || !pass) {
      this.errorMessage.set('Veuillez renseigner votre identifiant et votre mot de passe.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    try {
      await this.authService.login(user, pass);
      this.router.navigate(['/']);
    } catch (err: any) {
      const status = err?.status;
      if (status === 401 || status === 400) {
        this.errorMessage.set('Identifiant ou mot de passe incorrect.');
      } else if (status === 0) {
        this.errorMessage.set(
          'Impossible de joindre le serveur d\'authentification. Veuillez vérifier votre connexion.'
        );
      } else {
        this.errorMessage.set('Une erreur est survenue lors de la connexion.');
      }
    } finally {
      this.isLoading.set(false);
    }
  }

  fillDemoCredentials(user: string, pass: string) {
    this.username.set(user);
    this.password.set(pass);
    this.errorMessage.set(null);
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.signIn();
    }
  }

  togglePassword() {
    this.showPassword.update(v => !v);
  }
}
