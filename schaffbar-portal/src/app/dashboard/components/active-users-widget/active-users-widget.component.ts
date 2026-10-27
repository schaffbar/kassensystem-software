import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';

import { TranslatePipe } from '@ngx-translate/core';
import { differenceInMinutes, parseISO } from 'date-fns';
import { interval, map } from 'rxjs';

import { ActiveUser } from '../../dashboard.model';

const DURATION_REFRESH_MS = 60_000;

interface ActiveUserRow extends ActiveUser {
  duration: string;
}

@Component({
  selector: 'schbar-active-users-widget',
  templateUrl: './active-users-widget.component.html',
  styleUrl: './active-users-widget.component.scss',
  imports: [MatButtonModule, MatCardModule, MatTableModule, MatIconModule, DatePipe, TranslatePipe],
})
export class ActiveUsersWidgetComponent {
  readonly activeUsers = input.required<ActiveUser[]>();

  readonly reload = output<void>();
  readonly userSelected = output<ActiveUser>();

  protected readonly displayedColumns = ['name', 'entryTime', 'duration'];
  protected readonly activeUsersCount = computed(() => this.activeUsers().length);

  // ticks every minute, so the duration column stays up to date (the interval ends with the component)
  private readonly now = toSignal(interval(DURATION_REFRESH_MS).pipe(map(() => new Date())), {
    initialValue: new Date(),
  });

  protected readonly dataSource = computed(() => {
    const now = this.now();
    const rows: ActiveUserRow[] = this.activeUsers().map((user) => ({
      ...user,
      duration: formatDuration(user.entryTime, now),
    }));
    return new MatTableDataSource<ActiveUserRow>(rows);
  });

  protected reloadActiveUsers(): void {
    this.reload.emit();
  }

  protected selectUser(user: ActiveUser): void {
    this.userSelected.emit(user);
  }
}

function formatDuration(entryTime: string, now: Date): string {
  const totalMinutes = differenceInMinutes(now, parseISO(entryTime));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
}
