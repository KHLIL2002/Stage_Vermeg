import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../services/data.service';
import { SearchSyncService } from '../../services/search-sync.service';
import { Router } from '@angular/router';
import { DropdownModule } from 'primeng/dropdown';
@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [CommonModule, FormsModule, DropdownModule],
  templateUrl: './policies.component.html',
  styleUrl: './policies.component.scss'
})
export class PoliciesComponent implements OnInit {
  private dataService = inject(DataService);
  private syncService = inject(SearchSyncService);
  private router = inject(Router); // <--- 2. AJOUTER CETTE LIGNE

  policies = signal<any[]>([]);
  searchText = signal<string>('');
  statusFilter = signal<string>('');

  selectedPolicy = signal<any | null>(null);
  roles = signal<any[]>([]);
  coverages = signal<any[]>([]);
  bills = signal<any[]>([]);

  filtered = computed(() => {
    const all = this.policies();
    const s = this.searchText().toLowerCase();
    const status = this.statusFilter();

    return all.filter(p => {
      const matchStatus = status ? p.status === status : true;
      const matchText = s ? (
        p.policyNumber.toLowerCase().includes(s) ||
        p.holder?.name?.toLowerCase().includes(s) ||
        p.product?.name?.toLowerCase().includes(s)
      ) : true;
      return matchStatus && matchText;
    });
  });

  // Dans le constructor de PoliciesComponent
constructor() {
  // ÉCOUTEUR 1 : RAFRAÎCHISSEMENT HTTP
  effect(() => {
    const trigger = this.syncService.refreshTrigger();
    if (trigger > 0) {
      console.log("PoliciesComponent: Refresh trigger detected. Loading data...");
      this.loadData(); // Appelle l'API GET /api/policies
    }
  });

  effect(() => {
  const cmd = this.syncService.currentFilter();
  
  // Si l'IA envoie un statut, on met à jour. Sinon on ne touche à rien ou on vide.
  if (cmd.status !== undefined) {
    this.statusFilter.set(cmd.status);
  } else {
    this.statusFilter.set(''); // Reset si l'IA ne demande pas de statut précis
  }

  // Si l'IA envoie un numéro, on met à jour. Sinon on vide.
  if (cmd.text !== undefined) {
    this.searchText.set(cmd.text);
  } else {
    this.searchText.set(''); // C'est ici que le "600" sera effacé !
  }
});
}

loadData() {
    this.dataService.getPolicies().subscribe({
      next: (data) => {
        this.policies.set(data);
        console.log("✅ Policies: " + data.length + " polices chargées.");
      }
    });
  }
  ngOnInit() {
    this.loadData();
  }

  

  onTextChange(val: string) { this.searchText.set(val); }
  onStatusChange(val: string) { this.statusFilter.set(val); }

 selectPolicy(policy: any) {
  // Au lieu d'ouvrir le panneau, on change de page
  this.router.navigate(['/policies', policy.policyNumber]);
}

  clearSelection() {
    this.selectedPolicy.set(null);
    this.roles.set([]);
    this.coverages.set([]);
    this.bills.set([]);
  }

  // --- NOUVELLE MÉTHODE DE NAVIGATION ---
  goToSubscription() {
    console.log("Navigation vers le formulaire de souscription...");
    this.router.navigate(['/subscription']);
  }

}