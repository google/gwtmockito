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
package com.google.gwtmockito.fakes;

import com.google.gwt.i18n.client.DateTimeFormatInfo;
import com.google.gwt.i18n.client.DefaultDateTimeFormatInfo;
import com.google.gwt.i18n.client.impl.LocaleInfoImpl;

/**
 * Provides a fake implementations of {@link LocaleInfoImpl} that makes use of
 * {@link DefaultDateTimeFormatInfo}, which makes date/time formatting possible in tests.
 * 
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class FakeLocaleInfoImplProvider implements FakeProvider<LocaleInfoImpl> {
  @Override
  public LocaleInfoImpl getFake(Class<?> type) {
    return new LocaleInfoImpl() {
      @Override
      public DateTimeFormatInfo getDateTimeFormatInfo() {
        return new DefaultDateTimeFormatInfo();
      }
    };
  }
}
