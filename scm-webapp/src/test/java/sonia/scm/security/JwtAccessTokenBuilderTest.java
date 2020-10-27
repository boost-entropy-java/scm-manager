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

import com.google.common.collect.Sets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static sonia.scm.security.SecureKeyTestUtil.createSecureKey;

/**
 * Unit test for {@link JwtAccessTokenBuilder}.
 *
 * @author Sebastian Sdorra
 */
@ExtendWith(MockitoExtension.class)
class JwtAccessTokenBuilderTest {

  @Mock
  private KeyGenerator keyGenerator;

  @Mock
  private SecureKeyResolver secureKeyResolver;

  private Set<AccessTokenEnricher> enrichers;

  private JwtAccessTokenBuilderFactory factory;

  @Mock
  private Subject subject;
  @Mock
  private PrincipalCollection principalCollection;

  @BeforeEach
  void bindSubject() {
    lenient().when(subject.getPrincipal()).thenReturn("trillian");
    lenient().when(subject.getPrincipals()).thenReturn(principalCollection);
    ThreadContext.bind(subject);
  }

  @AfterEach
  void unbindSubject() {
    ThreadContext.unbindSubject();
  }

  /**
   * Prepare mocks and set up object under test.
   */
  @BeforeEach
  void setUpObjectUnderTest() {
    lenient().when(keyGenerator.createKey()).thenReturn("42");
    lenient().when(secureKeyResolver.getSecureKey(anyString())).thenReturn(createSecureKey());
    enrichers = Sets.newHashSet();
    factory = new JwtAccessTokenBuilderFactory(keyGenerator, secureKeyResolver, enrichers);
  }

  /**
   * Tests {@link JwtAccessTokenBuilder#build()}.
   */
  @Test
  void testBuild() {
    JwtAccessToken token = factory.create().subject("dent")
      .issuer("https://www.scm-manager.org")
      .expiresIn(5, TimeUnit.SECONDS)
      .custom("a", "b")
      .scope(Scope.valueOf("repo:*"))
      .build();

    // assert claims
    assertClaims(token);

    // reparse and assert again
    String compact = token.compact();
    assertThat(compact).isNotEmpty();
    Claims claims = Jwts.parser()
      .setSigningKey(secureKeyResolver.getSecureKey("dent").getBytes())
      .parseClaimsJws(compact)
      .getBody();
    assertClaims(new JwtAccessToken(claims, compact));
  }

  private void assertClaims(JwtAccessToken token) {
    assertThat(token.getId()).isNotEmpty();
    assertThat(token.getIssuedAt()).isNotNull();
    assertThat(token.getExpiration()).isNotNull();
    assertThat(token.getExpiration().getTime() > token.getIssuedAt().getTime()).isTrue();
    assertThat(token.getSubject()).isEqualTo("dent");
    assertThat(token.getIssuer()).isNotEmpty();
    assertThat(token.getIssuer()).get().isEqualTo("https://www.scm-manager.org");
    assertThat(token.getCustom("a")).get().isEqualTo("b");
    assertThat(token.getScope()).hasToString("[\"repo:*\"]");
  }

  @Nested
  class FromApiKeyRealm {

    @BeforeEach
    void mockApiKeyRealm() {
      lenient().when(principalCollection.getRealmNames()).thenReturn(singleton("ApiTokenRealm"));
    }

    @Test
    void testRejectedRequest() {
      JwtAccessTokenBuilder builder = factory.create().subject("dent");
      assertThrows(AuthorizationException.class, builder::build);
    }
  }

  @Nested
  class FromDefaultRealm {

    @BeforeEach
    void mockDefaultRealm() {
      lenient().when(principalCollection.getRealmNames()).thenReturn(singleton("DefaultRealm"));
    }

    /**
     * Tests {@link JwtAccessTokenBuilder#build()} with subject from shiro context.
     */
    @Test
    void testBuildWithoutSubject() {
      JwtAccessToken token = factory.create().build();
      assertThat(token.getSubject()).isEqualTo("trillian");
    }

    /**
     * Tests {@link JwtAccessTokenBuilder#build()} with explicit subject.
     */
    @Test
    void testBuildWithSubject() {
      JwtAccessToken token = factory.create().subject("dent").build();
      assertThat(token.getSubject()).isEqualTo("dent");
    }

    /**
     * Tests {@link JwtAccessTokenBuilder#build()} with enricher.
     */
    @Test
    void testBuildWithEnricher() {
      enrichers.add((b) -> b.custom("c", "d"));
      JwtAccessToken token = factory.create().subject("dent").build();
      assertThat(token.getCustom("c")).get().isEqualTo("d");
    }
  }
}
