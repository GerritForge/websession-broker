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

package com.gerritforge.gerrit.plugins.websession.broker.util;

import com.google.inject.Singleton;
import java.time.Clock;
import java.time.Instant;

@Singleton
public class TimeMachine {

  private Clock clock = Clock.systemDefaultZone();

  public Instant now() {
    return Instant.now(getClock());
  }

  public Clock getClock() {
    return clock;
  }
}
