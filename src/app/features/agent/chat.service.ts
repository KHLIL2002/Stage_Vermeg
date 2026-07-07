import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChatService {

  private http = inject(HttpClient);

  send(message: string, provider: string): Observable<{ response: string, provider: string }> {
    return this.http.post<{ response: string, provider: string }>('/api/agent', { message, provider });
  }

  getProviders(): Observable<string[]> {
    return this.http.get<string[]>('/api/providers');
  }
}