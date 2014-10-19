/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwtmockito.fakes;

import com.google.gwt.i18n.client.constants.NumberConstantsImpl;

/**
 * Provides a fake implementation of {@link NumberConstantsImpl} using a hardcoded English locale.
 * The data here come from GWT's com/google/gwt/i18n/client/constants/NumberConstants_en.properties.
 *
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class FakeNumberConstantsImplProvider implements FakeProvider<NumberConstantsImpl> {
  @Override
  public NumberConstantsImpl getFake(Class<?> type) {
    return new NumberConstantsImpl() {
      @Override
      public String zeroDigit() {
        return "0";
      }

      @Override
      public String simpleCurrencyPattern() {
        return "\u00A4#,##0.00;(\u00A4#,##0.00)";
      }

      @Override
      public String scientificPattern() {
        return "#E0";
      }

      @Override
      public String plusSign() {
        return "+";
      }

      @Override
      public String percentPattern() {
        return "#,##0%";
      }

      @Override
      public String percent() {
        return "%";
      }

      @Override
      public String perMill() {
        return "\u2030";
      }

      @Override
      public String notANumber() {
        return "NaN";
      }

      @Override
      public String monetarySeparator() {
        return ".";
      }

      @Override
      public String monetaryGroupingSeparator() {
        return ",";
      }

      @Override
      public String minusSign() {
        return "-";
      }

      @Override
      public String infinity() {
        return "\u221E";
      }

      @Override
      public String groupingSeparator() {
        return ",";
      }

      @Override
      public String globalCurrencyPattern() {
        return "\u00A4\u00A4\u00A4\u00A4#,##0.00 \u00A4\u00A4;(\u00A4\u00A4\u00A4\u00A4#,##0.00";
      }

      @Override
      public String exponentialSymbol() {
        return "E";
      }

      @Override
      public String defCurrencyCode() {
        return "USD";
      }

      @Override
      public String decimalSeparator() {
        return ".";
      }

      @Override
      public String decimalPattern() {
        return "#,##0.###";
      }

      @Override
      public String currencyPattern() {
        return "\u00A4#,##0.00;(\u00A4#,##0.00)";
      }
    };
  }
}
