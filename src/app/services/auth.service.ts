import { Injectable, signal, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { map, tap, catchError, finalize, shareReplay } from 'rxjs/operators';

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  refresh_expires_in: number;
  token_type: string;
}

export interface UserInfo {
  name: string;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  initials: string;
  role: string;
  roles: string[];
}

const KEYCLOAK_URL = 'http://localhost:8180';
const REALM = 'insurance';
const CLIENT_ID = 'insurance-frontend';

const TOKEN_KEY = 'sl_access_token';
const REFRESH_KEY = 'sl_refresh_token';
const EXPIRY_KEY = 'sl_token_expiry';
const USER_OVERRIDE_KEY = 'sl_user_override';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private tokenEndpoint = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
  private logoutEndpoint = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`;

  // Reactive state
  isAuthenticated = signal(false);
  userInfo = signal<UserInfo | null>(null);

  // Un seul refresh en cours partagé entre les requêtes concurrentes
  private refreshInFlight$: Observable<string | null> | null = null;

  constructor() {
    // Restore session on startup
    this.restoreSession();
  }

  /** Log in with username+password via Direct Access Grant */
  async login(username: string, password: string): Promise<void> {
    const body = new URLSearchParams({
      grant_type: 'password',
      client_id: CLIENT_ID,
      username,
      password,
    });

    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
    });

    const response = await firstValueFrom(
      this.http.post<TokenResponse>(this.tokenEndpoint, body.toString(), { headers })
    );

    // Clear any previous session overrides on new login
    sessionStorage.removeItem(USER_OVERRIDE_KEY);

    this.storeTokens(response);
    this.parseAndSetUser(response.access_token);
    this.isAuthenticated.set(true);
  }

  /** Update local user profile info */
  updateUserProfile(updated: { firstName: string; lastName: string; email: string }) {
    const current = this.userInfo();
    if (!current) return;

    const fullName = [updated.firstName, updated.lastName].filter(Boolean).join(' ') || current.username;
    const parts = fullName.trim().split(' ');
    let initials = 'U';
    if (parts.length >= 2) {
      initials = (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    } else if (parts[0]?.length) {
      initials = parts[0].slice(0, 2).toUpperCase();
    }

    const newUserInfo: UserInfo = {
      ...current,
      firstName: updated.firstName,
      lastName: updated.lastName,
      name: fullName,
      email: updated.email,
      initials,
    };

    this.userInfo.set(newUserInfo);
    sessionStorage.setItem(USER_OVERRIDE_KEY, JSON.stringify(newUserInfo));
  }

  /**
   * Changes the user's password for real, via the backend endpoint which uses
   * the Keycloak Admin API (verifies the current password, then resets it).
   */
  async updatePassword(currentPass: string, newPass: string): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.post('/api/profile/change-password', {
          currentPassword: currentPass,
          newPassword: newPass,
        })
      );
      return true;
    } catch (err: any) {
      const message = err?.error?.error || 'Impossible de modifier le mot de passe.';
      throw new Error(message);
    }
  }

  /** Get Keycloak Account Console URL */
  getKeycloakAccountUrl(): string {
    return `${KEYCLOAK_URL}/realms/${REALM}/account`;
  }

  /** Log out: clear tokens + notify Keycloak */
  async logout(): Promise<void> {
    const refreshToken = sessionStorage.getItem(REFRESH_KEY);

    if (refreshToken) {
      try {
        const body = new URLSearchParams({
          client_id: CLIENT_ID,
          refresh_token: refreshToken,
        });
        const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });
        await firstValueFrom(
          this.http.post(this.logoutEndpoint, body.toString(), { headers })
        );
      } catch {
        // Best-effort logout on server
      }
    }

    this.clearTokens();
    this.router.navigate(['/login']);
  }

  /** Returns the stored access token if still valid, otherwise null (no side effect). */
  getToken(): string | null {
    const token = sessionStorage.getItem(TOKEN_KEY);
    const expiry = sessionStorage.getItem(EXPIRY_KEY);

    if (!token || !expiry) return null;
    if (Date.now() > parseInt(expiry, 10)) return null;
    return token;
  }

  /**
   * Returns a valid access token, refreshing transparently if it has expired.
   * Used by the HTTP interceptor BEFORE sending each API request.
   */
  getValidToken(): Observable<string | null> {
    const current = this.getToken();
    if (current) return of(current);

    const refreshToken = sessionStorage.getItem(REFRESH_KEY);
    if (!refreshToken) return of(null);

    return this.refreshTokens(refreshToken);
  }

  /** Forces a token refresh (used to retry a request that returned 401). */
  forceRefresh(): Observable<string | null> {
    const refreshToken = sessionStorage.getItem(REFRESH_KEY);
    if (!refreshToken) {
      this.clearTokens();
      this.router.navigate(['/login']);
      return of(null);
    }
    return this.refreshTokens(refreshToken);
  }

  /**
   * L'utilisateur est considéré authentifié s'il a un token d'accès valide OU
   * un refresh token (la session peut être rafraîchie de façon transparente).
   * Évite d'être déconnecté à chaque expiration du token d'accès (~5 min),
   * puisque le garde de navigation s'appuie sur cette méthode.
   */
  checkAuth(): boolean {
    if (this.getToken()) return true;
    return !!sessionStorage.getItem(REFRESH_KEY);
  }

  // ─── Private helpers ───────────────────────────────────────

  private storeTokens(response: TokenResponse): void {
    const expiry = Date.now() + response.expires_in * 1000 - 15000;
    sessionStorage.setItem(TOKEN_KEY, response.access_token);
    sessionStorage.setItem(REFRESH_KEY, response.refresh_token);
    sessionStorage.setItem(EXPIRY_KEY, expiry.toString());
  }

  private clearTokens(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_KEY);
    sessionStorage.removeItem(EXPIRY_KEY);
    sessionStorage.removeItem(USER_OVERRIDE_KEY);
    this.isAuthenticated.set(false);
    this.userInfo.set(null);
  }

  private restoreSession(): void {
    const token = this.getToken();
    if (token) {
      this.parseAndSetUser(token);
      this.isAuthenticated.set(true);
      return;
    }

    // Token d'accès expiré au démarrage (ex: après un F5) : si un refresh token
    // existe encore, on le rafraîchit au lieu de considérer l'utilisateur déconnecté.
    const refreshToken = sessionStorage.getItem(REFRESH_KEY);
    if (refreshToken) {
      this.refreshTokens(refreshToken).subscribe();
    }
  }

  /**
   * Performs a Keycloak refresh_token grant and returns the new access token.
   * Concurrent callers share a single in-flight request. If the refresh token
   * itself is invalid/expired, the session is cleared and the user is redirected.
   */
  private refreshTokens(refreshToken: string): Observable<string | null> {
    if (this.refreshInFlight$) return this.refreshInFlight$;

    const body = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: CLIENT_ID,
      refresh_token: refreshToken,
    });
    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    this.refreshInFlight$ = this.http
      .post<TokenResponse>(this.tokenEndpoint, body.toString(), { headers })
      .pipe(
        tap(res => {
          this.storeTokens(res);
          this.parseAndSetUser(res.access_token);
          this.isAuthenticated.set(true);
        }),
        map(res => res.access_token as string | null),
        catchError(() => {
          this.clearTokens();
          this.router.navigate(['/login']);
          return of(null);
        }),
        finalize(() => { this.refreshInFlight$ = null; }),
        shareReplay(1)
      );

    return this.refreshInFlight$;
  }

  private decodeJwt(token: string): any {
    try {
      const payload = token.split('.')[1];
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decoded);
    } catch {
      return null;
    }
  }

  private parseAndSetUser(accessToken: string): void {
    // Check if user saved updated info during this session
    const override = sessionStorage.getItem(USER_OVERRIDE_KEY);
    if (override) {
      try {
        const parsed = JSON.parse(override);
        this.userInfo.set(parsed);
        return;
      } catch {
        // Fallback to JWT payload
      }
    }

    const payload = this.decodeJwt(accessToken);
    if (!payload) return;

    const firstName = payload.given_name || '';
    const lastName = payload.family_name || '';
    const fullName = [firstName, lastName].filter(Boolean).join(' ')
                     || payload.name
                     || payload.preferred_username
                     || 'Utilisateur';

    const parts = fullName.trim().split(' ');
    let initials = 'U';
    if (parts.length >= 2) {
      initials = (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    } else if (parts[0]?.length) {
      initials = parts[0].slice(0, 2).toUpperCase();
    }

    const roles: string[] = payload?.realm_access?.roles ?? [];
    let role = 'Utilisateur Connecté';
    if (roles.some(r => r.toLowerCase().includes('admin'))) role = 'Administrateur';
    else if (roles.some(r => r.toLowerCase().includes('agent'))) role = 'Agent Solife';

    this.userInfo.set({
      name: fullName,
      firstName,
      lastName,
      username: payload.preferred_username || '',
      email: payload.email || '',
      initials,
      role,
      roles,
    });
  }
}
