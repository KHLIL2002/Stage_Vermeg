import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-bills',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bills.component.html',
  styleUrl: './bills.component.scss'
})
export class BillsComponent implements OnInit {
  private http = inject(HttpClient);

  // Sources de données
  bills = signal<any[]>([]);
  
  // États de filtrage
  statusFilter = signal<string>('');
  searchText = signal<string>('');

  // FILTRAGE AUTOMATIQUE (Computed)
  filteredBills = computed(() => {
    return this.bills().filter(b => {
      const s = this.statusFilter();
      const t = this.searchText().toLowerCase();
      
      const matchStatus = s ? b.status === s : true;
      const matchText = t ? (
        b.identifier.toLowerCase().includes(t) || 
        b.policyNumber.toLowerCase().includes(t)
      ) : true;
      
      return matchStatus && matchText;
    });
  });

  // STATISTIQUES FINANCIÈRES (Computed)
  totalUnpaid = computed(() => 
    this.filteredBills()
      .filter(b => b.status === 'UNPAID')
      .reduce((sum, b) => sum + (b.billAmount || 0), 0)
  );

  totalPending = computed(() => 
    this.filteredBills()
      .filter(b => b.status === 'PENDING')
      .reduce((sum, b) => sum + (b.billAmount || 0), 0)
  );

  ngOnInit() {
    this.http.get<any[]>('/api/bills').subscribe({
      next: (data) => this.bills.set(data)
    });
  }

  // Méthodes pour mettre à jour les signaux depuis le HTML
  onStatusChange(val: string) { this.statusFilter.set(val); }
  onTextChange(val: string) { this.searchText.set(val); }
}