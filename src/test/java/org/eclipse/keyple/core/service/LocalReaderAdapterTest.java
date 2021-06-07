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
import static org.mockito.Mockito.*;

import java.util.*;
import org.calypsonet.terminal.card.CardBrokenCommunicationException;
import org.calypsonet.terminal.card.CardSelectionResponseApi;
import org.calypsonet.terminal.card.ChannelControl;
import org.calypsonet.terminal.card.ReaderBrokenCommunicationException;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectorSpi;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalReaderAdapterTest {
  private ReaderSpiMock readerSpi;
  private CardSelectorMock cardSelector;
  private CardSelectionRequestSpi cardSelectionRequestSpi;

  private static final String PLUGIN_NAME = "plugin";
  private static final String READER_NAME = "reader";
  private static final String CARD_PROTOCOL = "cardProtocol";
  private static final String OTHER_CARD_PROTOCOL = "otherCardProtocol";
  private static final String POWER_ON_DATA = "12345678";

  interface ReaderSpiMock extends KeypleReaderExtension, ReaderSpi {}

  interface CardSelectorMock extends CardSelectorSpi {}

  @Before
  public void setUp() throws Exception {
    readerSpi = mock(ReaderSpiMock.class);
    when(readerSpi.getName()).thenReturn(READER_NAME);
    when(readerSpi.checkCardPresence()).thenReturn(true);
    when(readerSpi.getPowerOnData()).thenReturn(POWER_ON_DATA);
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(ByteArrayUtil.fromHex("6D00"));
    when(readerSpi.isProtocolSupported(CARD_PROTOCOL)).thenReturn(true);
    when(readerSpi.isCurrentProtocol(CARD_PROTOCOL)).thenReturn(true);

    cardSelector = mock(CardSelectorMock.class);
    when(cardSelector.getFileOccurrence()).thenReturn(CardSelectorSpi.FileOccurrence.FIRST);
    when(cardSelector.getFileControlInformation())
        .thenReturn(CardSelectorSpi.FileControlInformation.FCI);
    when(cardSelector.getSuccessfulSelectionStatusWords())
        .thenReturn(Collections.singleton(0x9000));

    cardSelectionRequestSpi = mock(CardSelectionRequestSpi.class);
  }

  @After
  public void tearDown() {}

  @Test
  public void getReaderSpi_shouldReturnReaderSpi() {
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    assertThat(localReaderAdapter.getReaderSpi()).isEqualTo(readerSpi);
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
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).getPowerOnData()).isEqualTo(POWER_ON_DATA);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isTrue();
  }

  @Test
  public void
      transmitCardSelectionRequests_withPermissiveCardSelectorAndProcessALL_shouldReturnMatchingResponseAndNotOpenChannel()
          throws Exception {
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.PROCESS_ALL,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).getPowerOnData()).isEqualTo(POWER_ON_DATA);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withNonMatchingPowerOnDataFilteringCardSelector_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    when(cardSelector.getPowerOnDataRegex()).thenReturn("FAILINGREGEX");
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).getPowerOnData()).isEqualTo(POWER_ON_DATA);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withNonMatchingDFNameFilteringCardSelector_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    when(cardSelector.getAid()).thenReturn(ByteArrayUtil.fromHex("1122334455"));
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).getPowerOnData()).isEqualTo(POWER_ON_DATA);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withMatchingDFNameFilteringCardSelector_shouldReturnMatchingResponseAndOpenChannel()
          throws Exception {
    byte[] selectResponseApdu = ByteArrayUtil.fromHex("123456789000");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);
    when(cardSelector.getAid()).thenReturn(ByteArrayUtil.fromHex("1122334455"));
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).getPowerOnData()).isEqualTo(POWER_ON_DATA);
    assertThat(cardSelectionResponses.get(0).getSelectApplicationResponse().getApdu())
        .isEqualTo(selectResponseApdu);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isTrue();
  }

  @Test
  public void
      transmitCardSelectionRequests_withMatchingDFNameFilteringCardSelectorInvalidatedRejected_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    byte[] selectResponseApdu = ByteArrayUtil.fromHex("123456786283");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);
    when(cardSelector.getAid()).thenReturn(ByteArrayUtil.fromHex("1122334455"));
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).getPowerOnData()).isEqualTo(POWER_ON_DATA);
    assertThat(cardSelectionResponses.get(0).getSelectApplicationResponse().getApdu())
        .isEqualTo(selectResponseApdu);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void
      transmitCardSelectionRequests_withMatchingDFNameFilteringCardSelectorInvalidatedAccepted_shouldReturnMatchingResponseAndOpenChannel()
          throws Exception {
    byte[] selectResponseApdu = ByteArrayUtil.fromHex("123456786283");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);
    when(cardSelector.getAid()).thenReturn(ByteArrayUtil.fromHex("1122334455"));
    when(cardSelector.getSuccessfulSelectionStatusWords())
        .thenReturn(new HashSet<Integer>(Arrays.asList(0x9000, 0x6283)));
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).getPowerOnData()).isEqualTo(POWER_ON_DATA);
    assertThat(cardSelectionResponses.get(0).getSelectApplicationResponse().getApdu())
        .isEqualTo(selectResponseApdu);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isTrue();
  }

  @Test
  public void
      transmitCardSelectionRequests_withNonMatchingCardProtocolFilteringCardSelector_shouldReturnNotMatchingResponseAndNotOpenChannel()
          throws Exception {
    when(cardSelector.getCardProtocol()).thenReturn(OTHER_CARD_PROTOCOL);
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.activateProtocol(CARD_PROTOCOL, CARD_PROTOCOL);
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.CLOSE_AFTER);
    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  @Test(expected = ReaderBrokenCommunicationException.class)
  public void transmitCardSelectionRequests_whenOpenPhysicalThrowsReaderIOException_shouldRCE()
      throws Exception {
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    doThrow(new ReaderIOException("Reader IO Exception")).when(readerSpi).openPhysicalChannel();

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelectionRequestSpi>(Collections.singletonList(cardSelectionRequestSpi)),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.CLOSE_AFTER);
  }

  @Test(expected = CardBrokenCommunicationException.class)
  public void transmitCardSelectionRequests_whenOpenPhysicalThrowsCArdIOException_shouldCCE()
      throws Exception {
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    doThrow(new CardIOException("Card IO Exception")).when(readerSpi).openPhysicalChannel();

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelectionRequestSpi>(Collections.singletonList(cardSelectionRequestSpi)),
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
