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

import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.user.client.ui.Anchor;

import org.mockito.Mockito;

import java.util.Map;

/**
 * Generates stub implementations for built-in GWT methods whose behavior we
 * want to replace.
 * <p>
 * This class is public so that it can be refernced by generated code - users
 * should not reference it directly.
 *
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class StubGenerator {

  private static final Map<ClassAndMethod, StubMethod> STUB_METHODS =
      new HashMap<ClassAndMethod, StubMethod>();
  static {
    // Anchor.getElement must return an AnchorElement rather than a plain Element
    STUB_METHODS.put(new ClassAndMethod(Anchor.class, "getAnchorElement"), new StubMethod() {
      @Override
      public Object invoke() {
        return Mockito.mock(AnchorElement.class, new ReturnsCustomMocks());
      }
    });
  }

  /** Returns whether the behavior of the given method should be replaced. */
  public static boolean shouldStub(CtMethod method) {
    return STUB_METHODS.containsKey(new ClassAndMethod(
        method.getDeclaringClass().getName(), method.getName()));
  }

  /** Invokes the stubbed behavior of the given method. */
  public static Object invoke(String className, String methodName) {
    return STUB_METHODS.get(new ClassAndMethod(className, methodName)).invoke();
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
}
