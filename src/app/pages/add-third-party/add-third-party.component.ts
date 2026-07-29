import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { DataService } from '../../services/data.service';
import { Router } from '@angular/router';
import { SearchSyncService } from '../../services/search-sync.service';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';

@Component({
  selector: 'app-add-third-party',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CalendarModule, DropdownModule],
  templateUrl: './add-third-party.component.html',
  styleUrl: './add-third-party.component.scss'
})
export class AddThirdPartyComponent {
  private fb      = inject(FormBuilder);
  private dataSvc = inject(DataService);
  private syncSvc = inject(SearchSyncService);
  private router  = inject(Router);

  // ── State ────────────────────────────────────────────────────────────────
  loading = signal(false);
  success = signal(false);
  currentStep = signal(0);
  today = new Date();

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

  readonly steps = [
    { label: 'Type & Identité',  icon: '👥' },
    { label: 'Informations',     icon: '📋' },
    { label: 'Coordonnées',      icon: '📍' },
    { label: 'Récapitulatif',    icon: '✅' },
  ];

  // ── Step 0: Type & Identité ────────────────────────────────────────────────
  stepIdentity: FormGroup = this.fb.group({
    type:                      ['PHYSICAL_PERSON', Validators.required],
    name:                      ['', Validators.required],
    nationalIdentifier:        ['', Validators.required],
    nationalIdentifierCountry: ['FR'],
  });

  // ── Step 1: Personne Physique / Morale ────────────────────────────────────
  stepDetails: FormGroup = this.fb.group({
    // Physical Person fields
    firstName:             [''],
    secondName:            [''],
    thirdName:             [''],
    birthDate:             [''],
    gender:                ['MALE'],
    // Legal Entity fields
    legalForm:             ['SA'],
    tradeRegister:         [''],
    creationDate:          [''],
    economicActivityCode:  [''],
  });

  // ── Step 2: Coordonnées & Adresse ─────────────────────────────────────────
  stepContact: FormGroup = this.fb.group({
    street:           ['', Validators.required],
    streetNumber:     [''],
    postalCode:       ['', Validators.required],
    town:             ['', Validators.required],
    country:          ['FR', Validators.required],
    appartmentNumber: [''],
    email:            ['', [Validators.required, Validators.email]],
    phone:            ['', Validators.required],
  });

  constructor() {
    // Le prénom est obligatoire uniquement pour une personne physique.
    const firstName = this.stepDetails.get('firstName');
    const toggleFirstName = (type: string) => {
      if (type === 'PHYSICAL_PERSON') {
        firstName?.setValidators([Validators.required]);
      } else {
        firstName?.clearValidators();
      }
      firstName?.updateValueAndValidity();
    };
    toggleFirstName(this.stepIdentity.get('type')?.value);
    this.stepIdentity.get('type')?.valueChanges.subscribe(toggleFirstName);
  }

  // ── Computed Helpers ──────────────────────────────────────────────────────
  get isPhysicalPerson(): boolean {
    return this.stepIdentity.get('type')?.value === 'PHYSICAL_PERSON';
  }

  get activeFormGroup(): FormGroup {
    return [this.stepIdentity, this.stepDetails, this.stepContact][this.currentStep()] ?? this.stepIdentity;
  }

  // ── Options Lists ─────────────────────────────────────────────────────────
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

  readonly legalForms = [
    { value: 'SA',    label: 'SA — Société Anonyme' },
    { value: 'SAS',   label: 'SAS — Société par Actions Simplifiée' },
    { value: 'SARL',  label: 'SARL — Société à Responsabilité Limitée' },
    { value: 'EURL',  label: 'EURL — Entreprise Unipersonnelle à Resp. Lim.' },
    { value: 'SNC',   label: 'SNC — Société en Nom Collectif' },
    { value: 'SCI',   label: 'SCI — Société Civile Immobilière' },
    { value: 'OTHER', label: 'Autre forme juridique' },
  ];

  readonly genders = [
    { value: 'MALE',   label: 'Homme' },
    { value: 'FEMALE', label: 'Femme' },
  ];

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
    [this.stepIdentity, this.stepDetails, this.stepContact].forEach(fg => fg.markAllAsTouched());
    if (this.stepIdentity.invalid || this.stepDetails.invalid || this.stepContact.invalid) return;

    this.loading.set(true);

    const isPhysical = this.isPhysicalPerson;

    // Payload PLAT, aligné sur l'entité ThirdParty du backend
    // (sinon ville / email / téléphone arrivent vides en base).
    const payload: any = {
      type:                      this.stepIdentity.value.type,
      name:                      this.stepIdentity.value.name,
      nationalIdentifier:        this.stepIdentity.value.nationalIdentifier || null,
      nationalIdentifierCountry: this.stepIdentity.value.nationalIdentifierCountry || null,

      // Coordonnées & adresse (champs plats attendus par le backend)
      email:      this.stepContact.value.email,
      phone:      this.stepContact.value.phone,
      street:     this.stepContact.value.streetNumber
                    ? `${this.stepContact.value.streetNumber} ${this.stepContact.value.street}`
                    : this.stepContact.value.street,
      postalCode: this.stepContact.value.postalCode,
      town:       this.stepContact.value.town,
      country:    this.stepContact.value.country,
    };

    if (isPhysical) {
      payload.firstName  = this.stepDetails.value.firstName || null;
      payload.secondName = this.stepDetails.value.secondName || null;
      payload.birthDate  = this.toIsoDate(this.stepDetails.value.birthDate);
      payload.gender     = this.stepDetails.value.gender || 'MALE';
    } else {
      payload.legalForm     = this.stepDetails.value.legalForm || null;
      payload.tradeRegister = this.stepDetails.value.tradeRegister || null;
    }

    this.dataSvc.createThirdParty(payload).subscribe({
      next: () => {
        this.syncSvc.triggerRefresh();
        this.success.set(true);
        setTimeout(() => this.router.navigate(['/'], { queryParams: { tab: 'third-parties' } }), 2500);
      },
      error: (err) => {
        console.error('Erreur création tiers', err);
        this.loading.set(false);
      }
    });
  }
}
