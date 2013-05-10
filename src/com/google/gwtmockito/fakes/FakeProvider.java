package com.google.gwtmockito.fakes;

import com.google.gwtmockito.GwtMockito;

/**
 * Interface implemented by an object capable of providing fake instances of a
 * given type or its subtypes. Instances of this interface must be registered
 * via {@link GwtMockito#useProviderForType}.
 *
 * @param <T> type that this interface can provide
 * @author ekuefler@google.com (Erik Kuefler)
 */
public interface FakeProvider<T> {
  /**
   * Returns a fake implementation of the given type.
   *
   * @param type the actual type passed to GWT.create, which might be a subtype
   *             of T. This allows general categories of types like UiBinders to
   *             be faked with a single provider. If you don't expect GWT.create
   *             to be called with any subtypes then there is no need to check
   *             this parameter.
   */
  T getFake(Class<? extends T> type);
}
