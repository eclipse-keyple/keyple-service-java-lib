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
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.card.*;
import org.eclipse.keypop.card.spi.ApduRequestSpi;
import org.eclipse.keypop.card.spi.CardRequestSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.ReaderCommunicationException;
import org.eclipse.keypop.reader.ReaderProtocolNotSupportedException;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

public class LocalReaderAdapterTest {
  private ReaderAdapterTestUtils.ReaderSpiMock readerSpi;
  private CardSelector<?> cardSelector;
  private CardSelectionRequestSpi cardSelectionRequestSpi;
  private CardRequestSpi cardRequestSpi;
  private ApduRequestSpi apduRequestSpi;

  @Before
  public void setUp() throws Exception {
    readerSpi = ReaderAdapterTestUtils.getReaderSpi();
    cardSelector =
        SmartCardServiceProvider.getService().getReaderApiFactory().createBasicCardSelector();
    cardSelectionRequestSpi = mock(CardSelectionRequestSpi.class);
    when(cardSelectionRequestSpi.getSuccessfulSelectionStatusWords())
        .thenReturn(new HashSet<Integer>(Collections.singletonList(0x9000)));

    apduRequestSpi = mock(ApduRequestSpi.class);
    when(apduRequestSpi.getSuccessfulStatusWords())
        .thenReturn(new HashSet<Integer>(Collections.singletonList(0x9000)));
    cardRequestSpi = mock(CardRequestSpi.class);
    when(cardRequestSpi.getApduRequests()).thenReturn(Collections.singletonList(apduRequestSpi));
  }

  @After
  public void tearDown() {}

  @Test
  public void getReaderSpi_shouldReturnReaderSpi() {
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    assertThat(localReaderAdapter.getReaderSpi()).isEqualTo(readerSpi);
  }

