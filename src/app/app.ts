import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router, NavigationEnd } from '@angular/router';
import { ChatComponent } from './features/agent/chat/chat';
import { AuthService } from './services/auth.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, ChatComponent, TranslocoModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class AppComponent implements OnInit {
  authService = inject(AuthService);
  private router = inject(Router);
  private transloco = inject(TranslocoService);

  activeLang = signal('fr');

  setLang(lang: string) {
    this.transloco.setActiveLang(lang);
    this.activeLang.set(lang);
    localStorage.setItem('lang', lang);
  }

  sidebarCollapsed = signal(false);
  chatOpen = signal(false);
  isCheckingAuth = signal(true);

  // Derived from AuthService signals
  isAuthenticated = this.authService.isAuthenticated;
  userInfo = this.authService.userInfo;

  userName = computed(() => this.userInfo()?.name ?? 'Utilisateur');
  userInitials = computed(() => this.userInfo()?.initials ?? 'U');
  userRole = computed(() => this.userInfo()?.role ?? 'Utilisateur');

  async ngOnInit() {
    // Restaure la langue choisie précédemment
    const savedLang = localStorage.getItem('lang');
    if (savedLang) this.transloco.setActiveLang(savedLang);
    this.activeLang.set(this.transloco.getActiveLang());

    // Guard routes on navigation
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        if (!this.authService.checkAuth() && !e.urlAfterRedirects.startsWith('/login')) {
          this.router.navigate(['/login']);
        }
      });

    // Initial auth check (session already restored in AuthService constructor)
    const loggedIn = this.authService.checkAuth();
    if (!loggedIn) {
      const current = this.router.url;
      if (!current.startsWith('/login')) {
        this.router.navigate(['/login']);
      }
    }
    this.isCheckingAuth.set(false);
  }

  logout() {
    this.authService.logout();
  }

  // ── Barre latérale : recherche / commandes ──────────────────────────────
  searchOpen = signal(false);
  searchQuery = signal('');

  readonly commands: { label: string; tab?: string; route?: string }[] = [
    { label: 'Tableau de bord', tab: '' },
    { label: 'Polices & Contrats', tab: 'policies' },
    { label: 'Nouvelle souscription (police)', route: '/subscription' },
    { label: 'Tiers & Clients', tab: 'third-parties' },
    { label: 'Nouveau tiers', route: '/add-third-party' },
    { label: 'Facturation', tab: 'bills' },
    { label: 'Garanties', tab: 'coverages' },
    { label: 'Avenants', tab: 'endorsements' },
    { label: 'Mon profil', route: '/profile' },
  ];

  filteredCommands = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    return q ? this.commands.filter(c => c.label.toLowerCase().includes(q)) : this.commands;
  });

  toggleSearch() {
    this.searchQuery.set('');
    this.searchOpen.update(v => !v);
  }
  closeSearch() { this.searchOpen.set(false); }

  goTab(tab: string) {
    this.router.navigate(['/'], { queryParams: tab ? { tab } : {} });
  }
  goRoute(route: string) { this.router.navigate([route]); }

  runCommand(c: { tab?: string; route?: string }) {
    if (c.route) this.router.navigate([c.route]);
    else this.goTab(c.tab || '');
    this.closeSearch();
  }

  toggleSidebar() {
    this.sidebarCollapsed.update(v => !v);
    this.refreshCharts();
  }

  toggleChat() {
    this.chatOpen.update(v => !v);
    this.refreshCharts();
  }

  private refreshCharts() {
    setTimeout(() => {
      window.dispatchEvent(new Event('resize'));
    }, 300);
  }
}