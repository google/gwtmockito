package com.google.gwtmockito.fakes;

import com.google.gwtmockito.GwtMockito;

/**
 * Interface implemented by an object capable of providing fake instances of a
 * given type. Instances of this interface must be registered via
 * {@link GwtMockito#useProviderForType}.
 *
 * @param <T> type that this interface can provide
 * @author ekuefler@google.com (Erik Kuefler)
 */
public interface FakeProvider<T> {
  /**
   * Returns a fake implementation of the given type.
   *
   * @param type the actual type passed to GWT.create. Usually this is the same
   *             as T, but it could be a subtype or a completely unrelated type
   *             depending on how {@link GwtMockito#useProviderForType} was
   *             called.
   */
  T getFake(Class<?> type);
}
