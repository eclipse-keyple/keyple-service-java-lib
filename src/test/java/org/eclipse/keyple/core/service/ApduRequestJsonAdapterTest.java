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

import org.eclipse.keyple.core.card.ApduRequest;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApduRequestJsonAdapterTest {

  @BeforeClass
  public static void beforeClass() {
    JsonUtil.registerTypeAdapter(ApduRequest.class, new ApduRequestJsonAdapter(), true);
  }

  @Test
  public void serialize_deserialize() {
    // create a ApduRequest
    ApduRequest apduRequestIn =
        new ApduRequest(ByteArrayUtil.fromHex("000C000000"), true)
            .setName("GET RESPONSE")
            .addSuccessfulStatusCode(0x9101);
    String json = JsonUtil.toJson(apduRequestIn);
    ApduRequest apduRequestOut = JsonUtil.getParser().fromJson(json, ApduRequest.class);
    assertThat(apduRequestOut.getBytes()).containsExactly(apduRequestIn.getBytes());
    assertThat(apduRequestOut.isCase4()).isEqualTo(apduRequestIn.isCase4());
    assertThat(apduRequestOut.getSuccessfulStatusCodes())
        .isEqualTo(apduRequestIn.getSuccessfulStatusCodes());
    assertThat(apduRequestOut.getName()).isEqualTo(apduRequestIn.getName());
  }
}
