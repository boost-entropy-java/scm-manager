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

package sonia.scm.server;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ServerConfigParser {

  private static final String CONFIGURATION_FILE = "/config.yml";

  ServerConfigParser() {
  }

  ServerConfigYaml parse() {
    return parse(this.getClass().getResource(CONFIGURATION_FILE));
  }

  ServerConfigYaml parse(URL configFile) {
    if (configFile == null) {
      throw new ServerConfigurationException("""
        Could not find config.yml.
                    
        If you have upgraded from an older SCM-Manager version, you have to migrate your server-config.xml 
        to the new format using the official instructions: 
                  
        https://scm-manager.org/docs/latest/en/migrate-scm-manager-from-v2/
        """);
    }
    try (InputStream is = configFile.openStream()) {
      Representer representer = new Representer(new DumperOptions());
      representer.getPropertyUtils().setSkipMissingProperties(true);
      return new Yaml(representer).loadAs(is, ServerConfigYaml.class);
    } catch (IOException e) {
      throw new ServerConfigurationException("Could not parse config.yml", e);
    }
  }
}