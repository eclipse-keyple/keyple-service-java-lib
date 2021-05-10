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
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.keyple.core.card.*;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.service.selection.CardSelector;
import org.eclipse.keyple.core.service.selection.MultiSelectionProcessing;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalReaderAdapterTest {
  private ReaderSpiMock readerSpi;

  private static final String PLUGIN_NAME = "plugin";
  private static final String READER_NAME = "reader";
  private static final String CARD_PROTOCOL = "cardProtocol";
  private static final String OTHER_CARD_PROTOCOL = "otherCardProtocol";
  private static final byte[] ATR = new byte[] {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};

  interface ReaderSpiMock extends KeypleReaderExtension, ReaderSpi {}

  @Before
  public void setUp() throws Exception {
    readerSpi = mock(ReaderSpiMock.class);
    when(readerSpi.getName()).thenReturn(READER_NAME);
    when(readerSpi.checkCardPresence()).thenReturn(true);
    when(readerSpi.getAtr()).thenReturn(ATR);
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(ByteArrayUtil.fromHex("6D00"));
    when(readerSpi.isProtocolSupported(CARD_PROTOCOL)).thenReturn(true);
    when(readerSpi.isCurrentProtocol(CARD_PROTOCOL)).thenReturn(true);
  }

  @After
  public void tearDown() {}

  @Test
  public void getPluginName_shouldReturnPluginName() {
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    assertThat(localReaderAdapter.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  @Test
  public void getName_shouldReturnReaderName() {
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    assertThat(localReaderAdapter.getName()).isEqualTo(READER_NAME);
  }

  @Test
  public void getReaderSpi_shouldReturnReaderSpi() {
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    assertThat(localReaderAdapter.getReaderSpi()).isEqualTo(readerSpi);
  }

  @Test
  public void getExtension_whenReaderIsRegistered_shouldReturnExtension() {
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.getExtension(ReaderSpiMock.class)).isEqualTo(readerSpi);
  }

  @Test(expected = IllegalStateException.class)
  public void getExtension_whenReaderIsNotRegistered_shouldISE() {
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.getExtension(ReaderSpiMock.class);
  }

  @Test(expected = KeypleReaderCommunicationException.class)
  public void isCardPresent_whenReaderSpiFails_shouldKRCE() throws Exception {
    doThrow(new ReaderIOException("Reader IO Exception")).when(readerSpi).checkCardPresence();
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.isCardPresent();
  }

  @Test
  public void
      transmitCardSelectionRequests_withPermissiveCardSelector_shouldReturnMatchingResponseAndOpenChannel()
          throws Exception {
    CardSelector cardSelector = CardSelector.builder().build();
    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getAtr()
                .getBytes())
        .isEqualTo(ATR);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isTrue();
  }

  @Test
  public void
      transmitCardSelectionRequests_withPermissiveCardSelectorAndProcessALL_shouldReturnMatchingResponseAndNotOpenChannel()
          throws Exception {
    CardSelector cardSelector = CardSelector.builder().build();
    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.PROCESS_ALL,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getAtr()
                .getBytes())
        .isEqualTo(ATR);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withNonMatchingAtrFilteringCardSelector_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    CardSelector cardSelector = CardSelector.builder().filterByAtr("1A2B3C").build();
    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getAtr()
                .getBytes())
        .isEqualTo(ATR);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withNonMatchingDFNameFilteringCardSelector_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    CardSelector cardSelector = CardSelector.builder().filterByDfName("1122334455").build();

    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getAtr()
                .getBytes())
        .isEqualTo(ATR);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withMatchingDFNameFilteringCardSelector_shouldReturnMatchingResponseAndOpenChannel()
          throws Exception {
    byte[] selectResponseApdu = ByteArrayUtil.fromHex("123456789000");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);

    CardSelector cardSelector = CardSelector.builder().filterByDfName("1122334455").build();

    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getAtr()
                .getBytes())
        .isEqualTo(ATR);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getFci()
                .getBytes())
        .isEqualTo(selectResponseApdu);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isTrue();
  }

  @Test
  public void
      transmitCardSelectionRequests_withMatchingDFNameFilteringCardSelectorInvalidatedRejected_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    byte[] selectResponseApdu = ByteArrayUtil.fromHex("123456786283");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);

    CardSelector cardSelector = CardSelector.builder().filterByDfName("1122334455").build();

    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getAtr()
                .getBytes())
        .isEqualTo(ATR);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getFci()
                .getBytes())
        .isEqualTo(selectResponseApdu);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withMatchingDFNameFilteringCardSelectorInvalidatedAccepted_shouldReturnMatchingResponseAndOpenChannel()
          throws Exception {
    byte[] selectResponseApdu = ByteArrayUtil.fromHex("123456786283");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);

    CardSelector cardSelector =
        CardSelector.builder().filterByDfName("1122334455").addSuccessfulStatusCode(0x6283).build();

    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getAtr()
                .getBytes())
        .isEqualTo(ATR);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .getFci()
                .getBytes())
        .isEqualTo(selectResponseApdu);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isTrue();
  }

  @Test
  public void
      transmitCardSelectionRequests_withNonMatchingCardProtocolFilteringCardSelector_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    CardSelector cardSelector =
        CardSelector.builder().filterByCardProtocol(OTHER_CARD_PROTOCOL).build();

    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.activateProtocol(CARD_PROTOCOL, CARD_PROTOCOL);
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponse> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(
            ((CardSelectionResponse) cardSelectionResponses.get(0))
                .getSelectionStatus()
                .hasMatched())
        .isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test(expected = ReaderCommunicationException.class)
  public void transmitCardSelectionRequests_whenOpenPhysicalThrowsReaderIOException_shouldRCE()
      throws Exception {
    CardSelector cardSelector = CardSelector.builder().build();
    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    doThrow(new ReaderIOException("Reader IO Exception")).when(readerSpi).openPhysicalChannel();

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.CLOSE_AFTER);
  }

  @Test(expected = CardCommunicationException.class)
  public void transmitCardSelectionRequests_whenOpenPhysicalThrowsCArdIOException_shouldCCE()
      throws Exception {
    CardSelector cardSelector = CardSelector.builder().build();
    CardSelectionRequest cardSelectionRequest = new CardSelectionRequest(cardSelector, null);

    doThrow(new CardIOException("Card IO Exception")).when(readerSpi).openPhysicalChannel();

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelectionRequest>(Arrays.asList(cardSelectionRequest)),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.CLOSE_AFTER);
  }

  @Test
  public void isContactless_whenSpiIsContactless_shouldReturnTrue() {
    when(readerSpi.isContactless()).thenReturn(true);
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isContactless()).isTrue();
  }

  @Test
  public void isContactless_whenSpiIsNotContactless_shouldReturnFalse() {
    when(readerSpi.isContactless()).thenReturn(false);
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isContactless()).isFalse();
  }
}
