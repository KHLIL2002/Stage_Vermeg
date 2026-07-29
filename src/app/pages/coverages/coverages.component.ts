import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-coverages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './coverages.component.html',
  styleUrl: './coverages.component.scss'
})
export class CoveragesComponent implements OnInit {
  private http = inject(HttpClient);

  // Données
  coverages = signal<any[]>([]);
  
  // Filtres
  searchText = signal<string>('');
  typeFilter = signal<string>('');

  // FILTRAGE AUTOMATIQUE
  filteredCoverages = computed(() => {
    return this.coverages().filter(c => {
      const t = this.searchText().toLowerCase();
      const type = this.typeFilter();

      const matchText = t ? (
        c.label.toLowerCase().includes(t) || 
        c.identifier.toLowerCase().includes(t) ||
        c.policyNumber?.toLowerCase().includes(t)
      ) : true;

      const matchType = type ? c.coverageType === type : true;

      return matchText && matchType;
    });
  });

  // STATS RÉCAPITULATIVES
  totalCapital = computed(() => 
    this.filteredCoverages().reduce((sum, c) => sum + (c.capital || 0), 0)
  );

  avgPremium = computed(() => {
    const filtered = this.filteredCoverages();
    return filtered.length > 0 
      ? filtered.reduce((sum, c) => sum + (c.premium || 0), 0) / filtered.length 
      : 0;
  });

  ngOnInit() {
    this.http.get<any[]>('/api/coverages').subscribe({
      next: (data) => this.coverages.set(data)
    });
  }

  updateSearch(val: string) { this.searchText.set(val); }
  updateType(val: string) { this.typeFilter.set(val); }
}