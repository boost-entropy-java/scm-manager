/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package sonia.scm.config;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;

@Slf4j
public final class WebappConfigProvider {

  private static WebappConfigProvider instance;

  private final Map<String, String> configBindings;
  private final Map<String, String> environment;

  private WebappConfigProvider(Map<String, String> configBindings, Map<String, String> environment) {
    this.configBindings = configBindings;
    this.environment = environment;
  }

  public static void setConfigBindings(Map<String, String> newBindings) {
    WebappConfigProvider.setConfigBindings(newBindings, System.getenv());
  }

  static void setConfigBindings(Map<String, String> newBindings, Map<String, String> environment) {
    instance = new WebappConfigProvider(newBindings, environment);
  }

  public static Optional<String> resolveAsString(String key) {
    return resolveConfig(key);
  }

  public static Optional<Boolean> resolveAsBoolean(String key) {
    return resolveConfig(key).map(Boolean::parseBoolean);
  }

  public static Optional<Integer> resolveAsInteger(String key) {
    return resolveConfig(key).map(Integer::parseInt);
  }

  public static Optional<Long> resolveAsLong(String key) {
    return resolveConfig(key).map(Long::parseLong);
  }

  private static Optional<String> resolveConfig(String key) {
    if (instance == null) {
      return empty();
    }
    return instance.resolveConfigInternal(key);
  }

  private Optional<String> resolveConfigInternal(String key) {
    String envValue = environment.get("SCM_WEBAPP_" + key.replace('.', '_').toUpperCase());
    if (envValue != null) {
      log.debug("resolve config for key '{}' to value '{}' from environment", key, envValue);
      return Optional.of(envValue);
    }
    String value = instance.configBindings.get(key);
    if (value == null) {
      log.debug("could not resolve config for key '{}'", key);
      return empty();
    } else {
      log.debug("resolve config for key '{}' to value '{}'", key, value);
      return Optional.of(value);
    }
  }
}
