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
package com.google.gwtmockito.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;

import org.mockito.internal.stubbing.defaultanswers.ReturnsMocks;
import org.mockito.invocation.InvocationOnMock;

/**
 * An answer that generally returns mocks, but with a few overrides.
 * <p>
 * This class is public so that it can be refernced by generated code - users
 * should not reference it directly.
 * 
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class ReturnsCustomMocks extends ReturnsMocks {
  @Override
  public Object answer(InvocationOnMock invocation) throws Throwable {
    // Make JavaScriptObject.cast work in most cases by forcing it to return the underlying mock
    // instead of a new mock of type JavaScriptObject. This allows cast to be used in situations
    // that don't violate the Java type system, but not in situations that do (even though
    // javascript would allow them).
    String methodName = invocation.getMethod().getName();
    if (invocation.getMock() instanceof JavaScriptObject && methodName.equals("cast")) {
      return invocation.getMock();
    } else if (invocation.getMock() instanceof Element && methodName.equals("getTagName")) {
      String className = invocation.getMock().getClass().getSimpleName();
      return className.substring(0, className.indexOf("Element")).toLowerCase();
    } else if (invocation.getMock() instanceof InputElement && methodName.equals("getType")) {
      return "text";
    } else {
      return super.answer(invocation);
    }
  }
}
