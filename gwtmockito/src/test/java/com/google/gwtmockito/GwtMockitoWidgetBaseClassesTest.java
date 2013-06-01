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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RenderablePanel;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.lang.reflect.Method;

/**
 * Tests confirming that GwtMockito works with a wide range of potential base classes for widgets by
 * checking that every method on the base classes can be invoked without exception.
 */
@RunWith(GwtMockitoTestRunner.class)
public class GwtMockitoWidgetBaseClassesTest {

  @Test
  public void testUiObjects() throws Exception {
    invokeAllAccessibleMethods(new Widget() {});
    invokeAllAccessibleMethods(new UIObject() {});
    invokeAllAccessibleMethods(new Composite() {});
  }

  @Test
  public void testPanels() throws Exception {
    invokeAllAccessibleMethods(new AbsolutePanel() {});
    invokeAllAccessibleMethods(new CellPanel() {});
    invokeAllAccessibleMethods(new ComplexPanel() {});
    invokeAllAccessibleMethods(new DeckLayoutPanel() {});
    invokeAllAccessibleMethods(new DeckPanel() {});
    invokeAllAccessibleMethods(new DecoratorPanel() {});
    invokeAllAccessibleMethods(new DockLayoutPanel(Unit.PX) {});
    invokeAllAccessibleMethods(new DockPanel() {});
    invokeAllAccessibleMethods(new FlowPanel() {});
    invokeAllAccessibleMethods(new FocusPanel() {});
    invokeAllAccessibleMethods(new HorizontalPanel() {});
    invokeAllAccessibleMethods(new HTMLPanel("") {});
    invokeAllAccessibleMethods(new LayoutPanel() {});
    invokeAllAccessibleMethods(new PopupPanel() {});
    invokeAllAccessibleMethods(new RenderablePanel("") {});
    invokeAllAccessibleMethods(new ResizeLayoutPanel() {});
    invokeAllAccessibleMethods(new SimpleLayoutPanel() {});
    invokeAllAccessibleMethods(new SimplePanel() {});
    invokeAllAccessibleMethods(new SplitLayoutPanel() {});
    invokeAllAccessibleMethods(new StackPanel() {});
    invokeAllAccessibleMethods(new VerticalPanel() {});
  }

  private void invokeAllAccessibleMethods(Object instance) throws Exception {
    for (Method method : instance.getClass().getMethods()) {
      if (method.getDeclaringClass() != Object.class) {
        invokeMethodWithArbitraryArguments(instance, method);
      }
    }
  }

  private void invokeMethodWithArbitraryArguments(Object instance, Method method) throws Exception {
    Object[] args = new Object[method.getParameterTypes().length];
    for (int i = 0; i < args.length; i++) {
      Class<?> type = method.getParameterTypes()[i];
      // Add more cases here as needed
      if (type == String.class) {
        args[i] = "";
      } else if (type == boolean.class) {
        args[i] = false;
      } else if (type == double.class || type == Double.class) {
        args[i] = 0.0;
      } else if (type == char.class) {
        args[i] = 'a';
      } else if (type == int.class) {
        args[i] = 0;
      } else if (type == long.class) {
        args[i] = 0L;
      } else if (type.isEnum()) {
        args[i] = type.getEnumConstants()[0];
      } else {
        args[i] = Mockito.mock(type);
      }
    }
    method.invoke(instance, args);
  }
}
