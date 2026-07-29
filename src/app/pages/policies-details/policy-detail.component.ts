import { Component, OnInit, inject, signal, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { DataService } from '../../services/data.service';

@Component({
  selector: 'app-policy-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './policy-detail.component.html',
  styleUrl: './policy-detail.component.scss'
})
export class PolicyDetailComponent implements OnInit {
  private dataService = inject(DataService);
  private router = inject(Router);

  // Angular 18 récupère l'ID du contrat directement depuis l'URL
  id = input.required<string>(); 

  policy = signal<any>(null);
  roles = signal<any[]>([]);
  coverages = signal<any[]>([]);
  bills = signal<any[]>([]);

  ngOnInit() {
    console.log('ID récupéré depuis l\'URL :', this.id());
    this.loadAllData(this.id());
  }

  loadAllData(num: string) {
    // On charge tout en parallèle
    this.dataService.getPolicyByNumber(num).subscribe(p => this.policy.set(p));
    this.dataService.getPolicyRoles(num).subscribe(r => this.roles.set(r));
    this.dataService.getCoverages(num).subscribe(c => this.coverages.set(c));
    this.dataService.getPolicyBills(num).subscribe(b => this.bills.set(b));
  }

  goBack() {
    this.router.navigate(['/'], { queryParams: { tab: 'policies' } });
  }

  onEdit() {
    // Placeholder : pas encore de page d'édition de contrat.
  }

  fmtDate(d: string): string {
    if (!d) return 'N/A';
    const date = new Date(d);
    return isNaN(date.getTime())
      ? d
      : date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  fmtMoney0(n: number): string {
    return (n ?? 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 });
  }

  fmtMoney2(n: number): string {
    return (n ?? 0).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  statusFr(s: string): string {
    return ({ ACTIVE: 'Actif', CANCELLED: 'Annulé', SUSPENDED: 'Suspendu', PENDING: 'En attente' } as any)[s] || s;
  }

  payModeFr(m: string): string {
    return ({
      TRANSFER: 'Virement',
      BANK_TRANSFER: 'Virement bancaire',
      DIRECT_DEBIT: 'Prélèvement automatique',
      CHEQUE: 'Chèque',
      CASH: 'Espèces',
    } as any)[m] || m || 'N/A';
  }

  roleFr(r: string): string {
    return ({ HOLDER: 'SOUSCRIPTEUR', BENEFICIARY: 'BÉNÉFICIAIRE', LIFE_ASSURED: 'ASSURÉ', PAYER: 'PAYEUR', BROKER: 'COURTIER' } as any)[r] || r;
  }

  covTypeFr(t: string): string {
    return ({ DEATH: 'DÉCÈS', SAVINGS: 'ÉPARGNE', UNIT_LINKED: 'UC', DISABILITY: 'INVALIDITÉ', CAPITALIZATION: 'CAPITALISATION', RETIREMENT: 'RETRAITE', PROTECTION: 'PRÉVOYANCE' } as any)[t] || t;
  }

  billFr(s: string): string {
    return ({ PAID: 'Payée', UNPAID: 'Impayée', PENDING: 'En attente' } as any)[s] || s;
  }

  badgeCls(s: string): string {
    if (['ACTIVE', 'PAID', 'VALIDATED'].includes(s)) return 'g';
    if (['UNPAID', 'CANCELLED', 'REJECTED'].includes(s)) return 'r';
    if (s === 'SUSPENDED') return 'a';
    return 'b';
  }
}