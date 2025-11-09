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

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.httpd.WebSessionManager;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.gerritforge.gerrit.plugins.websession.broker.log.WebSessionLogger;
import com.gerritforge.gerrit.plugins.websession.broker.log.WebSessionLogger.Direction;
import com.gerritforge.gerrit.plugins.websession.broker.util.TimeMachine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.eclipse.jgit.lib.Config;

@Singleton
public class BrokerBasedWebSessionCache
    implements Cache<String, WebSessionManager.Val>, LifecycleListener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static String DEFAULT_WEB_SESSION_TOPIC = "gerrit_web_session";

  Cache<String, Val> cache;
  String webSessionTopicName;
  DynamicItem<BrokerApi> brokerApi;
  TimeMachine timeMachine;
  ExecutorService executor;
  private final WebSessionLogger webSessionLogger;
  private String instanceId;
  private final boolean shouldReplayAllSessions;

  @Inject
  public BrokerBasedWebSessionCache(
      @Named(WebSessionManager.CACHE_NAME) Cache<String, Val> cache,
      DynamicItem<BrokerApi> brokerApi,
      TimeMachine timeMachine,
      PluginConfigFactory cfg,
      @PluginName String pluginName,
      WebSessionLogger webSessionLogger,
      @WebSessionProducerExecutor ExecutorService executor,
      @Nullable @GerritInstanceId String gerritInstanceId,
      @GerritServerConfig Config gerritConfig) {
    this.cache = cache;
    this.brokerApi = brokerApi;
    this.timeMachine = timeMachine;
    this.webSessionTopicName = getWebSessionTopicName(cfg, pluginName);
    this.shouldReplayAllSessions = shouldReplayAllSessions(gerritConfig);
    this.webSessionLogger = webSessionLogger;
    this.executor = executor;
    this.instanceId = gerritInstanceId;
  }

  private Boolean shouldReplayAllSessions(Config gerritConfig) {
    return gerritConfig.getInt("cache", "web_sessions", "diskLimit", 1024) == 0;
  }

  protected void processMessage(Event message) {
    if (!WebSessionEvent.TYPE.equals(message.getType())) {
      logger.atWarning().log("Skipping web session message of unknown type: %s", message.getType());
      return;
    }

    WebSessionEvent event = (WebSessionEvent) message;

    switch (event.operation) {
      case ADD:
        try (ByteArrayInputStream in = new ByteArrayInputStream(event.payload);
            ObjectInputStream inputStream = new ObjectInputStream(in)) {
          Val value = (Val) inputStream.readObject();

          webSessionLogger.log(Direction.CONSUME, webSessionTopicName, event, Optional.of(value));
          Instant expires = Instant.ofEpochMilli(value.getExpiresAt());
          if (expires.isAfter(timeMachine.now())) {
            cache.put(event.key, value);
          }

        } catch (IOException | ClassNotFoundException e) {
          logger.atSevere().withCause(e).log("Malformed event '%s'", message);
        }
        break;
      case REMOVE:
        cache.invalidate(event.key);
        webSessionLogger.log(Direction.CONSUME, webSessionTopicName, event, Optional.empty());

        break;
      default:
        logger.atWarning().log(
            "Skipping web session message of unknown operation type: %s", event.operation);
        break;
    }
  }

  @Override
  public @Nullable Val getIfPresent(Object key) {
    return cache.getIfPresent(key);
  }

  @Override
  public Val get(String key, Callable<? extends Val> valueLoader) throws ExecutionException {
    return cache.get(key, valueLoader);
  }

  @Override
  public ImmutableMap<String, Val> getAllPresent(Iterable<?> keys) {
    return cache.getAllPresent(keys);
  }

  @Override
  public void put(String key, Val value) {
    sendEvent(key, value, WebSessionEvent.Operation.ADD);
    cache.put(key, value);
  }

  @Override
  public void putAll(Map<? extends String, ? extends Val> keys) {
    for (Entry<? extends String, ? extends Val> e : keys.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public void invalidate(Object key) {
    sendEvent((String) key, null, WebSessionEvent.Operation.REMOVE);
    cache.invalidate(key);
  }

  @Override
  public void invalidateAll(Iterable<?> keys) {
    for (Object key : keys) {
      invalidate(key);
    }
  }

  @Override
  public void invalidateAll() {
    cache.asMap().forEach((key, value) -> invalidate(key));
  }

  @Override
  public long size() {
    return cache.size();
  }

  @Override
  public CacheStats stats() {
    return cache.stats();
  }

  @Override
  public ConcurrentMap<String, Val> asMap() {
    return cache.asMap();
  }

  @Override
  public void cleanUp() {
    Instant now = timeMachine.now();
    cache.asMap().entrySet().stream()
        .filter(entry -> Instant.ofEpochMilli(entry.getValue().getExpiresAt()).isBefore(now))
        .forEach(entry -> cache.invalidate(entry.getKey()));
  }

  private void sendEvent(String key, Val value, WebSessionEvent.Operation operation) {
    try {
      executor.execute(new WebSessionEventTask(key, value, operation));
    } catch (RuntimeException e) {
      logger.atSevere().withCause(e).log(
          "Cannot send web-session message for '%s Topic: '%s'", key, webSessionTopicName);
    }
  }

  public String getWebSessionTopicName(PluginConfigFactory cfg, String pluginName) {
    return cfg.getFromGerritConfig(pluginName)
        .getString("webSessionTopic", DEFAULT_WEB_SESSION_TOPIC);
  }

  public static class WebSessionEvent extends Event {

    public enum Operation {
      ADD,
      REMOVE;
    }

    static final String TYPE = "web-session";
    public String key;
    public byte[] payload;
    public Operation operation;

    protected WebSessionEvent(String key, byte[] payload, Operation operation) {
      super(TYPE);
      this.key = key;
      this.payload = payload;
      this.operation = operation;
    }
  }

  @Override
  public void start() {
    if (brokerApi == null || brokerApi.get() == null) {
      throw new IllegalStateException("Cannot find binding for BrokerApi");
    }
    brokerApi.get().receiveAsync(webSessionTopicName, this::processMessage);
    if (shouldReplayAllSessions) {
      brokerApi.get().replayAllEvents(webSessionTopicName);
    }
  }

  @Override
  public void stop() {}

  private class WebSessionEventTask implements Runnable {
    private String key;
    private Val value;
    private WebSessionEvent.Operation operation;

    public WebSessionEventTask(String key, Val value, WebSessionEvent.Operation operation) {
      this.key = key;
      this.value = value;
      this.operation = operation;
    }

    @Override
    public void run() {
      try (ByteArrayOutputStream out = new ByteArrayOutputStream();
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(out)) {

        objectOutputStream.writeObject(value);
        out.flush();
        byte[] serializedObject = out.toByteArray();
        WebSessionEvent webSessionEvent = new WebSessionEvent(key, serializedObject, operation);
        webSessionEvent.instanceId = instanceId;
        ListenableFuture<Boolean> resultF =
            brokerApi.get().send(webSessionTopicName, webSessionEvent);
        Futures.addCallback(
            resultF,
            new FutureCallback<Boolean>() {
              @Override
              public void onSuccess(Boolean aBoolean) {
                webSessionLogger.log(
                    Direction.PUBLISH,
                    webSessionTopicName,
                    webSessionEvent,
                    Optional.ofNullable(value));
              }

              @Override
              public void onFailure(Throwable throwable) {
                logger.atSevere().log(
                    "Cannot send web-session message for '%s Topic: '%s'",
                    key, webSessionTopicName);
              }
            },
            MoreExecutors.directExecutor());
      } catch (IOException e) {
        logger.atSevere().withCause(e).log(
            "Cannot serialize event for account id '%s'", value.getAccountId());
      }
    }
  }
}
