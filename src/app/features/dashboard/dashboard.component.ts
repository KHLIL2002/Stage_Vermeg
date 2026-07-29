import { Component, OnInit, AfterViewInit, OnDestroy, inject, signal, computed, effect, untracked, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DataService } from '../../services/data.service';
import { SearchSyncService } from '../../services/search-sync.service';
import { DropdownModule } from 'primeng/dropdown';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DropdownModule, TranslocoModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private dataService = inject(DataService);
  private syncService = inject(SearchSyncService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private transloco = inject(TranslocoService);

  // Suit la langue active pour recalculer les libellés traduits des menus
  langSig = signal(this.transloco.getActiveLang());
  private t(key: string): string { return this.transloco.translate(key); }

  private resizeObserver?: ResizeObserver;

  // Active Tab
  activeTab = signal<string>('overview');

  // Master Data Signals
  policies = signal<any[]>([]);
  thirdParties = signal<any[]>([]);
  bills = signal<any[]>([]);
  coverages = signal<any[]>([]);
  endorsements = signal<any[]>([]);
  products = signal<any[]>([]);

  // ====================================================
  // MODEL-SPECIFIC SEARCH & FILTER SIGNALS
  // ====================================================

  // Global / Overview Search
  overviewSearch = signal<string>('');

  // 1. POLICES MODEL SEARCH VARIABLES
  policySearch = signal<string>('');
  policyStatus = signal<string>('');
  policyProduct = signal<string>('');
  policyMinPremium = signal<number | null>(null);
  policyMaxPremium = signal<number | null>(null);
  policyPaymentMode = signal<string>('');
  policyYear = signal<string>('');

  // 2. TIERS MODEL SEARCH VARIABLES
  tpSearch = signal<string>('');
  tpType = signal<string>('');
  tpTown = signal<string>('');

  // 3. FACTURES MODEL SEARCH VARIABLES
  billSearch = signal<string>('');
  billStatus = signal<string>('');
  billMinAmount = signal<number | null>(null);
  billMaxAmount = signal<number | null>(null);
  billYear = signal<string>('');

  // 4. GARANTIES MODEL SEARCH VARIABLES
  coverageSearch = signal<string>('');
  coverageType = signal<string>('');
  coverageStatus = signal<string>('');
  coverageMinCapital = signal<number | null>(null);
  coverageMaxCapital = signal<number | null>(null);

  // 5. AVENANTS MODEL SEARCH VARIABLES
  endorsementSearch = signal<string>('');
  endorsementStatus = signal<string>('');
  endorsementType = signal<string>('');
  endorsementMinAmount = signal<number | null>(null);
  endorsementMaxAmount = signal<number | null>(null);

  // ====================================================
  // PAGINATION (50 lignes par page)
  // ====================================================
  readonly pageSize = 20;
  policyPage = signal(1);
  tpPage = signal(1);
  billPage = signal(1);
  coveragePage = signal(1);
  endorsementPage = signal(1);

  // ====================================================
  // OPTIONS DES FILTRES (p-dropdown)
  // ====================================================
  optStatusPolicy = computed(() => { this.langSig(); return [
    { label: this.t('filters.allStatuses'), value: '' },
    { label: this.t('status.active'), value: 'ACTIVE' },
    { label: this.t('status.cancelled'), value: 'CANCELLED' },
    { label: this.t('status.suspended'), value: 'SUSPENDED' },
    { label: this.t('status.pending'), value: 'PENDING' },
  ]; });
  optPaymentMode = computed(() => { this.langSig(); return [
    { label: this.t('filters.allModes'), value: '' },
    { label: this.t('pmode.annual'), value: 'ANNUAL' },
    { label: this.t('pmode.monthly'), value: 'MONTHLY' },
    { label: this.t('pmode.quarterly'), value: 'QUARTERLY' },
  ]; });
  readonly optYear = [
    { label: 'Toutes les années', value: '' },
    { label: '2026', value: '2026' },
    { label: '2025', value: '2025' },
    { label: '2024', value: '2024' },
  ];
  optTpType = computed(() => { this.langSig(); return [
    { label: this.t('filters.allTypes'), value: '' },
    { label: this.t('ttype.physical'), value: 'PHYSICAL_PERSON' },
    { label: this.t('ttype.legal'), value: 'LEGAL_ENTITY' },
  ]; });
  optStatusBill = computed(() => { this.langSig(); return [
    { label: this.t('filters.allStatuses'), value: '' },
    { label: this.t('status.paid'), value: 'PAID' },
    { label: this.t('status.unpaid'), value: 'UNPAID' },
    { label: this.t('status.pending'), value: 'PENDING' },
  ]; });
  optCoverageType = computed(() => { this.langSig(); return [
    { label: this.t('filters.allTypes'), value: '' },
    { label: this.t('covtype.death'), value: 'DEATH' },
    { label: this.t('covtype.savings'), value: 'SAVINGS' },
    { label: this.t('covtype.unitLinked'), value: 'UNIT_LINKED' },
    { label: this.t('covtype.disability'), value: 'DISABILITY' },
  ]; });
  optStatusCoverage = computed(() => { this.langSig(); return [
    { label: this.t('filters.allStatuses'), value: '' },
    { label: this.t('status.active'), value: 'ACTIVE' },
    { label: this.t('status.cancelled'), value: 'CANCELLED' },
  ]; });
  optStatusEndorsement = computed(() => { this.langSig(); return [
    { label: this.t('filters.allStatuses'), value: '' },
    { label: this.t('status.validated'), value: 'VALIDATED' },
    { label: this.t('status.pending'), value: 'PENDING' },
    { label: this.t('status.rejected'), value: 'REJECTED' },
  ]; });
  optEndorsementType = computed(() => { this.langSig(); return [
    { label: this.t('filters.allTypes'), value: '' },
    { label: this.t('endtype.clauseChange'), value: 'CLAUSE_CHANGE' },
    { label: this.t('endtype.premiumAdjustment'), value: 'PREMIUM_ADJUSTMENT' },
    { label: this.t('endtype.holderChange'), value: 'HOLDER_CHANGE' },
  ]; });
  productOptions = computed(() => { this.langSig(); return [
    { label: this.t('filters.allProducts'), value: '' },
    ...this.products().map(p => ({ label: p.name, value: p.productCode })),
  ]; });

  // Années présentes dans les données (dynamique — toutes les années possibles)
  policyYearOptions = computed(() => { this.langSig(); return this.yearsFrom(this.policies(), 'effectiveDate'); });
  billYearOptions = computed(() => { this.langSig(); return this.yearsFrom(this.bills(), 'effectiveDate'); });

  private yearsFrom(list: any[], field: string): { label: string; value: string }[] {
    const years = new Set<string>();
    for (const item of list) {
      const d = item?.[field];
      if (typeof d === 'string' && d.length >= 4) years.add(d.substring(0, 4));
    }
    const sorted = Array.from(years).sort((a, b) => b.localeCompare(a)); // décroissant
    return [{ label: this.t('filters.allYears'), value: '' }, ...sorted.map(y => ({ label: y, value: y }))];
  }

  // Reset Methods per Model
  resetPolicySearch() {
    this.policySearch.set('');
    this.policyStatus.set('');
    this.policyProduct.set('');
    this.policyMinPremium.set(null);
    this.policyMaxPremium.set(null);
    this.policyPaymentMode.set('');
    this.policyYear.set('');
  }

  resetTpSearch() {
    this.tpSearch.set('');
    this.tpType.set('');
    this.tpTown.set('');
  }

  resetBillSearch() {
    this.billSearch.set('');
    this.billStatus.set('');
    this.billMinAmount.set(null);
    this.billMaxAmount.set(null);
    this.billYear.set('');
  }

  resetCoverageSearch() {
    this.coverageSearch.set('');
    this.coverageType.set('');
    this.coverageStatus.set('');
    this.coverageMinCapital.set(null);
    this.coverageMaxCapital.set(null);
  }

  resetEndorsementSearch() {
    this.endorsementSearch.set('');
    this.endorsementStatus.set('');
    this.endorsementType.set('');
    this.endorsementMinAmount.set(null);
    this.endorsementMaxAmount.set(null);
  }

  // Active Filter Counters per Model
  policyFilterCount = computed(() => {
    let c = 0;
    if (this.policySearch().trim()) c++;
    if (this.policyStatus()) c++;
    if (this.policyProduct()) c++;
    if (this.policyMinPremium() !== null && this.policyMinPremium()! > 0) c++;
    if (this.policyMaxPremium() !== null && this.policyMaxPremium()! > 0) c++;
    if (this.policyPaymentMode()) c++;
    if (this.policyYear()) c++;
    return c;
  });

  tpFilterCount = computed(() => {
    let c = 0;
    if (this.tpSearch().trim()) c++;
    if (this.tpType()) c++;
    if (this.tpTown().trim()) c++;
    return c;
  });

  billFilterCount = computed(() => {
    let c = 0;
    if (this.billSearch().trim()) c++;
    if (this.billStatus()) c++;
    if (this.billMinAmount() !== null && this.billMinAmount()! > 0) c++;
    if (this.billMaxAmount() !== null && this.billMaxAmount()! > 0) c++;
    if (this.billYear()) c++;
    return c;
  });

  coverageFilterCount = computed(() => {
    let c = 0;
    if (this.coverageSearch().trim()) c++;
    if (this.coverageType()) c++;
    if (this.coverageStatus()) c++;
    if (this.coverageMinCapital() !== null && this.coverageMinCapital()! > 0) c++;
    if (this.coverageMaxCapital() !== null && this.coverageMaxCapital()! > 0) c++;
    return c;
  });

  endorsementFilterCount = computed(() => {
    let c = 0;
    if (this.endorsementSearch().trim()) c++;
    if (this.endorsementStatus()) c++;
    if (this.endorsementType()) c++;
    if (this.endorsementMinAmount() !== null && this.endorsementMinAmount()! > 0) c++;
    if (this.endorsementMaxAmount() !== null && this.endorsementMaxAmount()! > 0) c++;
    return c;
  });

  // Selected Policy Detail
  selectedPolicy = signal<any | null>(null);

  // Overview Metrics
  totalPremium = computed(() => this.policies().reduce((acc, p) => acc + (p.annualPremium || 0), 0));
  activeCount = computed(() => this.policies().filter(p => p.status === 'ACTIVE').length);
  avgPremium = computed(() => this.policies().length > 0 ? this.totalPremium() / this.policies().length : 0);

  totalUnpaid = computed(() =>
    this.bills()
      .filter(b => b.status === 'UNPAID')
      .reduce((sum, b) => sum + (b.billAmount || 0), 0)
  );

  physicalCount = computed(() => this.thirdParties().filter(t => t.type === 'PHYSICAL_PERSON').length);
  legalCount = computed(() => this.thirdParties().filter(t => t.type === 'LEGAL_ENTITY').length);

  // New Pro Metrics
  totalCapital = computed(() =>
    this.coverages()
      .filter(c => c.status === 'ACTIVE')
      .reduce((sum, c) => sum + (c.capital || 0), 0)
  );

  collectionRate = computed(() => {
    const totalBilled = this.bills().reduce((sum, b) => sum + (b.billAmount || 0), 0);
    const totalPaid = this.bills().reduce((sum, b) => sum + (b.paidAmount || 0), 0);
    return totalBilled > 0 ? (totalPaid / totalBilled) * 100 : 0;
  });

  totalEndorsementsVolume = computed(() =>
    this.endorsements()
      .filter(e => e.status === 'VALIDATED')
      .reduce((sum, e) => sum + (e.grossAmount || 0), 0)
  );

  // Recent Activity
  recentPolicies = computed(() => [...this.policies()].reverse().slice(0, 5));
  recentBills = computed(() => [...this.bills()].reverse().slice(0, 5));

  // ====================================================
  // MODEL 1: POLICES PROPER FILTERING
  // ====================================================
  filteredPolicies = computed(() => {
    const q = this.policySearch().toLowerCase().trim();
    const st = this.policyStatus();
    const prod = this.policyProduct();
    const min = this.policyMinPremium();
    const max = this.policyMaxPremium();
    const pay = this.policyPaymentMode();
    const yr = this.policyYear();

    return this.policies().filter(p => {
      if (st && p.status !== st) return false;
      if (prod && p.product?.productCode !== prod && p.product?.name !== prod) return false;
      if (min !== null && (p.annualPremium || 0) < min) return false;
      if (max !== null && (p.annualPremium || 0) > max) return false;
      if (pay && p.paymentMode !== pay) return false;
      if (yr && p.effectiveDate && !p.effectiveDate.startsWith(yr)) return false;

      if (q) {
        const matchNumber = p.policyNumber?.toLowerCase().includes(q);
        const matchHolderName = p.holder?.name?.toLowerCase().includes(q);
        const matchHolderFirstName = p.holder?.firstName?.toLowerCase().includes(q);
        const matchHolderEmail = p.holder?.email?.toLowerCase().includes(q);
        const matchProductName = p.product?.name?.toLowerCase().includes(q);
        if (!matchNumber && !matchHolderName && !matchHolderFirstName && !matchHolderEmail && !matchProductName) {
          return false;
        }
      }
      return true;
    }).sort((a, b) => (b.effectiveDate || '').localeCompare(a.effectiveDate || ''));
  });

  // ====================================================
  // MODEL 2: TIERS PROPER FILTERING
  // ====================================================
  filteredThirdParties = computed(() => {
    const q = this.tpSearch().toLowerCase().trim();
    const type = this.tpType();
    const town = this.tpTown().toLowerCase().trim();

    return this.thirdParties().filter(t => {
      if (type && t.type !== type) return false;
      if (town && !t.town?.toLowerCase().includes(town)) return false;

      if (q) {
        const matchId = t.identifier?.toLowerCase().includes(q);
        const matchName = t.name?.toLowerCase().includes(q);
        const matchFirstName = t.firstName?.toLowerCase().includes(q);
        const matchEmail = t.email?.toLowerCase().includes(q);
        const matchPhone = t.phone?.toLowerCase().includes(q);
        const matchNational = t.nationalIdentifier?.toLowerCase().includes(q);
        if (!matchId && !matchName && !matchFirstName && !matchEmail && !matchPhone && !matchNational) {
          return false;
        }
      }
      return true;
    }).sort((a, b) => (b.id || 0) - (a.id || 0));
  });

  // ====================================================
  // MODEL 3: FACTURES PROPER FILTERING
  // ====================================================
  filteredBills = computed(() => {
    const q = this.billSearch().toLowerCase().trim();
    const st = this.billStatus();
    const min = this.billMinAmount();
    const max = this.billMaxAmount();
    const yr = this.billYear();

    return this.bills().filter(b => {
      if (st && b.status !== st) return false;
      if (min !== null && (b.billAmount || 0) < min) return false;
      if (max !== null && (b.billAmount || 0) > max) return false;
      if (yr && b.effectiveDate && !b.effectiveDate.startsWith(yr)) return false;

      if (q) {
        const matchId = b.identifier?.toLowerCase().includes(q);
        const matchPolicy = b.policyNumber?.toLowerCase().includes(q);
        const matchCurrency = b.currency?.toLowerCase().includes(q);
        if (!matchId && !matchPolicy && !matchCurrency) return false;
      }
      return true;
    });
  });

  // ====================================================
  // MODEL 4: GARANTIES PROPER FILTERING
  // ====================================================
  filteredCoverages = computed(() => {
    const q = this.coverageSearch().toLowerCase().trim();
    const type = this.coverageType();
    const st = this.coverageStatus();
    const min = this.coverageMinCapital();
    const max = this.coverageMaxCapital();

    return this.coverages().filter(c => {
      if (type && c.coverageType !== type) return false;
      if (st && c.status !== st) return false;
      if (min !== null && (c.capital || 0) < min) return false;
      if (max !== null && (c.capital || 0) > max) return false;

      if (q) {
        const matchId = c.identifier?.toLowerCase().includes(q);
        const matchLabel = c.label?.toLowerCase().includes(q);
        const matchPolicy = c.policyNumber?.toLowerCase().includes(q);
        if (!matchId && !matchLabel && !matchPolicy) return false;
      }
      return true;
    });
  });

  // ====================================================
  // MODEL 5: AVENANTS PROPER FILTERING
  // ====================================================
  filteredEndorsements = computed(() => {
    const q = this.endorsementSearch().toLowerCase().trim();
    const st = this.endorsementStatus();
    const type = this.endorsementType();
    const min = this.endorsementMinAmount();
    const max = this.endorsementMaxAmount();

    return this.endorsements().filter(e => {
      if (st && e.status !== st) return false;
      if (type && e.endorsementType !== type) return false;
      if (min !== null && (e.grossAmount || 0) < min) return false;
      if (max !== null && (e.grossAmount || 0) > max) return false;

      if (q) {
        const matchId = e.identifier?.toLowerCase().includes(q);
        const matchPolicy = e.policyNumber?.toLowerCase().includes(q);
        const matchType = e.endorsementType?.toLowerCase().includes(q);
        const matchSubType = e.endorsementSubType?.toLowerCase().includes(q);
        if (!matchId && !matchPolicy && !matchType && !matchSubType) return false;
      }
      return true;
    });
  });

  // ── Pagination : tranches de 50 + nombre de pages ───────────────
  policyTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredPolicies().length / this.pageSize)));
  pagedPolicies = computed(() => {
    const start = (this.policyPage() - 1) * this.pageSize;
    return this.filteredPolicies().slice(start, start + this.pageSize);
  });

  tpTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredThirdParties().length / this.pageSize)));
  pagedThirdParties = computed(() => {
    const start = (this.tpPage() - 1) * this.pageSize;
    return this.filteredThirdParties().slice(start, start + this.pageSize);
  });

  billTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredBills().length / this.pageSize)));
  pagedBills = computed(() => {
    const start = (this.billPage() - 1) * this.pageSize;
    return this.filteredBills().slice(start, start + this.pageSize);
  });

  coverageTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredCoverages().length / this.pageSize)));
  pagedCoverages = computed(() => {
    const start = (this.coveragePage() - 1) * this.pageSize;
    return this.filteredCoverages().slice(start, start + this.pageSize);
  });

  endorsementTotalPages = computed(() => Math.max(1, Math.ceil(this.filteredEndorsements().length / this.pageSize)));
  pagedEndorsements = computed(() => {
    const start = (this.endorsementPage() - 1) * this.pageSize;
    return this.filteredEndorsements().slice(start, start + this.pageSize);
  });

  @ViewChild('statusChart') statusChartCanvas?: ElementRef;
  @ViewChild('productChart') productChartCanvas?: ElementRef;
  charts: Chart[] = [];

  constructor() {
    this.transloco.langChanges$.subscribe(l => this.langSig.set(l));

    effect(() => {
      if (this.policies().length >= 0 && this.activeTab() === 'overview') {
        setTimeout(() => this.updateCharts(), 50);
      }
    });

    // Revenir à une page valide quand un filtre réduit la liste
    effect(() => { const t = this.policyTotalPages(); untracked(() => { if (this.policyPage() > t) this.policyPage.set(1); }); });
    effect(() => { const t = this.tpTotalPages(); untracked(() => { if (this.tpPage() > t) this.tpPage.set(1); }); });
    effect(() => { const t = this.billTotalPages(); untracked(() => { if (this.billPage() > t) this.billPage.set(1); }); });
    effect(() => { const t = this.coverageTotalPages(); untracked(() => { if (this.coveragePage() > t) this.coveragePage.set(1); }); });
    effect(() => { const t = this.endorsementTotalPages(); untracked(() => { if (this.endorsementPage() > t) this.endorsementPage.set(1); }); });

    effect(() => {
      const cmd = this.syncService.currentFilter();
      // 'target' = onglet ciblé par l'agent. Par défaut (non défini) → policies.
      const target = cmd.target ?? 'policies';

      // Statut : appliqué uniquement au modèle concerné.
      if (cmd.status !== undefined) {
        if (target === 'policies') this.policyStatus.set(cmd.status);
        else if (target === 'bills') this.billStatus.set(cmd.status);
      }

      // Texte de recherche : appliqué UNIQUEMENT à l'onglet ciblé
      // (fini la propagation à Polices + Tiers + Factures en même temps).
      if (cmd.text !== undefined) {
        switch (target) {
          case 'third-parties': this.tpSearch.set(cmd.text); break;
          case 'bills':         this.billSearch.set(cmd.text); break;
          case 'coverages':     this.coverageSearch.set(cmd.text); break;
          case 'endorsements':  this.endorsementSearch.set(cmd.text); break;
          case 'policies':
          default:              this.policySearch.set(cmd.text); break;
        }
      }
    });
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.activeTab.set(params['tab']);
      }
    });

    this.loadAllData();
  }

  loadAllData() {
    this.dataService.getPolicies().subscribe(d => this.policies.set(d));
    this.dataService.getThirdParties().subscribe(d => this.thirdParties.set(d));
    this.dataService.getBills().subscribe(d => this.bills.set(d));
    this.dataService.getCoverages().subscribe(d => this.coverages.set(d));
    this.dataService.getEndorsements().subscribe(d => this.endorsements.set(d));
    this.dataService.getProducts().subscribe(d => this.products.set(d));
  }

  setTab(tab: string) {
    this.activeTab.set(tab);
    if (tab === 'overview') {
      setTimeout(() => {
        this.initCharts();
        this.updateCharts();
      }, 100);
    }
  }

  ngAfterViewInit() {
    if (this.activeTab() === 'overview') {
      this.initCharts();
    }
    const target = document.querySelector('.page-content');
    if (target) {
      this.resizeObserver = new ResizeObserver(() => this.charts.forEach(c => c.resize()));
      this.resizeObserver.observe(target);
    }
  }

  initCharts() {
    if (!this.statusChartCanvas || !this.productChartCanvas) return;
    this.charts.forEach(c => c.destroy());
    this.charts = [];

    const baseOpts: any = { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } };

    this.charts.push(new Chart(this.statusChartCanvas.nativeElement, {
      type: 'doughnut',
      data: { datasets: [] },
      options: { ...baseOpts, plugins: { legend: { display: true, position: 'bottom', labels: { boxWidth: 12, padding: 14, font: { size: 11 } } } } }
    }));

    this.charts.push(new Chart(this.productChartCanvas.nativeElement, {
      type: 'bar',
      data: { labels: [], datasets: [] },
      options: { ...baseOpts, scales: { x: { grid: { display: false } }, y: { grid: { color: '#f1f5f9' }, border: { display: false } } } }
    }));
  }

  updateCharts() {
    if (this.charts.length < 2) return;
    const policies = this.policies();

    const statuses = ['ACTIVE', 'CANCELLED', 'SUSPENDED', 'PENDING'];
    const counts = statuses.map(s => policies.filter(p => p.status === s).length);
    this.charts[0].data = {
      labels: ['Actif', 'Annulé', 'Suspendu', 'En attente'],
      datasets: [{ data: counts, backgroundColor: ['#10b981', '#ef4444', '#f59e0b', '#94a3b8'], borderWidth: 0, hoverOffset: 6 }]
    };
    this.charts[0].update();

    const prodCounts: any = {};
    policies.forEach(p => { const n = p.product?.name || 'Autre'; prodCounts[n] = (prodCounts[n] || 0) + 1; });
    const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#14b8a6', '#f97316'];
    this.charts[1].data = {
      labels: Object.keys(prodCounts),
      datasets: [{ data: Object.values(prodCounts), backgroundColor: colors, borderRadius: 8, borderSkipped: false }]
    };
    this.charts[1].update();
  }

  selectPolicy(policy: any) {
    this.router.navigate(['/policies', policy.policyNumber]);
  }

  selectTier(tp: any) {
    this.router.navigate(['/third-parties', tp.identifier]);
  }

  /**
   * Construit la liste des numéros de page à afficher, avec des « … » quand
   * il y a beaucoup de pages (ex: 1 … 4 5 6 … 10).
   */
  pageList(current: number, total: number): (number | string)[] {
    const delta = 2;
    const range: number[] = [];
    for (let i = Math.max(1, current - delta); i <= Math.min(total, current + delta); i++) {
      range.push(i);
    }
    const pages: (number | string)[] = [];
    if (range[0] > 1) {
      pages.push(1);
      if (range[0] > 2) pages.push('…');
    }
    pages.push(...range);
    const last = range[range.length - 1];
    if (last < total) {
      if (last < total - 1) pages.push('…');
      pages.push(total);
    }
    return pages;
  }

  goToSubscription() {
    this.router.navigate(['/subscription']);
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
    this.charts.forEach(c => c.destroy());
  }
}