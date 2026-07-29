import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SearchSyncService {
  // 'target' = l'onglet/modèle concerné par le filtre (policies, third-parties,
  // bills, coverages, endorsements). Permet de n'appliquer la recherche qu'au bon modèle.
  currentFilter = signal<{status?: string, text?: string, target?: string}>({});

  // Nouveau signal pour forcer le rafraîchissement
  refreshTrigger = signal<number>(0);

  update(status?: string, text?: string, target?: string) {
    console.log("🔄 SyncService : Mise à jour du filtre pour la cible :", target);
    this.currentFilter.set({ status, text, target });
  }

  triggerRefresh() {
    console.log("🔔 SyncService : Signal de rafraîchissement envoyé");
    this.refreshTrigger.update(n => n + 1);
  }

}