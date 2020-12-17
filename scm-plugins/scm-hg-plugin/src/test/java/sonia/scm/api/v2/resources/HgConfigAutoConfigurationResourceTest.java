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

package sonia.scm.api.v2.resources;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.HgConfig;
import sonia.scm.repository.HgRepositoryHandler;
import sonia.scm.web.HgVndMediaType;
import sonia.scm.web.RestDispatcher;

import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware(
  configuration = "classpath:sonia/scm/configuration/shiro.ini",
  password = "secret"
)
@RunWith(MockitoJUnitRunner.class)
public class HgConfigAutoConfigurationResourceTest {

  @Rule
  public ShiroRule shiro = new ShiroRule();

  private RestDispatcher dispatcher = new RestDispatcher();

  @InjectMocks
  private HgConfigDtoToHgConfigMapperImpl dtoToConfigMapper;

  @Mock
  private HgRepositoryHandler repositoryHandler;

  @Mock
  private Provider<HgConfigAutoConfigurationResource> resourceProvider;

  @Before
  public void prepareEnvironment() {
    HgConfigAutoConfigurationResource resource =
      new HgConfigAutoConfigurationResource(dtoToConfigMapper, repositoryHandler);

    when(resourceProvider.get()).thenReturn(resource);
    dispatcher.addSingletonResource(
      new HgConfigResource(null, null, null,
        resourceProvider));
  }

  @Test
  @SubjectAware(username = "writeOnly")
  public void shouldSetDefaultConfigAndInstallHg() throws Exception {
    MockHttpResponse response = put(null);

    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());

    HgConfig actualConfig = captureConfig();
    assertFalse(actualConfig.isDisabled());
  }

  @Test
  @SubjectAware(username = "readOnly")
  public void shouldNotSetDefaultConfigAndInstallHgWhenNotAuthorized() throws Exception {
    MockHttpResponse response = put(null);

    assertEquals("Subject does not have permission [configuration:write:hg]", response.getContentAsString());
    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  @Test
  @SubjectAware(username = "writeOnly")
  public void shouldUpdateConfigAndInstallHg() throws Exception {
    MockHttpResponse response = put("{\"disabled\":true}");

    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());

    HgConfig actualConfig = captureConfig();
    assertTrue(actualConfig.isDisabled());
  }

  @Test
  @SubjectAware(username = "readOnly")
  public void shouldNotUpdateConfigAndInstallHgWhenNotAuthorized() throws Exception {
    MockHttpResponse response = put("{\"disabled\":true}");

    assertEquals("Subject does not have permission [configuration:write:hg]", response.getContentAsString());
    assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
  }

  private MockHttpResponse put(String content) throws URISyntaxException {
    MockHttpRequest request = MockHttpRequest.put("/" + HgConfigResource.HG_CONFIG_PATH_V2 + "/auto-configuration");

    if (content != null) {
      request
        .contentType(HgVndMediaType.CONFIG)
        .content(content.getBytes());
    }

    MockHttpResponse response = new MockHttpResponse();
    dispatcher.invoke(request, response);
    return response;
  }

  private HgConfig captureConfig() {
    ArgumentCaptor<HgConfig> configCaptor = ArgumentCaptor.forClass(HgConfig.class);
    verify(repositoryHandler).doAutoConfiguration(configCaptor.capture());
    return configCaptor.getValue();
  }

}
