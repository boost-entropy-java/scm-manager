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

package sonia.scm.security;

import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sonia.scm.AlreadyExistsException;
import sonia.scm.store.ConfigurationEntryStore;
import sonia.scm.store.ConfigurationEntryStoreFactory;
import sonia.scm.store.InMemoryConfigurationEntryStoreFactory;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyServiceTest {

  int nextKey = 1;

  PasswordService passwordService = mock(PasswordService.class);
  Supplier<String> keyGenerator = () -> Integer.toString(nextKey++);
  ConfigurationEntryStoreFactory storeFactory = new InMemoryConfigurationEntryStoreFactory();
  ConfigurationEntryStore<ApiKeyCollection> store = storeFactory.withType(ApiKeyCollection.class).withName("apiKeys").build();
  ApiKeyService service = new ApiKeyService(storeFactory, passwordService, keyGenerator);


  @BeforeEach
  void mockPasswordService() {
    when(passwordService.encryptPassword(any()))
      .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0) + "-hashed");
    when(passwordService.passwordsMatch(any(), any()))
      .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(1, String.class).startsWith(invocationOnMock.getArgument(0)));
  }

  @Nested
  class WithLoggedInUser {
    @BeforeEach
    void mockUser() {
      final Subject subject = mock(Subject.class);
      ThreadContext.bind(subject);
      final PrincipalCollection principalCollection = mock(PrincipalCollection.class);
      when(subject.getPrincipals()).thenReturn(principalCollection);
      when(principalCollection.getPrimaryPrincipal()).thenReturn("dent");
    }

    @AfterEach
    void unbindSubject() {
      ThreadContext.unbindSubject();
    }

    @Test
    void shouldCreateNewKeyAndStoreItHashed() {
      String newKey = service.createNewKey("1", "READ");

      ApiKeyCollection apiKeys = store.get("dent");

      assertThat(apiKeys.getKeys()).hasSize(1);
      ApiKeyWithPassphrase key = apiKeys.getKeys().iterator().next();
      assertThat(key.getRole()).isEqualTo("READ");
      assertThat(key.getPassphrase()).isEqualTo("1-hashed");
      assertThat(newKey).isEqualTo("1");

      Optional<String> role = service.check("dent", "1",  "1-hashed");

      assertThat(role).contains("READ");
    }

    @Test
    void shouldReturnRoleForKey() {
      String newKey = service.createNewKey("1", "READ");

      Optional<String> role = service.check("dent", "1", newKey);

      assertThat(role).contains("READ");
    }

    @Test
    void shouldNotReturnAnythingWithWrongKey() {
      service.createNewKey("1", "READ");

      Optional<String> role = service.check("dent", "1", "wrong");

      assertThat(role).isEmpty();
    }

    @Test
    void shouldAddSecondKey() {
      String firstKey = service.createNewKey("1", "READ");
      String secondKey = service.createNewKey("2", "WRITE");

      ApiKeyCollection apiKeys = store.get("dent");

      assertThat(apiKeys.getKeys()).hasSize(2);

      assertThat(service.check("dent", "1", firstKey)).contains("READ");
      assertThat(service.check("dent", "2", secondKey)).contains("WRITE");

      assertThat(service.getKeys()).extracting("name").contains("1", "2");
    }

    @Test
    void shouldRemoveKey() {
      String firstKey = service.createNewKey("1", "READ");
      String secondKey = service.createNewKey("2", "WRITE");

      service.remove("1");

      assertThat(service.check("dent", "1", firstKey)).isEmpty();
      assertThat(service.check("dent", "2", secondKey)).contains("WRITE");
    }

    @Test
    void shouldFailWhenAddingSameNameTwice() {
      String firstKey = service.createNewKey("1", "READ");

      assertThrows(AlreadyExistsException.class, () -> service.createNewKey("1", "WRITE"));

      assertThat(service.check("dent", "1", firstKey)).contains("READ");
    }

    @Test
    void shouldIgnoreCorrectPassphraseWithWrongName() {
      String firstKey = service.createNewKey("1", "READ");

      assertThat(service.check("dent", "other", firstKey)).isEmpty();
    }
  }
}
