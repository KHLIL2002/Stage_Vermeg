import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { DataService } from '../../services/data.service';
import { Router } from '@angular/router';
import { SearchSyncService } from '../../services/search-sync.service';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, CalendarModule, DropdownModule, TableModule, TranslocoModule],
  templateUrl: './subscription.component.html',
  styleUrl: './subscription.component.scss'
})
export class SubscriptionComponent implements OnInit {
  private fb     = inject(FormBuilder);
  private dataSvc = inject(DataService);
  private syncSvc = inject(SearchSyncService);
  private router  = inject(Router);
  private transloco = inject(TranslocoService);

  langSig = signal(this.transloco.getActiveLang());
  private t(key: string): string { return this.transloco.translate(key); }

  constructor() {
    this.transloco.langChanges$.subscribe(l => this.langSig.set(l));
  }

  // ── State ────────────────────────────────────────────────────────────────
  loading = signal(false);
  success = signal(false);
  currentStep = signal(0);

  /** Convertit une date du p-calendar (Date) en chaîne YYYY-MM-DD (sans décalage de fuseau). */
  private toIsoDate(d: any): string | null {
    if (!d) return null;
    const date = d instanceof Date ? d : new Date(d);
    if (isNaN(date.getTime())) return null;
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  steps = computed(() => { this.langSig(); return [
    { label: this.t('sub.stProduct'), icon: '🛡️' },
    { label: this.t('sub.stHolder'),  icon: '👤' },
    { label: this.t('sub.stPremium'), icon: '💰' },
    { label: this.t('sub.stSummary'), icon: '✅' },
  ]; });

  // ── Products & Tiers Data ────────────────────────────────────────────────
  products = signal<any[]>([]);
  thirdParties = signal<any[]>([]);
  selectedTier = signal<any | null>(null);

  // ── Tiers Picker Modal & Search State ─────────────────────────────────────
  showTierModal = signal(false);
  tierSearch = signal('');
  tierTypeFilter = signal('');
  tierSortBy = signal<'name' | 'identifier' | 'town'>('name');

  // ── Computed Filtered & Sorted Tiers for Modal ────────────────────────────
  filteredModalThirdParties = computed(() => {
    let list = this.thirdParties();
    const search = this.tierSearch().toLowerCase().trim();
    const type = this.tierTypeFilter();

    if (type) {
      list = list.filter(tp => tp.type === type);
    }

    if (search) {
      list = list.filter(tp =>
        (tp.identifier && tp.identifier.toLowerCase().includes(search)) ||
        (tp.name && tp.name.toLowerCase().includes(search)) ||
        (tp.firstName && tp.firstName.toLowerCase().includes(search)) ||
        (tp.email && tp.email.toLowerCase().includes(search)) ||
        (tp.town && tp.town.toLowerCase().includes(search)) ||
        (tp.nationalIdentifier && tp.nationalIdentifier.toLowerCase().includes(search))
      );
    }

    const sortBy = this.tierSortBy();
    return list.slice().sort((a, b) => {
      if (sortBy === 'identifier') {
        return (a.identifier || '').localeCompare(b.identifier || '');
      } else if (sortBy === 'town') {
        return (a.town || '').localeCompare(b.town || '');
      } else {
        const nameA = `${a.name || ''} ${a.firstName || ''}`;
        const nameB = `${b.name || ''} ${b.firstName || ''}`;
        return nameA.localeCompare(nameB);
      }
    });
  });

  // ── Product Picker State & Computed ──────────────────────────────────────
  showProductModal = signal(false);
  productSearch = signal('');

  selectedProduct = computed(() => {
    const code = this.stepProduct.value.productCode;
    return this.products().find(p => p.productCode === code) || null;
  });

  filteredModalProducts = computed(() => {
    let list = this.products();
    const search = this.productSearch().toLowerCase().trim();
    if (search) {
      list = list.filter(p =>
        (p.productCode && p.productCode.toLowerCase().includes(search)) ||
        (p.name && p.name.toLowerCase().includes(search))
      );
    }
    return list;
  });

  selectProduct(prod: any): void {
    this.stepProduct.patchValue({ productCode: prod.productCode });
    this.showProductModal.set(false);
  }

  openProductModal(): void {
    this.showProductModal.set(true);
  }

  closeProductModal(): void {
    this.showProductModal.set(false);
  }

  getProductIcon(productCode: string = '', name: string = ''): string {
    const str = (productCode + ' ' + name).toLowerCase();
    if (str.includes('vie') || str.includes('life') || str.includes('vita')) return '💎';
    if (str.includes('epargne') || str.includes('invest') || str.includes('retraite')) return '📈';
    if (str.includes('sante') || str.includes('health') || str.includes('prevoyance')) return '🏥';
    if (str.includes('auto') || str.includes('car')) return '🚗';
    if (str.includes('hab') || str.includes('home')) return '🏠';
    return '🛡️';
  }

  getProductCategory(productCode: string = '', name: string = ''): string {
    const str = (productCode + ' ' + name).toLowerCase();
    if (str.includes('vie') || str.includes('life') || str.includes('vita')) return 'Assurance Vie & Épargne';
    if (str.includes('epargne') || str.includes('invest')) return 'Capitalisation';
    if (str.includes('retraite')) return 'Plan Épargne Retraite';
    if (str.includes('sante') || str.includes('prevoyance')) return 'Santé & Prévoyance';
    return 'Garantie Multirisque';
  }

  // ── Step 1: Produit ───────────────────────────────────────────────────────
  stepProduct: FormGroup = this.fb.group({
    productCode:          ['', Validators.required],
    commercialProductId:  [''],
    commercialPackageId:  [''],
    effectiveDate:        ['', Validators.required],
    termDate:             [''],
    currency:             ['EUR', Validators.required],
    taxCountry:           ['FR'],
  });

  // ── Step 2: Titulaire ─────────────────────────────────────────────────────
  stepHolder: FormGroup = this.fb.group({
    holderId:             ['', [Validators.required, Validators.pattern('TP-\\d{3}')]],
    isTransfer:           [false],
    sourcePolicyNumber:   [''],
  });

  // ── Step 3: Prime & Paiement ──────────────────────────────────────────────
  stepPremium: FormGroup = this.fb.group({
    premium:              [null, [Validators.required, Validators.min(100)]],
    premiumType:          ['UNIQUE', Validators.required],
    paymentMode:          ['DIRECT_DEBIT'],
    paymentPeriodicity:   ['ANNUAL'],
    strategyType:         ['FREE'],
    taxFrameworkId:       [''],
  });

  // ── Computed helpers ──────────────────────────────────────────────────────
  get isTransfer(): boolean {
    return !!this.stepHolder.get('isTransfer')?.value;
  }

  get activeFormGroup(): FormGroup {
    return [this.stepProduct, this.stepHolder, this.stepPremium][this.currentStep()] ?? this.stepProduct;
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.dataSvc.getProducts().subscribe({
      next:  (list) => this.products.set(list),
      error: (err)  => console.error('Erreur chargement produits', err),
    });

    this.dataSvc.getThirdParties().subscribe({
      next: (list) => {
        this.thirdParties.set(list);
        const currentHolderId = this.stepHolder.value.holderId;
        if (currentHolderId) {
          const found = list.find(t => t.identifier === currentHolderId);
          if (found) this.selectedTier.set(found);
        }
      },
      error: (err) => console.error('Erreur chargement tiers', err),
    });
  }

  // ── Tier Selection Methods ────────────────────────────────────────────────
  openTierModal(): void {
    this.showTierModal.set(true);
  }

  closeTierModal(): void {
    this.showTierModal.set(false);
  }

  selectTier(tp: any): void {
    this.stepHolder.patchValue({ holderId: tp.identifier });
    this.selectedTier.set(tp);
    this.showTierModal.set(false);
  }

  clearSelectedTier(): void {
    this.stepHolder.patchValue({ holderId: '' });
    this.selectedTier.set(null);
  }

  onSelectDropdownChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    if (val) {
      const found = this.thirdParties().find(t => t.identifier === val);
      if (found) {
        this.selectTier(found);
      } else {
        this.stepHolder.patchValue({ holderId: val });
        this.selectedTier.set(null);
      }
    } else {
      this.clearSelectedTier();
    }
  }

