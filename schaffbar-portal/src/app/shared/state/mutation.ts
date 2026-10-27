import { Signal, signal } from '@angular/core';

import { tapResponse } from '@ngrx/operators';
import { RxMethod, rxMethod } from '@ngrx/signals/rxjs-interop';
import { exhaustMap, finalize, Observable, pipe } from 'rxjs';

/**
 * Note: the callbacks must not throw – an error thrown inside `onSuccess` or `onError` is not caught and would end the
 * rxMethod (see {@link mutation}).
 */
interface MutationConfig<TCommand> {
  /** the backend call for the command */
  request: (command: TCommand) => Observable<unknown>;
  /** runs after a successful request, e.g. reloading the affected data */
  onSuccess: () => void;
  /** optional: runs after a failed request – the snackbar is shown by the error interceptor anyway */
  onError?: (error: unknown) => void;
}

/** A store method for a backend change, with its own pending state: `store.createTool.isPending()`. */
export type Mutation<TCommand> = RxMethod<TCommand> & {
  /** true while the request of this mutation is running */
  isPending: Signal<boolean>;
};

/**
 * Store method for a backend change (call it in `withMethods`):
 * - ignores further calls while the request is running (exhaustMap – protects against double clicks)
 * - tracks its own `isPending`, independent of other mutations of the store
 * - runs `onSuccess` after a successful request, the optional `onError` after a failed one
 *
 * Errors are shown by the error interceptor. They still have to be handled here: an error that reaches the outer
 * stream of the rxMethod ends it for good, and every later call would silently do nothing.
 */
export function mutation<TCommand>(config: MutationConfig<TCommand>): Mutation<TCommand> {
  const isPending = signal(false);

  const method = rxMethod<TCommand>(
    pipe(
      exhaustMap((command) => {
        isPending.set(true);
        return config.request(command).pipe(
          tapResponse({
            next: () => config.onSuccess(),
            // handled inside the request stream, so only this request ends – not the rxMethod
            error: (error) => config.onError?.(error),
          }),
          finalize(() => isPending.set(false)),
        );
      }),
    ),
  );

  return Object.assign(method, { isPending: isPending.asReadonly() });
}
