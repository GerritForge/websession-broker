// Copyright (C) 2026 GerritForge, Inc.
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

import com.gerritforge.gerrit.eventbroker.AckAwareConsumer;
import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.BrokerApiPluginListener;
import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.plugins.StartPluginListener;
import com.google.gerrit.server.plugins.StopPluginListener;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.UniqueAnnotations;

/** Subscribes to a topic as soon as a broker plugin has bound its {@link BrokerApi}. */
@Singleton
public class WebSessionBrokerApiLoadedListener implements BrokerApiPluginListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final DynamicItem<BrokerApi> brokerApi;

  private String topic;
  private AckAwareConsumer<Event> consumer;
  private boolean replayAllEvents;
  private volatile boolean started;

  @Inject
  WebSessionBrokerApiLoadedListener(DynamicItem<BrokerApi> brokerApi) {
    this.brokerApi = brokerApi;
  }

  @Override
  public DynamicItem<BrokerApi> brokerApiDynamicItem() {
    return brokerApi;
  }

  synchronized void subscribe(String topic, AckAwareConsumer<Event> consumer, boolean replayAll) {
    this.topic = topic;
    this.consumer = consumer;
    this.replayAllEvents = replayAll;
    if (isBrokerApiStarted()) {
      onBrokerApiStarted();
    } else {
      logger.atInfo().log(
          "No broker plugin started, waiting before subscribing to topic %s", topic);
    }
  }

  @Override
  public synchronized void onBrokerApiStarted() {
    Preconditions.checkState(!started, "Broker api has already been started");
    logger.atInfo().log(
        "Subscribing to topic %s on broker plugin %s", topic, brokerApi.getPluginName());
    brokerApi.get().receiveAsync(topic, consumer);
    if (replayAllEvents) {
      brokerApi.get().replayAllEvents(topic);
    }
    started = true;
  }

  @Override
  public void beforeBrokerApiStopped() {
    Preconditions.checkState(started, "Broker api was not started");
    started = false;
  }

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(WebSessionBrokerApiLoadedListener.class);
      bind(StartPluginListener.class)
          .annotatedWith(UniqueAnnotations.create())
          .to(WebSessionBrokerApiLoadedListener.class);
      bind(StopPluginListener.class)
          .annotatedWith(UniqueAnnotations.create())
          .to(WebSessionBrokerApiLoadedListener.class);
    }
  }
}
