import { Component, OnInit, inject, signal, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { DataService } from '../../services/data.service';

@Component({
  selector: 'app-third-party-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './third-party-detail.component.html',
  styleUrl: './third-party-detail.component.scss'
})
export class ThirdPartyDetailComponent implements OnInit {
  private dataService = inject(DataService);
  private router = inject(Router);

  // Identifiant du tiers (ex: TP-001) récupéré depuis l'URL
  id = input.required<string>();

  tier = signal<any>(null);
  policies = signal<any[]>([]);

  totalPremium = computed(() =>
    this.policies().reduce((sum, p) => sum + (p.annualPremium || 0), 0)
  );
  activeCount = computed(() =>
    this.policies().filter(p => p.status === 'ACTIVE').length
  );

  ngOnInit() {
    this.load(this.id());
  }

  load(identifier: string) {
    this.dataService.getThirdPartyById(identifier).subscribe(t => this.tier.set(t));
    this.dataService.getPoliciesByHolder(identifier).subscribe(p => this.policies.set(p));
  }

  initials(t: any): string {
    if (!t) return '';
    if (t.type === 'PHYSICAL_PERSON') {
      return ((t.name?.[0] || '') + (t.firstName?.[0] || '')).toUpperCase();
    }
    return (t.name || '').slice(0, 2).toUpperCase();
  }

  addressLine(t: any): string {
    if (!t) return '—';
    const parts = [t.street, t.postalCode, t.town, t.country].filter(Boolean);
    return parts.length ? parts.join(', ') : '—';
  }

  goBack() {
    this.router.navigate(['/'], { queryParams: { tab: 'third-parties' } });
  }

  openPolicy(p: any) {
    this.router.navigate(['/policies', p.policyNumber]);
  }
}
