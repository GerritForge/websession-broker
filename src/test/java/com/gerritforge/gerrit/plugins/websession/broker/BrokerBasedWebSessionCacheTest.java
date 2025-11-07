// Copyright (C) 2025 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.websession.broker;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.FakeWebSessionVal;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.Event;
import com.gerritforge.gerrit.plugins.websession.broker.BrokerBasedWebSessionCache.WebSessionEvent;
import com.gerritforge.gerrit.plugins.websession.broker.BrokerBasedWebSessionCache.WebSessionEvent.Operation;
import com.gerritforge.gerrit.plugins.websession.broker.log.WebSessionLogger;
import com.gerritforge.gerrit.plugins.websession.broker.util.TimeMachine;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BrokerBasedWebSessionCacheTest {

  private static final int DEFAULT_ACCOUNT_ID = 1000000;
  private static final String KEY = "aSceprtma6B0qZ0hKxXHvQ5iyfUhCcFXxG";
  private static Val VAL =
      FakeWebSessionVal.getVal(Account.id(1), ExternalId.Key.parse("foo:bar", true));
  private static final String PLUGIN_NAME = "websession-broker";

  private byte[] emptyPayload = new byte[] {-84, -19, 0, 5, 112};
  byte[] defaultPayload =
      new byte[] {
        -84, -19, 0, 5, 115, 114, 0, 45, 99, 111, 109, 46, 103, 111, 111, 103, 108, 101, 46, 103,
        101, 114, 114, 105, 116, 46, 104, 116, 116, 112, 100, 46, 87, 101, 98, 83, 101, 115, 115,
        105, 111, 110, 77, 97, 110, 97, 103, 101, 114, 36, 86, 97, 108, 0, 0, 0, 0, 0, 0, 0, 2, 3,
        0, 0, 120, 112, 119, 97, 1, -64, -124, 61, 2, 0, 0, 1, 111, 13, -8, 90, 7, 3, 0, 5, 34, 97,
        83, 99, 101, 112, 114, 113, 86, 87, 54, 85, 79, 45, 88, 51, 107, 51, 116, 102, 85, 109, 86,
        103, 82, 73, 90, 56, 53, 99, 99, 52, 71, 114, 87, 6, 0, 0, 1, 111, 16, 84, -103, -121, 7,
        34, 97, 83, 99, 101, 112, 114, 114, 82, 103, 119, 49, 71, 110, 90, 56, 122, 54, 49, 49, 86,
        52, 121, 110, 65, 100, 110, 113, 99, 68, 45, 105, 99, 75, 97, 0, 120
      };

  ExecutorService executorServce = MoreExecutors.newDirectExecutorService();

  @Mock BrokerApi brokerApi;
  Cache<String, Val> cache;
  @Mock TimeMachine timeMachine;
  @Mock PluginConfigFactory cfg;
  @Mock PluginConfig pluginConfig;
  @Mock WebSessionLogger webSessionLogger;
  @Mock Config gerritConfig;
  @Captor ArgumentCaptor<Event> eventCaptor;
  @Captor ArgumentCaptor<Val> valCaptor;

  BrokerBasedWebSessionCache objectUnderTest;

  String instanceId = "instance-id";

  @Before
  public void setup() {
    cache = CacheBuilder.newBuilder().build();
    when(pluginConfig.getString("webSessionTopic", "gerrit_web_session"))
        .thenReturn("gerrit_web_session");
    when(cfg.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    when(timeMachine.now()).thenReturn(Instant.EPOCH);

    objectUnderTest = createBroker();
  }

  @Test
  public void shouldPublishMessageWhenLoginEvent() {
    WebSessionEvent eventMessage = createEventMessage();
    Val value = createVal(eventMessage);

    objectUnderTest.put(KEY, value);
    verify(brokerApi, times(1)).send(anyString(), eventCaptor.capture());

    assertThat(eventCaptor.getValue()).isNotNull();
    WebSessionEvent event = (WebSessionEvent) eventCaptor.getValue();
    assertThat(event.operation).isEqualTo(WebSessionEvent.Operation.ADD);
    assertThat(event.key).isEqualTo(KEY);
    assertThat(event.payload).isEqualTo(defaultPayload);
  }

  @Test
  public void shouldPublishMessageWhenLogoutEvent() {
    objectUnderTest.invalidate(KEY);

    verify(brokerApi, times(1)).send(anyString(), eventCaptor.capture());

    assertThat(eventCaptor.getValue()).isNotNull();
    WebSessionEvent event = (WebSessionEvent) eventCaptor.getValue();
    assertThat(event.operation).isEqualTo(WebSessionEvent.Operation.REMOVE);
    assertThat(event.key).isEqualTo(KEY);
    assertThat(event.payload).isEqualTo(emptyPayload);
  }

  @Test
  public void shouldUpdateCacheWhenLoginMessageReceived() {
    WebSessionEvent eventMessage = createEventMessage();

    objectUnderTest.processMessage(eventMessage);

    Val val = cache.getIfPresent(eventMessageKey(eventMessage));
    assertThat(val).isNotNull();
    assertThat(val.getAccountId().get()).isEqualTo(DEFAULT_ACCOUNT_ID);
  }

  @Test
  public void shouldUpdateCacheWhenLogoutMessageReceived() {
    WebSessionEvent eventMessage = createEventMessage(emptyPayload, Operation.REMOVE);
    cache.put(KEY, VAL);

    objectUnderTest.processMessage(eventMessage);

    assertThat(cache.getIfPresent(KEY)).isNull();
  }

  @Test
  public void shouldCleanupExpiredSessions() {
    when(timeMachine.now()).thenReturn(Instant.MIN, Instant.MAX);

    WebSessionEvent eventMessage = createEventMessage();

    objectUnderTest.processMessage(eventMessage);

    Val val = cache.getIfPresent(eventMessageKey(eventMessage));
    assertThat(val).isNotNull();

    objectUnderTest.cleanUp();

    assertThat(cache.getIfPresent(eventMessageKey(eventMessage))).isNull();
  }

  @Test
  public void shouldSkipSessionsReplayForPersistedCache() {
    when(gerritConfig.getInt(eq("cache"), eq("web_sessions"), eq("diskLimit"), anyInt()))
        .thenReturn(2048);
    objectUnderTest = createBroker();

    objectUnderTest.start();

    verify(brokerApi, never()).replayAllEvents(anyString());
  }

  @Test
  public void shouldReplaySessionsForInMemoryCache() {
    when(gerritConfig.getInt(eq("cache"), eq("web_sessions"), eq("diskLimit"), anyInt()))
        .thenReturn(0);

    objectUnderTest = createBroker();

    objectUnderTest.start();

    verify(brokerApi, times(1)).replayAllEvents(anyString());
  }

  @Test
  public void shouldSkipSessionsReplayForDefaultCacheSettings() {
    when(gerritConfig.getInt(eq("cache"), eq("web_sessions"), eq("diskLimit"), anyInt()))
        .thenReturn(1024);

    objectUnderTest = createBroker();
    objectUnderTest.start();
    verify(brokerApi, never()).replayAllEvents(anyString());
  }

  private BrokerBasedWebSessionCache createBroker() {
    DynamicItem<BrokerApi> item = DynamicItem.itemOf(BrokerApi.class, brokerApi);
    return new BrokerBasedWebSessionCache(
        cache,
        item,
        timeMachine,
        cfg,
        PLUGIN_NAME,
        webSessionLogger,
        executorServce,
        instanceId,
        gerritConfig);
  }

  private Val createVal(Event message) {
    WebSessionEvent event = (WebSessionEvent) message;

    objectUnderTest.processMessage(message);
    return cache.getIfPresent(event.key);
  }

  private WebSessionEvent createEventMessage() {

    return createEventMessage(defaultPayload, Operation.ADD);
  }

  private String eventMessageKey(WebSessionEvent eventMessage) {
    return eventMessage.key;
  }

  private WebSessionEvent createEventMessage(byte[] payload, Operation operation) {
    WebSessionEvent event = new WebSessionEvent(KEY, payload, operation);
    event.instanceId = instanceId;
    return event;
  }
}
