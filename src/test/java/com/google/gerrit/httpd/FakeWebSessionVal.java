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

package com.google.gerrit.httpd;

import com.google.gerrit.entities.Account;
import com.google.gerrit.httpd.WebSessionManager.Val;
import com.google.gerrit.server.account.externalids.ExternalId;
import org.junit.Ignore;

@Ignore
public class FakeWebSessionVal {

  public static Val getVal(Account.Id accountId, ExternalId.Key externalIdKey) {
    return new Val(accountId, 0, false, externalIdKey, 0, "", "");
  }
}
