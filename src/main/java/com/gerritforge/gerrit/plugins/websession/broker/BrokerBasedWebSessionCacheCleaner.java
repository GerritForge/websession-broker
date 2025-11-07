// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.websession.broker;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.config.ConfigUtil;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.concurrent.ScheduledFuture;

@Singleton
public class BrokerBasedWebSessionCacheCleaner implements LifecycleListener {

  private static int DEFAULT_CLEANUP_INTERVAL = 24;

  WorkQueue queue;
  Provider<CleanupTask> cleanupTaskProvider;
  ScheduledFuture<?> scheduledCleanupTask;
  long cleanupIntervalMillis;

  static class CleanupTask implements Runnable {
    private final BrokerBasedWebSessionCache brokerBasedWebSessionCache;
    private final String pluginName;

    @Inject
    CleanupTask(
        BrokerBasedWebSessionCache brokerBasedWebSessionCache, @PluginName String pluginName) {
      this.brokerBasedWebSessionCache = brokerBasedWebSessionCache;
      this.pluginName = pluginName;
    }

    @Override
    public void run() {
      brokerBasedWebSessionCache.cleanUp();
    }

    @Override
    public String toString() {
      return String.format("[%s] Clean up expired file based websessions", pluginName);
    }
  }

  @Inject
  public BrokerBasedWebSessionCacheCleaner(
      WorkQueue queue,
      Provider<CleanupTask> cleanupTaskProvider,
      PluginConfigFactory cfg,
      @PluginName String pluginName) {
    this.queue = queue;
    this.cleanupTaskProvider = cleanupTaskProvider;
    this.cleanupIntervalMillis = getCleanupInterval(cfg, pluginName);
  }

  @Override
  public void start() {
    scheduledCleanupTask =
        queue
            .getDefaultQueue()
            .scheduleAtFixedRate(
                cleanupTaskProvider.get(),
                SECONDS.toMillis(1),
                cleanupIntervalMillis,
                MILLISECONDS);
  }

  @Override
  public void stop() {
    if (scheduledCleanupTask != null) {
      scheduledCleanupTask.cancel(true);
      scheduledCleanupTask = null;
    }
  }

  private Long getCleanupInterval(PluginConfigFactory cfg, String pluginName) {
    String fromConfig =
        Strings.nullToEmpty(cfg.getFromGerritConfig(pluginName).getString("cleanupInterval"));
    return HOURS.toMillis(ConfigUtil.getTimeUnit(fromConfig, DEFAULT_CLEANUP_INTERVAL, HOURS));
  }
}
