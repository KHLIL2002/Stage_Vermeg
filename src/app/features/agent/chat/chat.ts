import { Component, signal, computed, inject, ElementRef, ViewChild, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ChatService } from '../chat.service';
import { Message } from '../chat.model';
import { MarkdownModule } from 'ngx-markdown';
import { DropdownModule } from 'primeng/dropdown';
import { SearchSyncService } from '../../../services/search-sync.service';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownModule, DropdownModule],
  templateUrl: './chat.html',
  styleUrl: './chat.scss'
})
export class ChatComponent implements OnInit {
  private chatService = inject(ChatService);
  private syncService = inject(SearchSyncService);
  private router = inject(Router);

  messages = signal<Message[]>([]);
  loading = signal(false);
  providers = signal<string[]>([]);
  providerOptions = computed(() => this.providers().map(p => ({ label: p.toUpperCase(), value: p })));
  selectedProvider = 'groq';
  userInput = '';

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  ngOnInit() {
    this.chatService.getProviders().subscribe({
      next: (list) => this.providers.set(list),
      error: () => this.providers.set(['groq'])
    });
  }

  sendMessage(): void {
    const text = this.userInput.trim();
    if (!text || this.loading()) return;

    this.messages.update((msgs) => [...msgs, { role: 'user', content: text }]);
    this.userInput = '';
    this.loading.set(true);
    this.scrollToBottom();

    this.chatService.send(this.messages(), this.selectedProvider).subscribe({
      next: (res: any) => {
        this.messages.update((msgs) => [...msgs, { role: 'assistant', content: res.response }]);

        const isCreationSuccess = res.response.includes('SUCCESS');

        if (res.command) {
          console.log("🚀 Commande IA reçue :", res.command.action);

          // --- LOGIQUE DE NAVIGATION CORRIGÉE ---
          
          if (res.command.action === 'OPEN_POLICY_DETAIL') {
    const pn = res.command.params.policyNumber;
    console.log("🚀 Navigation forcée vers le détail :", pn);
    // On force la route /policies/001-001
    this.router.navigate(['/policies', pn]);
}

          else {
            // Navigation classique vers la liste (Polices, Factures, etc.)
            this.router.navigate([`/${res.command.targetPage}`]);
            
            // Mise à jour des filtres (Recherche/Statut) via le service,
            // en ciblant UNIQUEMENT la page concernée (targetPage) pour éviter
            // que le filtre se propage à tous les onglets.
            this.syncService.update(
              res.command.params.status,
              res.command.params.policyNumber,
              res.command.targetPage
            );
          }

          // Si c'est une création ou un rafraîchissement demandé
          if (isCreationSuccess || res.command.action === 'REFRESH_AND_FILTER') {
            this.syncService.triggerRefresh();
          }
        }

        this.loading.set(false);
        this.scrollToBottom();
      },
      error: () => {
        this.messages.update((msgs) => [...msgs, { role: 'assistant', content: 'Erreur serveur.' }]);
        this.loading.set(false);
        this.scrollToBottom();
      }
    });
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    }, 50);
  }
}