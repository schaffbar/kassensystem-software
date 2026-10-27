import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { config, exhaustMap, Observable, pipe, Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { Mutation, mutation } from './mutation';

describe('mutation', () => {
  // every call of the request returns a new Subject, so the test decides when it succeeds or fails
  let responses: Subject<void>[];
  let request: Mock<(command: string) => Observable<void>>;
  let onSuccess: Mock<() => void>;
  let createItem: Mutation<string>;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
    responses = [];
    request = vi.fn(() => {
      const response = new Subject<void>();
      responses.push(response);
      return response;
    });
    onSuccess = vi.fn();
    createItem = TestBed.runInInjectionContext(() => mutation<string>({ request, onSuccess }));
  });

  function succeed(index: number): void {
    responses[index].next();
    responses[index].complete();
  }

  it('should be pending while the request is running and call onSuccess afterwards', () => {
    expect(createItem.isPending()).toBe(false);

    createItem('A');
    expect(request).toHaveBeenCalledExactlyOnceWith('A');
    expect(createItem.isPending()).toBe(true);

    succeed(0);
    expect(createItem.isPending()).toBe(false);
    expect(onSuccess).toHaveBeenCalledOnce();
  });

  it('should ignore further calls while a request is running (double click)', () => {
    createItem('A');
    createItem('A');

    expect(request).toHaveBeenCalledOnce();
  });

  it('should keep working after a failed request', () => {
    createItem('A');
    responses[0].error(new Error('500'));

    expect(createItem.isPending()).toBe(false);
    expect(onSuccess).not.toHaveBeenCalled();

    createItem('B');
    expect(request).toHaveBeenLastCalledWith('B');
    expect(request).toHaveBeenCalledTimes(2);
  });

  it('illustrates why the error has to be handled: without it the rxMethod stops after the first error', async () => {
    const withoutErrorHandling = TestBed.runInInjectionContext(() =>
      rxMethod<string>(pipe(exhaustMap((command) => request(command)))),
    );

    withoutErrorHandling('A');
    // the error reaches the outer stream of the rxMethod and ends it – RxJS reports it (asynchronously) as unhandled
    const unhandledErrors: unknown[] = [];
    config.onUnhandledError = (error) => unhandledErrors.push(error);
    responses[0].error(new Error('500'));
    await new Promise((resolve) => setTimeout(resolve));
    config.onUnhandledError = null;
    expect(unhandledErrors).toHaveLength(1);

    withoutErrorHandling('B');
    expect(request).toHaveBeenCalledOnce(); // 'B' never reaches the backend
  });
});
