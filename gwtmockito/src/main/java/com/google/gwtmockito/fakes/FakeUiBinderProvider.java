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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides fake implementations of {@link UiBinder}. The fake implementation
 * populates all (non-provided) {@link UiField} in the target type with objects
 * obtained from GWT.create. {@link com.google.gwtmockito.GwtMockito} can be
 * used to control values returned from GWT.create and hence affect how fields
 * are populated.
 *
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class FakeUiBinderProvider implements FakeProvider<UiBinder<?, ?>>{

  /**
   * Returns a new instance of FakeUiBinder that implements the given interface.
   * This is accomplished by returning a dynamic proxy object that delegates
   * calls to a backing FakeUiBinder.
   *
   * @param type interface to be implemented by the returned type. This must
   *        represent an interface that directly extends {@link UiBinder}.
   */
  @Override
  public UiBinder<?, ?> getFake(final Class<?> type) {
    return (UiBinder<?, ?>) Proxy.newProxyInstance(
        FakeUiBinderProvider.class.getClassLoader(),
        new Class<?>[] {type},
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            // createAndBindUi is the only method defined by UiBinder
            for (Field field : getAllFields(args[0].getClass())) {
              if (field.isAnnotationPresent(UiField.class)
                  && !field.getAnnotation(UiField.class).provided()) {
                field.setAccessible(true);
                field.set(args[0], GWT.create(field.getType()));
              }
            }
            return GWT.create(getUiRootType(type));
          }
        });
  }

  private <T> Class<?> getUiRootType(Class<T> type) {
    // The UI root type is the first generic type parameter of the UiBinder
    ParameterizedType parameterizedType = (ParameterizedType) type.getGenericInterfaces()[0];
    Type uiRootType = parameterizedType.getActualTypeArguments()[0];
    if (uiRootType instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) uiRootType).getRawType();
    } else {
      return (Class<?>) uiRootType;
    }
  }

  private List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new LinkedList<Field>();
    fields.addAll(Arrays.asList(type.getDeclaredFields()));
    if (type.getSuperclass() != null) {
      fields.addAll(getAllFields(type.getSuperclass()));
    }
    return fields;
  }
}