  onHolderInputBlur(): void {
    const val = this.stepHolder.value.holderId;
    if (val) {
      const found = this.thirdParties().find(t => t.identifier.toLowerCase() === val.toLowerCase());
      if (found) {
        this.selectedTier.set(found);
        this.stepHolder.patchValue({ holderId: found.identifier });
      }
    }
  }

  // ── Navigation ────────────────────────────────────────────────────────────
  goNext(): void {
    const fg = this.activeFormGroup;
    fg.markAllAsTouched();
    if (fg.invalid) return;
    if (this.currentStep() < this.steps.length - 1) {
      this.currentStep.update(s => s + 1);
    }
  }

  goBack(): void {
    if (this.currentStep() > 0) {
      this.currentStep.update(s => s - 1);
    }
  }

  goToStep(i: number): void {
    if (i < this.currentStep()) {
      this.currentStep.set(i);
    }
  }

  goToDashboard(): void {
    this.router.navigate(['/']);
  }

  // ── Submit ────────────────────────────────────────────────────────────────
  onSubmit(): void {
    [this.stepProduct, this.stepHolder, this.stepPremium].forEach(fg => fg.markAllAsTouched());
    if (this.stepProduct.invalid || this.stepHolder.invalid || this.stepPremium.invalid) return;

    this.loading.set(true);

    const payload = {
      productCode:          this.stepProduct.value.productCode,
      commercialProductId:  this.stepProduct.value.commercialProductId || null,
      commercialPackageId:  this.stepProduct.value.commercialPackageId || null,
      effectiveDate:        this.toIsoDate(this.stepProduct.value.effectiveDate),
      termDate:             this.toIsoDate(this.stepProduct.value.termDate),
      currency:             this.stepProduct.value.currency,
      taxCountry:           this.stepProduct.value.taxCountry || null,
      holderId:             this.stepHolder.value.holderId,
      isTransfer:           this.stepHolder.value.isTransfer,
      sourcePolicyNumber:   this.stepHolder.value.isTransfer ? this.stepHolder.value.sourcePolicyNumber : null,
      premium:              this.stepPremium.value.premium,
      premiumType:          this.stepPremium.value.premiumType,
      paymentMode:          this.stepPremium.value.paymentMode   || null,
      paymentPeriodicity:   this.stepPremium.value.paymentPeriodicity || null,
      strategyType:         this.stepPremium.value.strategyType  || null,
      taxFrameworkId:       this.stepPremium.value.taxFrameworkId || null,
    };

    this.dataSvc.createPolicy(payload).subscribe({
      next: () => {
        this.syncSvc.triggerRefresh();
        this.success.set(true);
        setTimeout(() => this.router.navigate(['/'], { queryParams: { tab: 'policies' } }), 2500);
      },
      error: () => this.loading.set(false),
    });
  }

