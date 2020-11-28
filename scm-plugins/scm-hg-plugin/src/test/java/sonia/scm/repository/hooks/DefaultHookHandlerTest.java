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

package sonia.scm.repository.hooks;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ExceptionWithContext;
import sonia.scm.NotFoundException;
import sonia.scm.TransactionId;
import sonia.scm.repository.RepositoryHookType;
import sonia.scm.repository.api.HgHookMessage;
import sonia.scm.repository.api.HgHookMessageProvider;
import sonia.scm.repository.spi.HgHookContextProvider;
import sonia.scm.repository.spi.HookEventFacade;
import sonia.scm.security.CipherUtil;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultHookHandlerTest {

  @Mock
  private HookContextProviderFactory hookContextProviderFactory;

  @Mock
  private HgHookContextProvider contextProvider;

  @Mock
  private HookEventFacade hookEventFacade;

  @Mock
  private HookEventFacade.HookEventHandler hookEventHandler;

  @Mock
  private Socket socket;

  private HookEnvironment hookEnvironment;

  private DefaultHookHandler handler;

  @Mock
  private Subject subject;

  @BeforeEach
  void setUp() {
    ThreadContext.bind(subject);

    hookEnvironment = new HookEnvironment();

    handler = new DefaultHookHandler(hookContextProviderFactory, hookEventFacade, hookEnvironment, socket);
  }

  private void mockMessageProvider() {
    mockMessageProvider(new HgHookMessageProvider());
  }

  private void mockMessageProvider(HgHookMessageProvider messageProvider) {
    when(hookContextProviderFactory.create("42", "abc")).thenReturn(contextProvider);
    when(contextProvider.getHgMessageProvider()).thenReturn(messageProvider);
  }

  @AfterEach
  void tearDown() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldFireHook() throws IOException {
    mockMessageProvider();
    when(hookEventFacade.handle("42")).thenReturn(hookEventHandler);

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertSuccess(response, RepositoryHookType.POST_RECEIVE);
    assertThat(hookEnvironment.isPending()).isFalse();
  }

  @Test
  void shouldSetPendingStateOnPreReceiveHooks() throws IOException {
    mockMessageProvider();
    when(hookEventFacade.handle("42")).thenReturn(hookEventHandler);

    // we have to capture the pending state, when the hook is fired
    // because the state is cleared before the method ends
    AtomicReference<Boolean> ref = new AtomicReference<>(Boolean.FALSE);
    doAnswer(ic -> {
      ref.set(hookEnvironment.isPending());
      return null;
    }).when(hookEventHandler).fireHookEvent(RepositoryHookType.PRE_RECEIVE, contextProvider);

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.PRE_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertSuccess(response, RepositoryHookType.PRE_RECEIVE);
    assertThat(ref.get()).isTrue();
  }

  @Test
  void shouldHandleUnknownFailure() throws IOException {
    mockMessageProvider();

    doThrow(new IllegalStateException("Something went wrong"))
      .when(hookEventFacade)
      .handle("42");

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertError(response, "unknown error");
  }

  @Test
  void shouldHandleExceptionWithContext() throws IOException {
    mockMessageProvider();

    doThrow(new TestingException("Exception with Context"))
      .when(hookEventFacade)
      .handle("42");

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertError(response, "Exception with Context");
  }

  @Test
  void shouldSendMessagesOnUnknownException() throws IOException {
    mockMessageProviderWithMessages();

    doThrow(new IllegalStateException("Abort it"))
      .when(hookEventFacade)
      .handle("42");

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertMessages(response, "unknown error");
  }

  @Test
  void shouldSendMessagesOnExceptionWithContext() throws IOException {
    mockMessageProviderWithMessages();

    doThrow(new TestingException("Exception with Context"))
      .when(hookEventFacade)
      .handle("42");

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertMessages(response, "Exception with Context");
  }

  private void assertMessages(DefaultHookHandler.Response response, String errorMessage) {
    List<String> received = response.getMessages()
      .stream()
      .map(HgHookMessage::getMessage)
      .collect(Collectors.toList());

    assertThat(received).containsExactly("Some note", "Some error", errorMessage);
  }

  private void mockMessageProviderWithMessages() {
    HgHookMessageProvider messageProvider = new HgHookMessageProvider();
    messageProvider.sendMessage("Some note");
    messageProvider.sendMessage("Some error");
    mockMessageProvider(messageProvider);
  }

  @Test
  void shouldSetAndClearTransactionId() throws IOException {
    mockMessageProvider();

    AtomicReference<String> ref = new AtomicReference<>();
    doAnswer(ic -> {
      TransactionId.get().ifPresent(ref::set);
      return null;
    }).when(hookEventFacade).handle("42");

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    send(request);

    assertThat(ref).hasValue("ti21");
    assertThat(TransactionId.get()).isEmpty();
  }

  @Test
  void shouldHandleAuthenticationFailure() throws IOException {
    doThrow(AuthenticationException.class)
      .when(subject)
      .login(any(AuthenticationToken.class));

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertError(response, "authentication");
  }

  @Test
  void shouldHandleNotFoundException() throws IOException {
    doThrow(NotFoundException.class)
      .when(hookEventFacade)
      .handle("42");

    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE);
    DefaultHookHandler.Response response = send(request);

    assertError(response, "not found");
  }

  @Test
  void shouldReturnErrorWithInvalidChallenge() throws IOException {
    DefaultHookHandler.Request request = createRequest(RepositoryHookType.POST_RECEIVE, "something-different");
    DefaultHookHandler.Response response = send(request);

    assertError(response, "challenge");
  }

  private void assertSuccess(DefaultHookHandler.Response response, RepositoryHookType type) {
    assertThat(response.getMessages()).isEmpty();
    assertThat(response.isAbort()).isFalse();

    verify(hookEventHandler).fireHookEvent(eq(type), any(HgHookContextProvider.class));
  }

  private void assertError(DefaultHookHandler.Response response, String message) {
    assertThat(response.isAbort()).isTrue();
    assertThat(response.getMessages()).hasSize(1);
    HgHookMessage hgHookMessage = response.getMessages().get(0);
    assertThat(hgHookMessage.getSeverity()).isEqualTo(HgHookMessage.Severity.ERROR);
    assertThat(hgHookMessage.getMessage()).contains(message);
  }

  @Nonnull
  private DefaultHookHandler.Request createRequest(RepositoryHookType type) {
    return createRequest(type, hookEnvironment.getChallenge());
  }

  @Nonnull
  private DefaultHookHandler.Request createRequest(RepositoryHookType type, String challenge) {
    String secret = CipherUtil.getInstance().encode("secret");
    return new DefaultHookHandler.Request(
      secret, type, "ti21", "42", challenge, "abc"
    );
  }

  private DefaultHookHandler.Response send(DefaultHookHandler.Request request) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Sockets.send(buffer, request);

    ByteArrayInputStream input = new ByteArrayInputStream(buffer.toByteArray());
    when(socket.getInputStream()).thenReturn(input);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    when(socket.getOutputStream()).thenReturn(output);

    handler.run();

    return Sockets.receive(new ByteArrayInputStream(output.toByteArray()), DefaultHookHandler.Response.class);
  }

  private static class TestingException extends ExceptionWithContext {

    private TestingException(String message) {
      super(Collections.emptyList(), message);
    }

    @Override
    public String getCode() {
      return "42";
    }
  }

}
