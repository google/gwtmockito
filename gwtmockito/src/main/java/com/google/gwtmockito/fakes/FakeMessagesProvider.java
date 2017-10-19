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
package com.google.gwtmockito.fakes;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Provides fake implementations of {@link com.google.gwt.i18n.client.Messages},
 * {@link com.google.gwt.resources.client.CssResource}, and
 * {@link com.google.gwt.safehtml.client.SafeHtmlTemplates}. The fake
 * implementations implement methods by returning Strings of SafeHtml instances
 * based on the method name and the arguments passed to it. The exact format of
 * the message is undefined and is subject to change.
 *
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class FakeMessagesProvider<T> implements FakeProvider<T> {

  /**
   * Returns a new instance of the given type that implements methods as
   * described in the class description.
   *
   * @param type interface to be implemented by the returned type.
   */
  @Override
  @SuppressWarnings("unchecked") // safe since the proxy implements type
  public T getFake(Class<?> type) {
    return (T) Proxy.newProxyInstance(FakeMessagesProvider.class.getClassLoader(), new Class<?>[] {type},
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            if (method.getName().equals("ensureInjected")) {
              return true;
            } else if (method.getName().equals("hashCode")) {
              return proxy.getClass().hashCode();
            } else if (method.getName().equals("equals")) {
              return proxy.getClass().equals(args[0].getClass());
            } else if (method.getReturnType() == String.class) {
              return buildMessage(method, args);
            } else if (method.getReturnType() == SafeHtml.class) {
              return SafeHtmlUtils.fromTrustedString(buildMessage(method, args));
            } else {
              throw new IllegalArgumentException(method.getName()
                  + " must return either String or SafeHtml");
            }
          }
        });
  }

  private String buildMessage(Method method, Object[] args) {
    StringBuilder message = new StringBuilder(method.getName());
    if (args == null || args.length == 0) {
      return message.toString();
    }

    message.append('(');
    message.append(stringify(args[0]));
    for (Object arg : Arrays.asList(args).subList(1, args.length)) {
      message.append(", ").append(stringify(arg));
    }
    return message.append(')').toString();
  }

  private String stringify(Object arg) {
    if (arg == null) {
      return "null";
    } else if (arg instanceof SafeHtml) {
      return ((SafeHtml) arg).asString();
    } else if (arg instanceof SafeUri) {
      return ((SafeUri) arg).asString();
    } else {
      return arg.toString();
    }
  }
}
