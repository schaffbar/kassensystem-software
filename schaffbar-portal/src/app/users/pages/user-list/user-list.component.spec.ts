import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideZonelessChangeDetection, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { User } from '../../shared/models/user.model';
import { UsersStore } from '../../shared/stores/users.store';
import { UserListComponent } from './user-list.component';

describe('UserListComponent', () => {
  const users = [
    { id: 'user-1', firstName: 'Max', lastName: 'Mustermann' },
    { id: 'user-2', firstName: 'Erika', lastName: 'Musterfrau' },
    { id: 'user-3', firstName: 'Otto', lastName: 'Normalverbraucher' },
  ] as User[];

  let fixture: ComponentFixture<UserListComponent>;
  let loader: HarnessLoader;
  let entities: WritableSignal<User[]>;

  beforeEach(async () => {
    entities = signal(users);

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideTranslateService(),
        {
          provide: UsersStore,
          useValue: { entities, loadAllUsers: vi.fn(), createUser: vi.fn(), deleteUser: vi.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
  });

  async function search(term: string): Promise<void> {
    await (await loader.getHarness(MatInputHarness.with({ selector: '#search' }))).setValue(term);
    await fixture.whenStable();
  }

  async function lastNames(): Promise<string[]> {
    const table = await loader.getHarness(MatTableHarness);
    const rows = await table.getCellTextByColumnName();
    return rows['lastName'].text;
  }

  it('should show all users without a search term', async () => {
    expect(await lastNames()).toEqual(['Mustermann', 'Musterfrau', 'Normalverbraucher']);
  });

  it('should filter the users by the search term (case-insensitive)', async () => {
    await search('MUSTER');

    expect(await lastNames()).toEqual(['Mustermann', 'Musterfrau']);
  });

  it('should keep the filter when the users are reloaded', async () => {
    await search('otto');

    entities.set([...users, { id: 'user-4', firstName: 'Otto', lastName: 'Waalkes' } as User]);
    await fixture.whenStable();

    expect(await lastNames()).toEqual(['Normalverbraucher', 'Waalkes']);
  });

  it('should clear the search with the clear button', async () => {
    await search('max');

    const clear = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Clear search"]' }));
    await clear.click();
    await fixture.whenStable();

    expect(await (await loader.getHarness(MatInputHarness.with({ selector: '#search' }))).getValue()).toBe('');
    expect(await lastNames()).toHaveLength(3);
    expect(
      await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Clear search"]' })),
    ).toBeNull();
  });
});
