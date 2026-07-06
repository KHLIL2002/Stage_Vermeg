import { Component } from '@angular/core';
import { ChatComponent } from './features/agent/chat/chat';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ChatComponent],
  template: '<app-chat />'
})
export class AppComponent {}