  /*
   * Transmit card selections operations
   */
  @Test
  public void
      transmitCardSelectionRequests_withPermissiveCardSelector_shouldReturnMatchingResponseAndOpenChannel()
          throws Exception {
    // TODO check this when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            Collections.<CardSelector<?>>singletonList(cardSelector),
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

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
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
    cardSelector.filterByPowerOnData("FAILINGREGEX");

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
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
    cardSelector =
        SmartCardServiceProvider.getService()
            .getReaderApiFactory()
            .createIsoCardSelector()
            .filterByDfName("1122334455");

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
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
    byte[] selectResponseApdu = HexUtil.toByteArray("123456789000");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);
    cardSelector =
        SmartCardServiceProvider.getService()
            .getReaderApiFactory()
            .createIsoCardSelector()
            .filterByDfName("1122334455");

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
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
    byte[] selectResponseApdu = HexUtil.toByteArray("123456786283");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);
    cardSelector =
        SmartCardServiceProvider.getService()
            .getReaderApiFactory()
            .createIsoCardSelector()
            .filterByDfName("1122334455");

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
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
    byte[] selectResponseApdu = HexUtil.toByteArray("123456786283");
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(selectResponseApdu);
    cardSelector =
        SmartCardServiceProvider.getService()
            .getReaderApiFactory()
            .createIsoCardSelector()
            .filterByDfName("1122334455");
    when(cardSelectionRequestSpi.getSuccessfulSelectionStatusWords())
        .thenReturn(new HashSet<Integer>(Arrays.asList(0x9000, 0x6283)));

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
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
    cardSelector.filterByCardProtocol(OTHER_CARD_PROTOCOL);

    LocalConfigurableReaderAdapter localReaderAdapter =
        new LocalConfigurableReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.activateProtocol(CARD_PROTOCOL, CARD_PROTOCOL);
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
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

    doThrow(new ReaderIOException("Reader IO Exception")).when(readerSpi).openPhysicalChannel();

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
        new ArrayList<CardSelectionRequestSpi>(Collections.singletonList(cardSelectionRequestSpi)),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.CLOSE_AFTER);
  }

  @Test(expected = CardBrokenCommunicationException.class)
  public void transmitCardSelectionRequests_whenOpenPhysicalThrowsCArdIOException_shouldCCE()
      throws Exception {

    doThrow(new CardIOException("Card IO Exception")).when(readerSpi).openPhysicalChannel();

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
        new ArrayList<CardSelectionRequestSpi>(Collections.singletonList(cardSelectionRequestSpi)),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.CLOSE_AFTER);
  }

  @Test(expected = CardBrokenCommunicationException.class)
  public void transmitCardSelectionRequests_whenTransmitApduThrowsCardIOException_shouldCCE()
      throws Exception {
    cardSelector =
        SmartCardServiceProvider.getService()
            .getReaderApiFactory()
            .createIsoCardSelector()
            .filterByDfName("12341234");
    doThrow(new CardIOException("Card IO Exception"))
        .when(readerSpi)
        .transmitApdu(ArgumentMatchers.<byte[]>any());

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
        new ArrayList<CardSelectionRequestSpi>(Collections.singletonList(cardSelectionRequestSpi)),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.CLOSE_AFTER);
  }

  @Test(expected = ReaderBrokenCommunicationException.class)
  public void transmitCardSelectionRequests_whenTransmitApduThrowsReaderIOException_shouldRCE()
      throws Exception {
    cardSelector =
        SmartCardServiceProvider.getService()
            .getReaderApiFactory()
            .createIsoCardSelector()
            .filterByDfName("12341234");
    doThrow(new ReaderIOException("Reader IO Exception"))
        .when(readerSpi)
        .transmitApdu(ArgumentMatchers.<byte[]>any());

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    localReaderAdapter.transmitCardSelectionRequests(
        new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
        new ArrayList<CardSelectionRequestSpi>(Collections.singletonList(cardSelectionRequestSpi)),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.CLOSE_AFTER);
  }

  @Test
  public void transmitCardSelectionRequests_whenFirstMatchAndSecondSelectionFails_shouldNotMatch()
      throws Exception {

    cardSelector =
        SmartCardServiceProvider.getService()
            .getReaderApiFactory()
            .createIsoCardSelector()
            .filterByDfName("12341234");

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();

    // first successful selection
    when(readerSpi.transmitApdu(any(byte[].class)))
        .thenReturn(HexUtil.toByteArray("AABBCCDDEE9000"));

    List<CardSelectionResponseApi> cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.KEEP_OPEN);

    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isTrue();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isTrue();

    // second not matching selection
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(HexUtil.toByteArray("6B00"));

    cardSelectionResponses =
        localReaderAdapter.transmitCardSelectionRequests(
            new ArrayList<CardSelector<?>>(Collections.singletonList(cardSelector)),
            new ArrayList<CardSelectionRequestSpi>(
                Collections.singletonList(cardSelectionRequestSpi)),
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.KEEP_OPEN);

    assertThat(cardSelectionResponses).hasSize(1);
    assertThat(cardSelectionResponses.get(0).hasMatched()).isFalse();
    assertThat(localReaderAdapter.isLogicalChannelOpen()).isFalse();
  }

  /*
   * Transmit card request
   */

  @Test
  public void transmitCardRequest_shouldReturnResponse() throws Exception {
    byte[] responseApdu = HexUtil.toByteArray("123456786283");
    byte[] requestApdu = HexUtil.toByteArray("0000");

    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(responseApdu);
    when(apduRequestSpi.getApdu()).thenReturn(requestApdu);

    LocalConfigurableReaderAdapter localReaderAdapter =
        new LocalConfigurableReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.activateProtocol(CARD_PROTOCOL, CARD_PROTOCOL);
    assertThat(localReaderAdapter.isCardPresent()).isTrue();
    CardResponseApi cardResponse =
        localReaderAdapter.transmitCardRequest(cardRequestSpi, ChannelControl.CLOSE_AFTER);

    assertThat(cardResponse.getApduResponses().iterator().next().getApdu()).isEqualTo(responseApdu);
    assertThat(cardResponse.isLogicalChannelOpen()).isFalse();
  }

  @Test
  public void transmitCardRequest_isCase4() throws Exception {
    byte[] requestApdu = HexUtil.toByteArray("11223344041234567802");
    byte[] responseApdu = HexUtil.toByteArray("9000");
    byte[] getResponseRApdu = HexUtil.toByteArray("00C0000002");
    byte[] getResponseCApdu = HexUtil.toByteArray("00009000");
    when(apduRequestSpi.getApdu()).thenReturn(requestApdu);
    when(readerSpi.transmitApdu(requestApdu)).thenReturn(responseApdu);
    when(readerSpi.transmitApdu(getResponseRApdu)).thenReturn(getResponseCApdu);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    CardResponseApi response =
        localReaderAdapter.transmitCardRequest(cardRequestSpi, ChannelControl.CLOSE_AFTER);
    assertThat(response.getApduResponses().get(0).getApdu()).isEqualTo(getResponseCApdu);
  }

  @Test(expected = UnexpectedStatusWordException.class)
  public void transmitCardRequest_withUnsuccessfulStatusWord_shouldThrow_USW() throws Exception {
    byte[] responseApdu = HexUtil.toByteArray("123456789000");
    byte[] requestApdu = HexUtil.toByteArray("0000");

    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(responseApdu);
    when(apduRequestSpi.getApdu()).thenReturn(requestApdu);
    when(apduRequestSpi.getSuccessfulStatusWords())
        .thenReturn(new HashSet<Integer>(Collections.singletonList(0x9001)));
    when(cardRequestSpi.stopOnUnsuccessfulStatusWord()).thenReturn(true);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.transmitCardRequest(cardRequestSpi, ChannelControl.CLOSE_AFTER);
  }

  @Test(expected = CardBrokenCommunicationException.class)
  public void transmitCardRequest_withCardExceptionOnTransmit_shouldThrow_CBCE() throws Exception {
    byte[] requestApdu = HexUtil.toByteArray("0000");

    when(readerSpi.transmitApdu(any(byte[].class))).thenThrow(new CardIOException(""));
    when(apduRequestSpi.getApdu()).thenReturn(requestApdu);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.transmitCardRequest(cardRequestSpi, ChannelControl.CLOSE_AFTER);
  }

  @Test(expected = ReaderBrokenCommunicationException.class)
  public void transmitCardRequest_withCardExceptionOnTransmit_shouldThrow_RBCE() throws Exception {
    byte[] requestApdu = HexUtil.toByteArray("0000");

    when(readerSpi.transmitApdu(any(byte[].class))).thenThrow(new ReaderIOException(""));
    when(apduRequestSpi.getApdu()).thenReturn(requestApdu);

    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.transmitCardRequest(cardRequestSpi, ChannelControl.CLOSE_AFTER);
  }

  /*
   * active protocol operations
   */

  @Test
  public void deActivateProtocol_shouldInvoke_deActivateProcotol_OnReaderSpi() throws Exception {
    ConfigurableReaderSpi spy = getReaderSpiSpy();
    LocalConfigurableReaderAdapter localReaderAdapter =
        new LocalConfigurableReaderAdapter(spy, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.activateProtocol(CARD_PROTOCOL, CARD_PROTOCOL);
    localReaderAdapter.deactivateProtocol(CARD_PROTOCOL);
    verify(spy, times(1)).deactivateProtocol(CARD_PROTOCOL);
  }

  @Test(expected = ReaderProtocolNotSupportedException.class)
  public void activateProtocol_whileNotSupported_should_RPNS() throws Exception {
    ConfigurableReaderSpi spy = getReaderSpiSpy();
    when(spy.isProtocolSupported(ArgumentMatchers.<String>any())).thenReturn(false);
    LocalConfigurableReaderAdapter localReaderAdapter =
        new LocalConfigurableReaderAdapter(spy, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.activateProtocol(CARD_PROTOCOL, CARD_PROTOCOL);
  }

  @Test(expected = ReaderProtocolNotSupportedException.class)
  public void deActivateProtocol_whileNotSupported_should_RPNS() throws Exception {
    ConfigurableReaderSpi spy = getReaderSpiSpy();
    LocalConfigurableReaderAdapter localReaderAdapter =
        new LocalConfigurableReaderAdapter(spy, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.activateProtocol(CARD_PROTOCOL, CARD_PROTOCOL);
    when(spy.isProtocolSupported(ArgumentMatchers.<String>any())).thenReturn(false);
    localReaderAdapter.deactivateProtocol(CARD_PROTOCOL);
  }

  /*
   * Misc operations
   */

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

  @Test
  public void closeLogicalAndPhysicalChannelsSilently_withException_does_not_propagate()
      throws Exception {
    doThrow(new ReaderIOException("")).when(readerSpi).closePhysicalChannel();
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.closeLogicalAndPhysicalChannelsSilently();
    // no exception is propagated
  }

  @Test(expected = ReaderBrokenCommunicationException.class)
  public void releaseChannel_withException_throwRBCE() throws Exception {
    doThrow(new ReaderIOException("")).when(readerSpi).closePhysicalChannel();
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.releaseChannel();
    // exception is thrown
  }

  @Test(expected = ReaderCommunicationException.class)
  public void isCardPresent_whenReaderSpiFails_shouldKRCE() throws Exception {
    doThrow(new ReaderIOException("Reader IO Exception")).when(readerSpi).checkCardPresence();
    LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, PLUGIN_NAME);
    localReaderAdapter.register();
    localReaderAdapter.isCardPresent();
  }
}
