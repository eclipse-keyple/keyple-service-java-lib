/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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

import org.eclipse.keyple.core.util.HexUtil;
import org.junit.Test;

public class ApduResponseAdapterTest {

  ApduResponseAdapter apduResponseAdapter;
  public static String HEX_REQUEST = "123456789000";
  public static String HEX_REQUEST_DATA = "12345678";

  @Test
  public void buildApduResponseAdapter() {
    apduResponseAdapter = new ApduResponseAdapter(HexUtil.toByteArray(HEX_REQUEST));
    assertThat(apduResponseAdapter.getApdu()).isEqualTo(HexUtil.toByteArray(HEX_REQUEST));
    assertThat(apduResponseAdapter.getStatusWord()).isEqualTo(0x9000);
    assertThat(apduResponseAdapter.getDataOut()).isEqualTo(HexUtil.toByteArray(HEX_REQUEST_DATA));
  }

  @Test
  public void buildApduResponseAdapter_withoutResponseTime_shouldReturnNull() {
    // Given
    byte[] apdu = HexUtil.toByteArray(HEX_REQUEST);

    // When
    apduResponseAdapter = new ApduResponseAdapter(apdu);

    // Then
    assertThat(apduResponseAdapter.getResponseTime()).isNull();
  }

  @Test
  public void buildApduResponseAdapter_withResponseTime_shouldReturnResponseTime() {
    // Given
    byte[] apdu = HexUtil.toByteArray(HEX_REQUEST);
    Integer expectedResponseTime = 150;

    // When
    apduResponseAdapter = new ApduResponseAdapter(apdu, expectedResponseTime);

    // Then
    assertThat(apduResponseAdapter.getResponseTime()).isEqualTo(150);
    assertThat(apduResponseAdapter.getApdu()).isEqualTo(HexUtil.toByteArray(HEX_REQUEST));
    assertThat(apduResponseAdapter.getStatusWord()).isEqualTo(0x9000);
    assertThat(apduResponseAdapter.getDataOut()).isEqualTo(HexUtil.toByteArray(HEX_REQUEST_DATA));
  }

  @Test
  public void buildApduResponseAdapter_withNullResponseTime_shouldReturnNull() {
    // Given
    byte[] apdu = HexUtil.toByteArray(HEX_REQUEST);

    // When
    apduResponseAdapter = new ApduResponseAdapter(apdu, null);

    // Then
    assertThat(apduResponseAdapter.getResponseTime()).isNull();
  }

  @Test
  public void buildApduResponseAdapter_withZeroResponseTime_shouldReturnZero() {
    // Given
    byte[] apdu = HexUtil.toByteArray(HEX_REQUEST);

    // When
    apduResponseAdapter = new ApduResponseAdapter(apdu, 0);

    // Then
    assertThat(apduResponseAdapter.getResponseTime()).isEqualTo(0);
  }

  @Test
  public void buildApduResponseAdapter_withLargeResponseTime_shouldReturnValue() {
    // Given
    byte[] apdu = HexUtil.toByteArray(HEX_REQUEST);
    Integer largeResponseTime = 5000; // 5 seconds

    // When
    apduResponseAdapter = new ApduResponseAdapter(apdu, largeResponseTime);

    // Then
    assertThat(apduResponseAdapter.getResponseTime()).isEqualTo(5000);
  }
}
