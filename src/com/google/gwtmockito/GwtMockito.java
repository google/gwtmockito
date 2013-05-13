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
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwtmockito.fakes.FakeClientBundleProvider;
import com.google.gwtmockito.fakes.FakeMessagesProvider;
import com.google.gwtmockito.fakes.FakeProvider;
import com.google.gwtmockito.fakes.FakeUiBinderProvider;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
 *   <li> Messages, CssResource, and SafeHtmlTemplates: uses a fake that
 *        implements each method by returning a String of SafeHtml based on the
 *        name of the method and any arguments passed to it. The exact format is
 *        undefined. See {@link FakeMessagesProvider} for details.
 * </ul>
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
    useProviderForType(ClientBundle.class, new FakeClientBundleProvider());
    useProviderForType(CssResource.class, new FakeMessagesProvider<CssResource>());
    useProviderForType(Messages.class, new FakeMessagesProvider<Messages>());
    useProviderForType(SafeHtmlTemplates.class, new FakeMessagesProvider<SafeHtmlTemplates>());
    useProviderForType(UiBinder.class, new FakeUiBinderProvider());

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
   * the given type and its subclasses. Note that if you just want to return a
   * Mockito mock from GWT.create, it's probably easier to use {@link GwtMock}
   * instead.
   */
  public static <T> void useProviderForType(Class<T> type, FakeProvider<? extends T> provider) {
    if (bridge == null) {
      throw new IllegalStateException("Must call initMocks() before calling useProviderForType()");
    }
    if (bridge.registeredMocks.containsKey(type)) {
      throw new IllegalArgumentException(
          "Can't use a provider for a type that already has a @GwtMock declared");
    }
    bridge.registeredProviders.put(type, provider);
  }

  private static void registerGwtMocks(Object owner) {
    for (Field field : owner.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(GwtMock.class)) {
        Object mock = Mockito.mock(field.getType());
        if (bridge.registeredMocks.containsKey(field.getType())) {
          throw new IllegalArgumentException("Owner declares multiple @GwtMocks for type "
              + field.getType().getSimpleName() + "; only one is allowed");
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

  private static class Bridge extends GWTBridge {
    private Map<Class<?>, FakeProvider<?>> registeredProviders = 
        new HashMap<Class<?>, FakeProvider<?>>();
    private Map<Class<?>, Object> registeredMocks = new HashMap<Class<?>, Object>();

    @Override
    @SuppressWarnings("unchecked") // safe since we check whether the type is assignable
    public <T> T create(Class<?> type) {
      // First check if we have a GwtMock for this exact type and use it if so.
      if (registeredMocks.containsKey(type)) {
        return (T) registeredMocks.get(type);
      }

      // Next see if we have a provider for this type or a supertype.
      for (Entry<Class<?>, FakeProvider<?>> entry : registeredProviders.entrySet()) {
        if (entry.getKey().isAssignableFrom(type)) {
          // We know this is safe since we just checked that the type can be assigned to the entry
          @SuppressWarnings({"rawtypes", "cast"})
          Class rawType = (Class) type;
          return (T) entry.getValue().getFake(rawType);
        }
      }

      // If nothing has been registered, just return a new mock object to avoid NPEs.
      return (T) mock(type, Mockito.RETURNS_MOCKS);
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
  }
}
