import { Component, signal, inject, ElementRef, ViewChild, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ChatService } from '../chat.service';
import { Message } from '../chat.model';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrl: './chat.scss'
})
export class ChatComponent implements OnInit {

  private chatService = inject(ChatService);

  messages = signal<Message[]>([]);
  loading = signal(false);
  providers = signal<string[]>([]);
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

    this.messages.update(msgs => [...msgs, { role: 'user', content: text }]);
    this.userInput = '';
    this.loading.set(true);
    this.scrollToBottom();

    this.chatService.send(text, this.selectedProvider).subscribe({
      next: (res) => {
        this.messages.update(msgs => [...msgs, { role: 'assistant', content: res.response }]);
        this.loading.set(false);
        this.scrollToBottom();
      },
      error: () => {
        this.messages.update(msgs => [...msgs, {
          role: 'assistant',
          content: 'Erreur : impossible de contacter le serveur.'
        }]);
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