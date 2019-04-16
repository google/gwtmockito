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
package com.google.gwtmockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allowing the test to configure a list of package names that should always be
 * loaded via the standard system classloader instead of through GwtMockito's custom classloader.
 * Any subpackages of these packages will also be loaded with the standard loader. If you're
 * getting "loader constraint violation" errors, try defining adding this annotation to your test
 * class with the package the error is complaining about.
 * <p>
 * Note that if you have a set of packages that must be blacklisted in every test, you might be
 * better off implementing your own subclass of {@link GwtMockitoTestRunner} that overrides
 * {@link GwtMockitoTestRunner#getPackagesToLoadViaStandardClassloader} so that you don't have to
 * duplicate the annotation in every test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithPackagesToLoadViaStandardClassLoader {
  /**
   * The set of packages whose classes should never be reloaded by GwtMockito's custom class loader
   * during the execution of an annotated test.
   */
  String[] value();
}
