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
import static org.calypsonet.terminal.reader.ObservableCardReader.NotificationMode.ALWAYS;
import static org.calypsonet.terminal.reader.ObservableCardReader.NotificationMode.MATCHED_ONLY;
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.*;
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.MULTI_NOT_MATCHING_RESPONSES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import org.calypsonet.terminal.card.*;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.util.ObservableReaderAsynchronousSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservableLocalReaderSelectionScenarioTest {

  private static final Logger logger =
      LoggerFactory.getLogger(ObservableLocalReaderSelectionScenarioTest.class);

  ObservableLocalReaderAdapter readerSpy;
  ObservableReaderAsynchronousSpiMock readerSpi;
  ReaderObserverSpiMock observer;
  CardReaderObservationExceptionHandlerSpi handler;
  ObservableLocalReaderSuite testSuite;

  CardSelectionRequestSpi cardSelectionRequestSpi;
  CardSelectionResponseApi cardSelectionResponseApi;
  CardResponseApi cardResponseApi;
  ReaderEvent event;

  @Before
  public void seTup() {
    readerSpi = Mockito.spy(new ObservableReaderAsynchronousSpiMock(READER_NAME));
    handler = Mockito.spy(CardReaderObservationExceptionHandlerSpi.class);
    readerSpy = spy(new ObservableLocalReaderAdapter(readerSpi, PLUGIN_NAME));
    observer = new ReaderObserverSpiMock(null);
    testSuite = new ObservableLocalReaderSuite(readerSpy, readerSpi, observer, handler, logger);
    cardSelectionRequestSpi = mock(CardSelectionRequestSpi.class);
    cardSelectionResponseApi = mock(CardSelectionResponseApi.class);
    cardResponseApi = mock(CardResponseApi.class);

    // test with event notification executor service
    readerSpy.register();
    readerSpy.setReaderObservationExceptionHandler(handler);
  }

  @Test
  public void process_card_with_no_scenario_return_cardInserted() {
    // scenario : no scenario scheduled

    event = readerSpy.processCardInserted();

    // verify ScheduledCardSelectionsResponseAdapter in event
    assertEventIs(CardReaderEvent.Type.CARD_INSERTED);
  }

  @Test
  public void process_card_with_matching_selections_return_cardMatched() throws Exception {
    // scenario : selection match
    mockReaderWithSelectionResponses(MATCHING_RESPONSES, MATCHED_ONLY);

    event = readerSpy.processCardInserted();

    // verify ScheduledCardSelectionsResponseAdapter in event
    assertEventIs(CardReaderEvent.Type.CARD_MATCHED);
    assertThat(event.getScheduledCardSelectionsResponse()).isNotNull();
  }

  @Test
  public void process_card_with_no_matching_selections_with_Always_return_CardInserted()
      throws Exception {
    // scenario : selection no match
    mockReaderWithSelectionResponses(NOT_MATCHING_RESPONSES, ALWAYS);

    event = readerSpy.processCardInserted();

    assertEventIs(CardReaderEvent.Type.CARD_INSERTED);
  }

  @Test
  public void process_card_with_no_matching_selections_with_MatchedOnly_return_null()
      throws Exception {
    // scenario : selection no match
    mockReaderWithSelectionResponses(NOT_MATCHING_RESPONSES, MATCHED_ONLY);

    event = readerSpy.processCardInserted();

    assertThat(event).isNull();
  }

  @Test
  public void process_card_with_multi_matching_selections_return_CardMatched() throws Exception {
    // scenario : first no match, second match, third match
    mockReaderWithSelectionResponses(MULTI_MATCHING_RESPONSES, MATCHED_ONLY);

    event = readerSpy.processCardInserted();

    assertEventIs(CardReaderEvent.Type.CARD_MATCHED);
    assertThat(event.getScheduledCardSelectionsResponse()).isNotNull();
    assertThat(event.getScheduledCardSelectionsResponse())
        .isOfAnyClassIn(ScheduledCardSelectionsResponseAdapter.class);
    assertThat(
            ((ScheduledCardSelectionsResponseAdapter) event.getScheduledCardSelectionsResponse())
                .getCardSelectionResponses())
        .size()
        .isEqualTo(3);
    // todo check this
  }

  @Test
  public void process_card_with_multi_not_matching_selections_with_matchedOnly_return_null()
      throws Exception {
    // scenario : first no match, second no match
    mockReaderWithSelectionResponses(MULTI_NOT_MATCHING_RESPONSES, MATCHED_ONLY);

    event = readerSpy.processCardInserted();

    assertThat(event).isNull();
  }

  @Test
  public void process_card_with_throw_reader_exception_scenario_return_null() throws Exception {
    doThrow(new ReaderBrokenCommunicationException(null, true, "", new RuntimeException()))
        .when(readerSpy)
        .transmitCardSelectionRequests(
            any(List.class), any(MultiSelectionProcessing.class), any(ChannelControl.class));

    readerSpy.scheduleCardSelectionScenario(
        new CardSelectionScenarioAdapter(
            Collections.singletonList(cardSelectionRequestSpi),
            MultiSelectionProcessing.PROCESS_ALL,
            ChannelControl.KEEP_OPEN),
        MATCHED_ONLY,
        ObservableCardReader.DetectionMode.SINGLESHOT);

    event = readerSpy.processCardInserted();

    assertThat(event).isNull();
  }

  @Test
  public void process_card_with_throw_card_exception_scenario_return_null() throws Exception {
    doThrow(new CardBrokenCommunicationException(null, true, "", new RuntimeException()))
        .when(readerSpy)
        .transmitCardSelectionRequests(
            any(List.class), any(MultiSelectionProcessing.class), any(ChannelControl.class));

    readerSpy.scheduleCardSelectionScenario(
        new CardSelectionScenarioAdapter(
            Collections.singletonList(cardSelectionRequestSpi),
            MultiSelectionProcessing.PROCESS_ALL,
            ChannelControl.KEEP_OPEN),
        MATCHED_ONLY,
        ObservableCardReader.DetectionMode.SINGLESHOT);

    event = readerSpy.processCardInserted();

    assertThat(event).isNull();
  }

  /**
   * Configure card selection scenario and mock selection responses
   *
   * @param cardSelectionResponse
   * @param notificationMode
   * @throws Exception
   */
  private void mockReaderWithSelectionResponses(
      List<CardSelectionResponseApi> cardSelectionResponse,
      ObservableCardReader.NotificationMode notificationMode)
      throws Exception {

    doReturn(cardSelectionResponse)
        .when(readerSpy)
        .transmitCardSelectionRequests(
            any(List.class), any(MultiSelectionProcessing.class), any(ChannelControl.class));

    readerSpy.scheduleCardSelectionScenario(
        new CardSelectionScenarioAdapter(
            Collections.singletonList(cardSelectionRequestSpi),
            MultiSelectionProcessing.PROCESS_ALL,
            ChannelControl.KEEP_OPEN),
        notificationMode,
        ObservableCardReader.DetectionMode.SINGLESHOT);
  }

  /**
   * assert event is of type
   *
   * @param type
   */
  private void assertEventIs(CardReaderEvent.Type type) {
    assertThat(event).isNotNull();
    assertThat(event.getType()).isEqualTo(type);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);
  }
}
