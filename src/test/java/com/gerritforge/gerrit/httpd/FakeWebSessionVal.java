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
