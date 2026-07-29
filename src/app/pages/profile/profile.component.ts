import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  authService = inject(AuthService);

  userInfo = this.authService.userInfo;

  // Form Signals for Profile Info
  firstName = signal('');
  lastName = signal('');
  email = signal('');

  // Form Signals for Password Update
  currentPassword = signal('');
  newPassword = signal('');
  confirmPassword = signal('');
  showCurrentPw = signal(false);
  showNewPw = signal(false);

  // Status Alerts
  profileSuccessMsg = signal<string | null>(null);
  profileErrorMsg = signal<string | null>(null);
  isSavingProfile = signal(false);

  passwordSuccessMsg = signal<string | null>(null);
  passwordErrorMsg = signal<string | null>(null);
  isSavingPassword = signal(false);

  keycloakAccountUrl = this.authService.getKeycloakAccountUrl();

  ngOnInit() {
    const user = this.userInfo();
    if (user) {
      this.firstName.set(user.firstName || '');
      this.lastName.set(user.lastName || '');
      this.email.set(user.email || '');
    }
  }

  saveProfile() {
    this.profileSuccessMsg.set(null);
    this.profileErrorMsg.set(null);

    const fn = this.firstName().trim();
    const ln = this.lastName().trim();
    const em = this.email().trim();

    if (!fn || !ln) {
      this.profileErrorMsg.set('Veuillez renseigner votre prénom et votre nom.');
      return;
    }

    this.isSavingProfile.set(true);

    try {
      this.authService.updateUserProfile({
        firstName: fn,
        lastName: ln,
        email: em,
      });

      this.profileSuccessMsg.set('Profil mis à jour avec succès !');
    } catch (err: any) {
      this.profileErrorMsg.set('Une erreur est survenue lors de la mise à jour du profil.');
    } finally {
      this.isSavingProfile.set(false);
    }
  }

  async savePassword() {
    this.passwordSuccessMsg.set(null);
    this.passwordErrorMsg.set(null);

    const cur = this.currentPassword();
    const nPw = this.newPassword();
    const cPw = this.confirmPassword();

    if (!cur || !nPw || !cPw) {
      this.passwordErrorMsg.set('Veuillez remplir tous les champs du mot de passe.');
      return;
    }

    if (nPw.length < 6) {
      this.passwordErrorMsg.set('Le nouveau mot de passe doit contenir au moins 6 caractères.');
      return;
    }

    if (nPw !== cPw) {
      this.passwordErrorMsg.set('Le nouveau mot de passe et la confirmation ne correspondent pas.');
      return;
    }

    this.isSavingPassword.set(true);

    try {
      await this.authService.updatePassword(cur, nPw);
      this.passwordSuccessMsg.set('Mot de passe modifié avec succès ! Vous allez être déconnecté…');
      this.currentPassword.set('');
      this.newPassword.set('');
      this.confirmPassword.set('');

      // Déconnexion automatique : la session actuelle n'est plus valide,
      // l'utilisateur doit se reconnecter avec son nouveau mot de passe.
      setTimeout(() => this.authService.logout(), 2000);
    } catch (err: any) {
      this.passwordErrorMsg.set(err.message || 'Impossible de modifier le mot de passe.');
      this.isSavingPassword.set(false);
    }
  }

  toggleCurrentPw() {
    this.showCurrentPw.update(v => !v);
  }

  toggleNewPw() {
    this.showNewPw.update(v => !v);
  }
}
