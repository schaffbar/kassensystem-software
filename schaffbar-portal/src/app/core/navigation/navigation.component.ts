import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterOutlet } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';

import { TranslatePipe } from '@ngx-translate/core';
import { map } from 'rxjs';

export interface MenuItem {
  icon: string;
  name: string;
  route: string;
}

@Component({
  selector: 'schbar-navigation',
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss',
  imports: [
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    TranslatePipe,
  ],
})
export class NavigationComponent {
  private readonly breakpointObserver = inject(BreakpointObserver);

  protected readonly menuItems = signal<MenuItem[]>([
    { icon: 'home', name: 'sidebar.dashboard', route: 'dashboard' },
    { icon: 'people', name: 'sidebar.users', route: 'users' },
    { icon: 'construction', name: 'sidebar.tools', route: 'tools' },
    { icon: 'memory', name: 'sidebar.rfidTags', route: 'rfid-tags' },
    { icon: 'developer_board', name: 'sidebar.rfidReaders', route: 'rfid-readers' },
  ]);

  protected readonly isHandset = toSignal(
    this.breakpointObserver.observe(Breakpoints.Handset).pipe(map((result) => result.matches)),
    { initialValue: false },
  );
}
