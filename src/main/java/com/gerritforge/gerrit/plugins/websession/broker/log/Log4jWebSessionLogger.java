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

package com.gerritforge.gerrit.plugins.websession.broker.log;

import com.google.gerrit.extensions.systemstatus.ServerInformation;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.google.gerrit.server.util.PluginLogFile;
import com.google.gerrit.server.util.SystemLog;
import com.google.inject.Inject;
import com.gerritforge.gerrit.plugins.websession.broker.BrokerBasedWebSessionCache.WebSessionEvent;
import java.util.Optional;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4jWebSessionLogger extends PluginLogFile implements WebSessionLogger {
  private static final String LOG_NAME = "websession_log";
  private final Logger msgLog;

  @Inject
  public Log4jWebSessionLogger(SystemLog systemLog, ServerInformation serverInfo) {
    super(systemLog, serverInfo, LOG_NAME, new PatternLayout("[%d{ISO8601}] [%t] %-5p : %m%n"));
    this.msgLog = LoggerFactory.getLogger(LOG_NAME);
  }

  @Override
  public void log(Direction direction, String topic, WebSessionEvent event, Optional<Val> payload) {
    msgLog.info("{} {} {} {}", direction, topic, formatEvent(event), formatSession(payload));
  }

  private Object formatEvent(WebSessionEvent event) {
    return String.format(
        "{ \"operation\":\"%s\", \"key\":\"%s\", \"eventCreatedOn\":%d }",
        event.operation, event.key, event.eventCreatedOn);
  }

  private Object formatSession(Optional<Val> payload) {
    return payload
        .map(
            session ->
                String.format(
                    "{ \"accountId\":%d, \"expiresAt\":%d }",
                    session.getAccountId().get(), session.getExpiresAt()))
        .orElse("");
  }
}
