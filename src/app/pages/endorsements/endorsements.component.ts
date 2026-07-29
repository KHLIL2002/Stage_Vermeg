import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';

@Component({
  selector: 'app-endorsements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './endorsements.component.html',
  styleUrl: './endorsements.component.scss'
})
export class EndorsementsComponent implements OnInit {
  private http = inject(HttpClient);

  // Données
  endorsements = signal<any[]>([]);
  
  // Filtres
  statusFilter = signal<string>('');
  searchText = signal<string>('');

  // FILTRAGE AUTOMATIQUE
  filteredEndorsements = computed(() => {
    return this.endorsements().filter(e => {
      const s = this.statusFilter();
      const t = this.searchText().toLowerCase();
      
      const matchStatus = s ? e.status === s : true;
      const matchText = t ? (
        e.identifier.toLowerCase().includes(t) || 
        e.policyNumber.toLowerCase().includes(t) ||
        e.endorsementType.toLowerCase().includes(t)
      ) : true;
      
      return matchStatus && matchText;
    });
  });

  // STATISTIQUES (Insights)
  pendingCount = computed(() => this.endorsements().filter(e => e.status === 'PENDING').length);
  totalVolume = computed(() => this.filteredEndorsements().reduce((sum, e) => sum + (e.grossAmount || 0), 0));

  ngOnInit() {
    this.http.get<any[]>('/api/endorsements').subscribe({
      next: (data) => this.endorsements.set(data)
    });
  }

  updateStatus(val: string) { this.statusFilter.set(val); }
  updateSearch(val: string) { this.searchText.set(val); }
}