import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../services/data.service';

@Component({
  selector: 'app-third-parties',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './third-parties.component.html',
  styleUrl: './third-parties.component.scss'
})
export class ThirdPartiesComponent implements OnInit {
  private dataService = inject(DataService);

  // Sources de données
  thirdParties = signal<any[]>([]);
  
  // États de filtrage (Signals)
  searchText = signal<string>('');
  typeFilter = signal<string>('');

  // FILTRAGE AUTOMATIQUE
  filteredThirdParties = computed(() => {
    return this.thirdParties().filter(t => {
      const s = this.searchText().toLowerCase();
      const type = this.typeFilter();

      const matchText = s ? (
        t.name?.toLowerCase().includes(s) || 
        t.firstName?.toLowerCase().includes(s) || 
        t.town?.toLowerCase().includes(s) ||
        t.identifier?.toLowerCase().includes(s)
      ) : true;

      const matchType = type ? t.type === type : true;

      return matchText && matchType;
    });
  });

  // STATISTIQUES (Insights)
  physicalCount = computed(() => this.thirdParties().filter(t => t.type === 'PHYSICAL_PERSON').length);
  legalCount = computed(() => this.thirdParties().filter(t => t.type === 'LEGAL_ENTITY').length);

  ngOnInit() {
    this.dataService.getThirdParties().subscribe({
      next: (data) => this.thirdParties.set(data)
    });
  }

  // Mise à jour des filtres
  updateSearch(val: string) { this.searchText.set(val); }
  updateType(val: string) { this.typeFilter.set(val); }
}