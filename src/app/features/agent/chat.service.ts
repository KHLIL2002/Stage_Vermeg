import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChatService {

  private http = inject(HttpClient);

  /**
   * Envoie le message à Spring Boot, reçoit la réponse texte.
   * Pas de streaming pour l'instant — juste un POST classique.
   */
  send(message: string): Observable<{ response: string }> {
    return this.http.post<{ response: string }>('/api/agent', { message });
  }
}