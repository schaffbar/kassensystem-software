import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';

import { TranslatePipe } from '@ngx-translate/core';
import { map } from 'rxjs';

import { ROUTE } from '../../app.routes';

export interface MenuGroup {
  // translation key
  name: string;
  items: MenuItem[];
}

export interface MenuItem {
  icon: string;
  // translation key
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
    RouterLinkActive,
    MatButtonModule,
    MatSidenavModule,
    MatIconModule,
    TranslatePipe,
  ],
})
export class NavigationComponent {
  private readonly breakpointObserver = inject(BreakpointObserver);

  protected readonly menuGroups: MenuGroup[] = [
    {
      name: 'sidebar.groups.operations',
      items: [
        { icon: 'dashboard', name: 'sidebar.dashboard', route: ROUTE.DASHBOARD },
        { icon: 'group', name: 'sidebar.users', route: ROUTE.USERS },
        { icon: 'build', name: 'sidebar.tools', route: ROUTE.TOOLS },
      ],
    },
    {
      name: 'sidebar.groups.hardware',
      items: [
        { icon: 'sell', name: 'sidebar.rfidTags', route: ROUTE.RFID_TAGS },
        { icon: 'sensors', name: 'sidebar.rfidReaders', route: ROUTE.RFID_READERS },
      ],
    },
  ];

  protected readonly isHandset = toSignal(
    this.breakpointObserver.observe(Breakpoints.Handset).pipe(map((result) => result.matches)),
    { initialValue: false },
  );

  // --------------------------------------------------------------------------
  // actions

  protected closeMenuOnHandset(drawer: MatSidenav): void {
    if (this.isHandset()) {
      void drawer.close();
    }
  }
}
