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
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.getReaderSpi;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.card.CardResponseApi;
import org.eclipse.keypop.card.ChannelControl;
import org.eclipse.keypop.card.spi.ApduRequestSpi;
import org.eclipse.keypop.card.spi.CardRequestSpi;
import org.junit.Before;
import org.junit.Test;

public class LocalReaderAdapterResponseTimeTest {

  private ReaderAdapterTestUtils.ReaderSpiMock readerSpi;
  private LocalReaderAdapter localReaderAdapter;
  private ApduRequestSpi apduRequest;
  private CardRequestSpi cardRequest;

  @Before
  public void setUp() throws Exception {
    readerSpi = getReaderSpi();
    when(readerSpi.isContactless()).thenReturn(true);
    when(readerSpi.isPhysicalChannelOpen()).thenReturn(true);

    localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();

    apduRequest = mock(ApduRequestSpi.class);
    when(apduRequest.getApdu()).thenReturn(HexUtil.toByteArray("00A4040005AABBCCDDEE00"));
    when(apduRequest.getSuccessfulStatusWords())
        .thenReturn(new HashSet<>(Collections.singletonList(0x9000)));
    when(apduRequest.getInfo()).thenReturn("Test APDU");

    cardRequest = mock(CardRequestSpi.class);
    when(cardRequest.getApduRequests()).thenReturn(Collections.singletonList(apduRequest));
    when(cardRequest.stopOnUnsuccessfulStatusWord()).thenReturn(false);
  }

  @Test
  public void processCardRequest_withNullMaxExpectedTime_shouldNotValidate() throws Exception {
    // Given: No max time constraint
    when(apduRequest.getMaxExpectedResponseTime()).thenReturn(null);
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(HexUtil.toByteArray("123456789000"));

    // When
    CardResponseApi response =
        localReaderAdapter.processCardRequest(cardRequest, ChannelControl.KEEP_OPEN);

    // Then: Should succeed without exception
    assertThat(response).isNotNull();
    assertThat(response.getApduResponses()).hasSize(1);
  }

  @Test
  public void processCardRequest_responseTimeIsRecorded() throws Exception {
    // Given
    when(apduRequest.getMaxExpectedResponseTime()).thenReturn(1000); // 1 second max
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(HexUtil.toByteArray("123456789000"));

    // When
    CardResponseApi response =
        localReaderAdapter.processCardRequest(cardRequest, ChannelControl.KEEP_OPEN);

    // Then: Response should have a response time recorded
    assertThat(response.getApduResponses()).hasSize(1);
    assertThat(response.getApduResponses().get(0).getResponseTime()).isNotNull();
    assertThat(response.getApduResponses().get(0).getResponseTime()).isGreaterThanOrEqualTo(0);
  }

  @Test
  public void processCardRequest_withVeryHighMaxTime_shouldAlwaysSucceed() throws Exception {
    // Given: Very high max time (1 hour)
    when(apduRequest.getMaxExpectedResponseTime()).thenReturn(3600000);
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(HexUtil.toByteArray("123456789000"));

    // When
    CardResponseApi response =
        localReaderAdapter.processCardRequest(cardRequest, ChannelControl.KEEP_OPEN);

    // Then: Should succeed
    assertThat(response).isNotNull();
    assertThat(response.getApduResponses()).hasSize(1);
    assertThat(response.getApduResponses().get(0).getResponseTime())
        .isLessThan(3600000); // Much less than 1 hour
  }
}
