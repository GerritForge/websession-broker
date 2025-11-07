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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Provider;
import com.gerritforge.gerrit.plugins.websession.broker.BrokerBasedWebSessionCacheCleaner.CleanupTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BrokerBasedWebSessionCacheCleanerTest {
  private static Long CLEANUP_INTERVAL = 1L;
  private static String SOME_PLUGIN_NAME = "somePluginName";

  @Mock private ScheduledThreadPoolExecutor executorMock;
  @Mock private ScheduledFuture<?> scheduledFutureMock;
  @Mock private WorkQueue workQueueMock;
  @Mock private Provider<CleanupTask> cleanupTaskProviderMock;
  @Mock PluginConfigFactory cfg;
  @Mock PluginConfig pluginConfig;

  private BrokerBasedWebSessionCacheCleaner objectUnderTest;

  @Before
  public void setUp() {
    when(pluginConfig.getString("cleanupInterval")).thenReturn(CLEANUP_INTERVAL.toString());
    when(cfg.getFromGerritConfig(SOME_PLUGIN_NAME)).thenReturn(pluginConfig);
    when(cleanupTaskProviderMock.get()).thenReturn(new CleanupTask(null, null));
    when(workQueueMock.getDefaultQueue()).thenReturn(executorMock);
    doReturn(scheduledFutureMock)
        .when(executorMock)
        .scheduleAtFixedRate(isA(CleanupTask.class), anyLong(), anyLong(), isA(TimeUnit.class));
    objectUnderTest =
        new BrokerBasedWebSessionCacheCleaner(
            workQueueMock, cleanupTaskProviderMock, cfg, SOME_PLUGIN_NAME);
  }

  @Test
  public void testCleanupTaskIsScheduledOnStart() {
    objectUnderTest.start();
    verify(executorMock, times(1))
        .scheduleAtFixedRate(
            isA(CleanupTask.class), eq(1000l), eq(3600000L), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testCleanupTaskIsCancelledOnStop() {
    objectUnderTest.start();
    objectUnderTest.stop();
    verify(scheduledFutureMock, times(1)).cancel(true);
  }

  @Test
  public void testCleanupTaskIsCancelledOnlyOnce() {
    objectUnderTest.start();
    objectUnderTest.stop();
    objectUnderTest.stop();
    verify(scheduledFutureMock, times(1)).cancel(true);
  }
}
