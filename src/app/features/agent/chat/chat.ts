import { Component, signal, inject, ElementRef, ViewChild } from '@angular/core';
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
export class ChatComponent {

  private chatService = inject(ChatService);

  messages = signal<Message[]>([]);
  loading = signal(false);
  userInput = '';

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  sendMessage(): void {
    const text = this.userInput.trim();
    if (!text || this.loading()) return;

    this.messages.update(msgs => [...msgs, { role: 'user', content: text }]);
    this.userInput = '';
    this.loading.set(true);
    this.scrollToBottom();

    this.chatService.send(text).subscribe({
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