  // ── Helpers for summary display ───────────────────────────────────────────
  getProductName(): string {
    const code = this.stepProduct.value.productCode;
    const prod = this.products().find(p => p.productCode === code);
    return prod ? prod.name : code;
  }

  modalTypeOptions = computed(() => { this.langSig(); return [
    { label: this.t('sub.allTypes'), value: '' },
    { label: this.t('sub.individuals'), value: 'PHYSICAL_PERSON' },
    { label: this.t('sub.companies'), value: 'LEGAL_ENTITY' },
  ]; });
  modalSortOptions = computed(() => { this.langSig(); return [
    { label: this.t('sub.sortName'), value: 'name' },
    { label: this.t('sub.sortId'), value: 'identifier' },
    { label: this.t('sub.sortTown'), value: 'town' },
  ]; });

  readonly currencies  = ['EUR', 'USD', 'GBP', 'CHF', 'TND', 'MAD'];
  premiumTypes = computed(() => { this.langSig(); return [
    { value: 'UNIQUE',  label: this.t('sub.ptUnique') },
    { value: 'REGULAR', label: this.t('sub.ptRegular') },
  ]; });
  paymentModes = computed(() => { this.langSig(); return [
    { value: 'DIRECT_DEBIT',  label: this.t('sub.pmDirectDebit') },
    { value: 'BANK_TRANSFER', label: this.t('sub.pmBankTransfer') },
    { value: 'CHEQUE',        label: this.t('sub.pmCheque') },
    { value: 'CASH',          label: this.t('sub.pmCash') },
  ]; });
  periodicities = computed(() => { this.langSig(); return [
    { value: 'ANNUAL',   label: this.t('sub.perAnnual') },
    { value: 'SEMESTER', label: this.t('sub.perSemester') },
    { value: 'QUARTER',  label: this.t('sub.perQuarter') },
    { value: 'MONTHLY',  label: this.t('sub.perMonthly') },
  ]; });
  strategyTypes = computed(() => { this.langSig(); return [
    { value: 'FREE',    label: this.t('sub.stFree') },
    { value: 'GUIDED',  label: this.t('sub.stGuided') },
    { value: 'MANAGED', label: this.t('sub.stManaged') },
    { value: 'PROFILE', label: this.t('sub.stProfile') },
  ]; });
  getPremiumTypeLabel(value: string): string {
    return this.premiumTypes().find(p => p.value === value)?.label ?? value;
  }

  readonly countries = [
    { value: 'FR', label: '🇫🇷 France' },
    { value: 'BE', label: '🇧🇪 Belgique' },
    { value: 'LU', label: '🇱🇺 Luxembourg' },
    { value: 'TN', label: '🇹🇳 Tunisie' },
    { value: 'MA', label: '🇲🇦 Maroc' },
    { value: 'GB', label: '🇬🇧 Royaume-Uni' },
    { value: 'DE', label: '🇩🇪 Allemagne' },
    { value: 'CH', label: '🇨🇭 Suisse' },
    { value: 'IT', label: '🇮🇹 Italie' },
    { value: 'ES', label: '🇪🇸 Espagne' },
  ];
}