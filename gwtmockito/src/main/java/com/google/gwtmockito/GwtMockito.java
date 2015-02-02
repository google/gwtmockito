/*
 * Copyright 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwtmockito;

import static org.mockito.Mockito.mock;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.constants.NumberConstantsImpl;
import com.google.gwt.i18n.client.impl.LocaleInfoImpl;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwtmockito.fakes.FakeClientBundleProvider;
import com.google.gwtmockito.fakes.FakeLocaleInfoImplProvider;
import com.google.gwtmockito.fakes.FakeMessagesProvider;
import com.google.gwtmockito.fakes.FakeNumberConstantsImplProvider;
import com.google.gwtmockito.fakes.FakeProvider;
import com.google.gwtmockito.fakes.FakeUiBinderProvider;
import com.google.gwtmockito.impl.ReturnsCustomMocks;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A library to make Mockito-based testing of GWT applications easier. Most
 * users won't have to reference this class directly and should instead use
 * {@link GwtMockitoTestRunner}. Users who cannot use that class (e.g. tests
 * using JUnit3) can invoke {@link #initMocks} directly in their setUp and
 * {@link #tearDown} in their tearDown methods.
 * <p>
 * Note that calling {@link #initMocks} and {@link #tearDown} directly does
 * <i>not</i> implement {@link GwtMockitoTestRunner}'s behavior of implementing
 * native methods and making final methods mockable. The only way to get this
 * behavior is by using {@link GwtMockitoTestRunner}.
 * <p>
 * Once {@link #initMocks} has been invoked, test code can safely call
 * GWT.create without exceptions. Doing so will return either a mock object
 * registered with {@link GwtMock}, a fake object specified by a call to
 * {@link #useProviderForType}, or a new mock instance if no other binding
 * exists. Fakes for types extending the following are provided by default:
 * <ul>
 *   <li> UiBinder: uses a fake that populates all UiFields with GWT.create'd
 *        widgets, allowing them to be mocked like other calls to GWT.create.
 *        See {@link FakeUiBinderProvider} for details.
 *   <li> ClientBundle: Uses a fake that will return fake CssResources as
 *        defined below, and will return fake versions of other resources that
 *        return unique strings for getText and getSafeUri. See
 *        {@link FakeClientBundleProvider} for details.
 *   <li> Messages, CssResource, and SafeHtmlTemplates: uses a fake that
 *        implements each method by returning a String of SafeHtml based on the
 *        name of the method and any arguments passed to it. The exact format is
 *        undefined. See {@link FakeMessagesProvider} for details.
 * </ul>
 * <p>
 * The type returned from GWT.create will generally be the same as the type
 * passed in. The exception is when GWT.create'ing a subclass of
 * {@link RemoteService} - in this case, the result of GWT.create will be the
 * Async version of that interface as defined by gwt-rpc.
 * <p>
 * If {@link #initMocks} is called manually, it is important to invoke
 * {@link #tearDown} once the test has been completed. Failure to do so can
 * cause state to leak between tests.
 *
 * @see GwtMockitoTestRunner
 * @see GwtMock
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class GwtMockito {

  private static final Map<Class<?>, FakeProvider<?>> DEFAULT_FAKE_PROVIDERS =
      new HashMap<Class<?>, FakeProvider<?>>();
  static {
    DEFAULT_FAKE_PROVIDERS.put(ClientBundle.class, new FakeClientBundleProvider());
    DEFAULT_FAKE_PROVIDERS.put(CssResource.class, new FakeMessagesProvider<CssResource>());
    DEFAULT_FAKE_PROVIDERS.put(LocaleInfoImpl.class, new FakeLocaleInfoImplProvider());
    DEFAULT_FAKE_PROVIDERS.put(Messages.class, new FakeMessagesProvider<Messages>());
    DEFAULT_FAKE_PROVIDERS.put(NumberConstantsImpl.class, new FakeNumberConstantsImplProvider());
    DEFAULT_FAKE_PROVIDERS.put(
        SafeHtmlTemplates.class, new FakeMessagesProvider<SafeHtmlTemplates>());
    DEFAULT_FAKE_PROVIDERS.put(UiBinder.class, new FakeUiBinderProvider());
  }

  private static Bridge bridge;

  /**
   * Causes all calls to GWT.create to be intercepted to return a mock or fake
   * object, and populates any {@link GwtMock}-annotated fields with mockito
   * mocks. This method should be usually be called during the setUp method of a
   * test case. Note that it explicitly calls
   * {@link MockitoAnnotations#initMocks}, so there is no need to call that
   * method separately. See the class description for more details.
   *
   * @param owner class to scan for {@link GwtMock}-annotated fields - almost
   *              always "this" in unit tests
   */
  public static void initMocks(Object owner) {
    // Create a new bridge and register built-in type providers
    bridge = new Bridge();
    for (Entry<Class<?>, FakeProvider<?>> entry : DEFAULT_FAKE_PROVIDERS.entrySet()) {
      useProviderForType(entry.getKey(), entry.getValue());
    }

    // Install the bridge and populate mock fields
    boolean success = false;
    try {
      setGwtBridge(bridge);
      registerGwtMocks(owner);
      MockitoAnnotations.initMocks(owner);
      success = true;
    } finally {
      if (!success) {
        tearDown();
      }
    }
  }

  /**
   * Resets GWT.create to its default behavior. This method should be called
   * after any test that called initMocks completes, usually in your test's
   * tearDown method. Failure to do so can introduce unexpected ordering
   * dependencies in tests.
   */
  public static void tearDown() {
    setGwtBridge(null);
  }

  /**
   * Specifies that the given provider should be used to GWT.create instances of
   * the given type and its subclasses. If multiple providers could produce a
   * given class (for example, if a provide is registered for a type and its
   * supertype), the provider for the more specific type is chosen. An exception
   * is thrown if this type is ambiguous. Note that if you just want to return a
   * Mockito mock from GWT.create, it's probably easier to use {@link GwtMock}
   * instead.
   */
  public static void useProviderForType(Class<?> type, FakeProvider<?> provider) {
    if (bridge == null) {
      throw new IllegalStateException("Must call initMocks() before calling useProviderForType()");
    }
    if (bridge.registeredMocks.containsKey(type)) {
      throw new IllegalArgumentException(
          "Can't use a provider for a type that already has a @GwtMock declared");
    }
    bridge.registeredProviders.put(type, provider);
  }

  /**
   * Returns a new fake object of the given type assuming a fake provider is
   * available for that type. Additional fake providers can be registered via
   * {@link #useProviderForType}.
   *
   * @param type type to get a fake object for
   * @return a fake of the given type, as returned by an applicable provider
   * @throws IllegalArgumentException if no provider for the given type (or one
   *                                  of its superclasses) has been registered
   */
  public static <T> T getFake(Class<T> type) {
    // If initMocks hasn't been called, read from the default fake provider map. This allows static
    // fields to be initialized with fakes in tests that don't use the GwtMockito test runner.
    T fake = getFakeFromProviderMap(
        type,
        bridge != null ? bridge.registeredProviders : DEFAULT_FAKE_PROVIDERS);
    if (fake == null) {
      throw new IllegalArgumentException("No fake provider has been registered "
          + "for " + type.getSimpleName() + ". Call useProviderForType to "
          + "register a provider before calling getFake.");
    }
    return fake;
  }

  private static void registerGwtMocks(Object owner) {
    Class<? extends Object> clazz = owner.getClass();

    while (!"java.lang.Object".equals(clazz.getName())) {
      for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(GwtMock.class)) {
          Object mock = Mockito.mock(field.getType());
          if (bridge.registeredMocks.containsKey(field.getType())) {
            throw new IllegalArgumentException("Owner declares multiple @GwtMocks for type "
                + field.getType().getSimpleName() + "; only one is allowed. Did you mean to "
                + "use a standard @Mock?");
          }
          bridge.registeredMocks.put(field.getType(), mock);
          field.setAccessible(true);
          try {
            field.set(owner, mock);
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to make field accessible: " + field);
          }
        }
      }

      clazz = clazz.getSuperclass();
    }
  }

  private static void setGwtBridge(GWTBridge bridge) {
    try {
      Method setBridge = GWT.class.getDeclaredMethod("setBridge", GWTBridge.class);
      setBridge.setAccessible(true);
      setBridge.invoke(null, bridge);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (IllegalAccessException e) {
      throw new AssertionError("Impossible since setBridge was made accessible");
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Impossible since setBridge is known to exist");
    }
  }

  private static <T> T getFakeFromProviderMap(Class<T> type, Map<Class<?>, FakeProvider<?>> map) {
      // See if we have any providers for this type or its supertypes.
      Map<Class<?>, FakeProvider<?>> legalProviders = new HashMap<Class<?>, FakeProvider<?>>();
      for (Entry<Class<?>, FakeProvider<?>> entry : map.entrySet()) {
        if (entry.getKey().isAssignableFrom(type)) {
          legalProviders.put(entry.getKey(), entry.getValue());
        }
      }

      // Filter the set of legal providers to the most specific type.
      Map<Class<?>, FakeProvider<?>> filteredProviders = new HashMap<Class<?>, FakeProvider<?>>();
      for (Entry<Class<?>, FakeProvider<?>> candidate : legalProviders.entrySet()) {
        boolean isSpecific = true;
        for (Entry<Class<?>, FakeProvider<?>> other : legalProviders.entrySet()) {
          if (candidate != other && candidate.getKey().isAssignableFrom(other.getKey())) {
            isSpecific = false;
            break;
          }
        }
        if (isSpecific) {
          filteredProviders.put(candidate.getKey(), candidate.getValue());
        }
      }

      // If exactly one provider remains, use it.
      if (filteredProviders.size() == 1) {
        // We know this is safe since we checked that the types are assignable
        @SuppressWarnings({"rawtypes", "cast"})
        Class rawType = (Class) type;
        return (T) filteredProviders.values().iterator().next().getFake(rawType);
      } else if (filteredProviders.isEmpty()) {
        return null;
      } else {
        throw new IllegalArgumentException("Can't decide which provider to use for " +
            type.getSimpleName() +
            ", it could be provided as any of the following: " +
            mapToSimpleNames(filteredProviders.keySet()) +
            ". Add a provider for " +
            type.getSimpleName() +
            " to resolve this ambiguity.");
      }
  }

    private static Set<String> mapToSimpleNames(Set<Class<?>> classes) {
      Set<String> simpleNames = new HashSet<String>();
      for (Class<?> clazz : classes) {
        simpleNames.add(clazz.getSimpleName());
      }
      return simpleNames;
    }

  private static class Bridge extends GWTBridge {
    private final Map<Class<?>, FakeProvider<?>> registeredProviders =
        new HashMap<Class<?>, FakeProvider<?>>();
    private final Map<Class<?>, Object> registeredMocks = new HashMap<Class<?>, Object>();

    @Override
    @SuppressWarnings("unchecked") // safe since we check whether the type is assignable
    public <T> T create(Class<?> createdType) {
      // If we're creating a RemoteService, assume that the result of GWT.create is being assigned
      // to the async version of that service. Otherwise, assume it's being assigned to the same
      // type we're creating.
      Class<?> assignedType = RemoteService.class.isAssignableFrom(createdType)
          ? getAsyncType((Class<? extends RemoteService>) createdType)
          : createdType;

      // First check if we have a GwtMock for this exact being assigned to and use it if so.
      if (registeredMocks.containsKey(assignedType)) {
        return (T) registeredMocks.get(assignedType);
      }

      // Next check if we have a fake provider that can provide a fake for the type being created.
      T fake = (T) getFakeFromProviderMap(createdType, registeredProviders);
      if (fake != null) {
        return fake;
      }

      // If nothing has been registered, just return a new mock for the type being assigned.
      return (T) mock(assignedType, new ReturnsCustomMocks());
    }

    @Override
    public String getVersion() {
      return getClass().getName();
    }

    @Override
    public boolean isClient() {
      return false;
    }

    @Override
    public void log(String message, Throwable e) {
      System.err.println(message + "\n");
      if (e != null) {
        e.printStackTrace();
      }
    }

    /** Returns the corresponding async service type for the given remote service type. */
    private Class<?> getAsyncType(Class<? extends RemoteService> type) {
      Class<?> asyncType;
      try {
        asyncType = Class.forName(type.getCanonicalName() + "Async");
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            type.getCanonicalName() + " does not have a corresponding async interface", e);
      }
      return asyncType;
    }

  }
}
