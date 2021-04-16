/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.keyple.core.card.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApduResponseJsonAdapterTest {

  @BeforeClass
  public static void beforeClass() {
    JsonUtil.registerTypeAdapter(ApduResponse.class, new ApduResponseJsonAdapter(), true);
  }

  @Test
  public void serialize_deserialize() {
    ApduResponse apduResponseIn = new ApduResponse(ByteArrayUtil.fromHex("01020304050607089000"));
    String json = JsonUtil.toJson(apduResponseIn);
    ApduResponse apduResponseOut = JsonUtil.getParser().fromJson(json, ApduResponse.class);
    assertThat(apduResponseOut.getBytes()).containsExactly(apduResponseIn.getBytes());
    assertThat(apduResponseOut.getStatusCode()).isEqualTo(apduResponseIn.getStatusCode());
  }
}
