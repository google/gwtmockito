/*
 * Copyright 2014 Google Inc.
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
package com.google.gwtmockito.impl;

import javassist.CtMethod;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import org.mockito.Mockito;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates stub implementations for built-in GWT methods whose behavior we
 * want to replace.
 * <p>
 * This class is public so that it can be referenced by generated code - users
 * should not reference it directly.
 *
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class StubGenerator {

  private static final Map<ClassAndMethod, StubMethod> STUB_METHODS =
      new HashMap<ClassAndMethod, StubMethod>();
  static {
    // Anchor.getElement must return an AnchorElement rather than a plain Element
    STUB_METHODS.put(
        new ClassAndMethod(Anchor.class, "getAnchorElement"),
        new ReturnMockStubMethod(AnchorElement.class));
    // ListBox.getSelectElement must return a SelectElement rather than a plain Element
    STUB_METHODS.put(
        new ClassAndMethod(ListBox.class, "getSelectElement"),
        new ReturnMockStubMethod(SelectElement.class));
    // TextBox.getInputElement must return an InputElement rather than a plain Element
    STUB_METHODS.put(
        new ClassAndMethod(TextBox.class, "getInputElement"),
        new ReturnMockStubMethod(InputElement.class));
    // InputElement needs to be able to convert generic elements into input elements
    STUB_METHODS.put(
        new ClassAndMethod(InputElement.class, "as"),
        new ReturnMockStubMethod(InputElement.class));
    // URL.encodeQueryStringImpl
    STUB_METHODS.put(
      new ClassAndMethod(URL.class, "encodeQueryStringImpl"),
      new ReturnStringStubMethod("encodeQueryStringImpl"));
    // URL.encodePathSegmentImpl
    STUB_METHODS.put(
      new ClassAndMethod(URL.class, "encodePathSegmentImpl"),
      new ReturnStringStubMethod("encodePathSegmentImpl"));
  }

  /** Returns whether the behavior of the given method should be replaced. */
  public static boolean shouldStub(CtMethod method, Collection<Class<?>> classesToStub) {
    // Stub any methods for which we have given explicit implementations
    if (STUB_METHODS.containsKey(new ClassAndMethod(
        method.getDeclaringClass().getName(), method.getName()))) {
      return true;
    }

    // Stub all non-abstract methods of classes for which stubbing has been requested
    for (Class<?> clazz : classesToStub) {
      if (declaringClassIs(method, clazz) && (method.getModifiers() & Modifier.ABSTRACT) == 0) {
        return true;
      }
    }

    // Stub all native methods
    if ((method.getModifiers() & Modifier.NATIVE) != 0) {
      return true;
    }

    return false;
  }

  /** Invokes the stubbed behavior of the given method. */
  public static Object invoke(Class<?> returnType, String className, String methodName) {
    // If we have an explicit implementation for this method, invoke it
    if (STUB_METHODS.containsKey(new ClassAndMethod(className, methodName))) {
      return STUB_METHODS.get(new ClassAndMethod(className, methodName)).invoke();
    }

    // Otherwise return an appropriate basic type
    if (returnType == String.class) {
      return "";
    } else if (returnType == Boolean.class) {
      return false;
    } else if (returnType == Byte.class) {
      return (byte) 0;
    } else if (returnType == Character.class) {
      return (char) 0;
    } else if (returnType == Double.class) {
      return (double) 0;
    } else if (returnType == Integer.class) {
      return (int) 0;
    } else if (returnType == Float.class) {
      return (float) 0;
    } else if (returnType == Long.class) {
      return (long) 0;
    } else if (returnType == Short.class) {
      return (short) 0;
    } else {
      return Mockito.mock(returnType, new ReturnsCustomMocks());
    }
  }

  private static boolean declaringClassIs(CtMethod method, Class<?> clazz) {
    return method.getDeclaringClass().getName().replace('$', '.')
        .equals(clazz.getCanonicalName());
  }

  /** Map key composed of a class and method name. */
  private static class ClassAndMethod {
    private final String className;
    private final String methodName;

    ClassAndMethod(Class<?> clazz, String methodName) {
      this.className = clazz.getCanonicalName();
      this.methodName = methodName;
    }

    ClassAndMethod(String className, String methodName) {
      this.className = className;
      this.methodName = methodName;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ClassAndMethod) {
        ClassAndMethod other = (ClassAndMethod) obj;
        return className.equals(other.className) && methodName.equals(other.methodName);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return (className + methodName).hashCode();
    }
  }

  /** Fake implementation of a method. */
  private interface StubMethod {
    Object invoke();
  }

  /** A fake method implementation that just returns a new stub for a given class. */
  private static class ReturnMockStubMethod implements StubMethod {
    final Class<?> clazz;

    ReturnMockStubMethod(Class<?> clazz) {
      this.clazz = clazz;
    }

    @Override
    public Object invoke() {
      return Mockito.mock(clazz, new ReturnsCustomMocks());
    }
  }

  /** A fake method implementation that just returns a string. */
  private static class ReturnStringStubMethod implements StubMethod {

    private String str;

    ReturnStringStubMethod(String str) {
      this.str = str;
    }

    @Override
    public Object invoke() {
      return str;
    }
  }
}
