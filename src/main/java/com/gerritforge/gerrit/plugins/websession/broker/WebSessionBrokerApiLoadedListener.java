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
import com.gerritforge.gerrit.eventbroker.BrokerApiPluginListener;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.server.plugins.StartPluginListener;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.UniqueAnnotations;

/**
 * Subscribes the web session cache once a broker plugin has bound its {@link BrokerApi}.
 */
@Singleton
public class WebSessionBrokerApiLoadedListener implements BrokerApiPluginListener {
  private final DynamicItem<BrokerApi> brokerApi;

  private volatile BrokerBasedWebSessionCache cache;

  @Inject
  WebSessionBrokerApiLoadedListener(DynamicItem<BrokerApi> brokerApi) {
    this.brokerApi = brokerApi;
  }

  @Override
  public DynamicItem<BrokerApi> brokerApiDynamicItem() {
    return brokerApi;
  }

  void register(BrokerBasedWebSessionCache cache) {
    this.cache = cache;
  }

  @Override
  public void onBrokerApiStarted() {
    BrokerBasedWebSessionCache registered = cache;
    if (registered != null) {
      registered.subscribe();
    }
  }

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(WebSessionBrokerApiLoadedListener.class);
      bind(StartPluginListener.class)
          .annotatedWith(UniqueAnnotations.create())
          .to(WebSessionBrokerApiLoadedListener.class);
    }
  }
}
