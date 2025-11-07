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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;

public class BrokerBasedWebSessionConfiguration {

  private static final int DEFAULT_NUMBER_OF_THREADS = 1;

  private final Integer numberOfThreads;

  @Inject
  public BrokerBasedWebSessionConfiguration(
      PluginConfigFactory configFactory, @PluginName String pluginName) {

    PluginConfig fromGerritConfig = configFactory.getFromGerritConfig(pluginName);

    this.numberOfThreads = fromGerritConfig.getInt("numberOfThreads", DEFAULT_NUMBER_OF_THREADS);
  }

  public Integer getNumberOfThreads() {
    return numberOfThreads;
  }
}
