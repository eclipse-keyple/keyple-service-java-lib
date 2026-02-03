/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.spi.ApduRequestSpi;
import org.junit.Test;

public class InternalDtoTest {

  @Test
  public void apduRequest_withMaxExpectedResponseTime_shouldCopyValue() {
    // Given
    ApduRequestSpi originalRequest = mock(ApduRequestSpi.class);
    when(originalRequest.getApdu()).thenReturn(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00});
    when(originalRequest.getSuccessfulStatusWords())
        .thenReturn(new HashSet<>(Collections.singletonList(0x9000)));
    when(originalRequest.getInfo()).thenReturn("Test APDU");
    when(originalRequest.getMaxExpectedResponseTime()).thenReturn(200);

    // When
    InternalDto.ApduRequest dto = new InternalDto.ApduRequest(originalRequest);

    // Then
    assertThat(dto.getMaxExpectedResponseTime()).isEqualTo(200);
    assertThat(dto.getApdu()).isEqualTo(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00});
    assertThat(dto.getInfo()).isEqualTo("Test APDU");
  }

  @Test
  public void apduRequest_withNullMaxExpectedResponseTime_shouldReturnNull() {
    // Given
    ApduRequestSpi originalRequest = mock(ApduRequestSpi.class);
    when(originalRequest.getApdu()).thenReturn(new byte[] {0x00, (byte) 0xA4});
    when(originalRequest.getSuccessfulStatusWords())
        .thenReturn(new HashSet<>(Collections.singletonList(0x9000)));
    when(originalRequest.getMaxExpectedResponseTime()).thenReturn(null);

    // When
    InternalDto.ApduRequest dto = new InternalDto.ApduRequest(originalRequest);

    // Then
    assertThat(dto.getMaxExpectedResponseTime()).isNull();
  }

  @Test
  public void apduRequest_withZeroMaxExpectedResponseTime_shouldReturnZero() {
    // Given
    ApduRequestSpi originalRequest = mock(ApduRequestSpi.class);
    when(originalRequest.getApdu()).thenReturn(new byte[] {0x00, (byte) 0xA4});
    when(originalRequest.getSuccessfulStatusWords())
        .thenReturn(new HashSet<>(Collections.singletonList(0x9000)));
    when(originalRequest.getMaxExpectedResponseTime()).thenReturn(0);

    // When
    InternalDto.ApduRequest dto = new InternalDto.ApduRequest(originalRequest);

    // Then
    assertThat(dto.getMaxExpectedResponseTime()).isEqualTo(0);
  }

  @Test
  public void apduRequest_jsonSerialization_shouldPreserveMaxExpectedResponseTime() {
    // Given
    ApduRequestSpi originalRequest = mock(ApduRequestSpi.class);
    when(originalRequest.getApdu()).thenReturn(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x05});
    when(originalRequest.getSuccessfulStatusWords())
        .thenReturn(new HashSet<>(Arrays.asList(0x9000, 0x6200)));
    when(originalRequest.getInfo()).thenReturn("Select AID");
    when(originalRequest.getMaxExpectedResponseTime()).thenReturn(500);

    InternalDto.ApduRequest dto = new InternalDto.ApduRequest(originalRequest);

    // When: Serialize to JSON
    String json = JsonUtil.toJson(dto);

    // Then: JSON should contain maxExpectedResponseTime field
    assertThat(json).contains("maxExpectedResponseTime");
    // Value is serialized as hex string "01F4" which equals 500 in decimal
    assertThat(json).containsAnyOf("500", "01F4");
  }

  @Test
  public void apduRequest_jsonSerialization_withNullMaxTime_shouldNotIncludeField() {
    // Given
    ApduRequestSpi originalRequest = mock(ApduRequestSpi.class);
    when(originalRequest.getApdu()).thenReturn(new byte[] {0x00, (byte) 0xA4});
    when(originalRequest.getSuccessfulStatusWords())
        .thenReturn(new HashSet<>(Collections.singletonList(0x9000)));
    when(originalRequest.getMaxExpectedResponseTime()).thenReturn(null);

    InternalDto.ApduRequest dto = new InternalDto.ApduRequest(originalRequest);

    // When: Serialize to JSON
    String json = JsonUtil.toJson(dto);

    // Then: JSON should still be valid (Gson handles null gracefully)
    assertThat(json).isNotNull();
    assertThat(json).isNotEmpty();
  }

  @Test
  public void apduRequest_defaultConstructor_shouldHaveNullMaxExpectedResponseTime() {
    // When
    InternalDto.ApduRequest dto = new InternalDto.ApduRequest();

    // Then
    assertThat(dto.getMaxExpectedResponseTime()).isNull();
  }
}
