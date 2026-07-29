import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Message } from './chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);

  // On envoie l'historique (Message[]) et on reçoit un objet complexe (any)
  send(messages: Message[], provider: string): Observable<any> {
    return this.http.post<any>('/api/agent', { messages, provider });
  }

  getProviders(): Observable<string[]> {
    return this.http.get<string[]>('/api/providers');
  }
}