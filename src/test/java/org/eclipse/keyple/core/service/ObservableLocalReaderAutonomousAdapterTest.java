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

import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.calypsonet.terminal.card.*;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.util.ObservableReaderAutonomousSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservableLocalReaderAutonomousAdapterTest {
  private static final Logger logger =
      LoggerFactory.getLogger(ObservableLocalReaderAutonomousAdapterTest.class);

  ObservableLocalReaderAdapter reader;
  ObservableReaderAutonomousSpiMock readerSpi;
  ReaderObserverSpiMock observer;
  CardReaderObservationExceptionHandlerSpi handler;
  ObservableLocalReaderSuiteTest testSuite;
  ExecutorService notificationExecutorService;

  CardSelectionRequestSpi cardSelectionRequestSpi;
  CardSelectionResponseApi cardSelectionResponseApi;
  CardResponseApi cardResponseApi;
  ReaderEvent event;
  /*
   *  With ObservableReaderAutonomousSpi
   */
  @Before
  public void seTup() {
    notificationExecutorService = Executors.newSingleThreadExecutor();
    readerSpi = Mockito.spy(new ObservableReaderAutonomousSpiMock(READER_NAME));
    handler = Mockito.spy(CardReaderObservationExceptionHandlerSpi.class);
    reader = new ObservableLocalReaderAdapter(readerSpi, PLUGIN_NAME);
    observer = new ReaderObserverSpiMock(null);
    testSuite = new ObservableLocalReaderSuiteTest(reader, readerSpi, observer, handler, logger);
    cardSelectionRequestSpi = mock(CardSelectionRequestSpi.class);
    cardSelectionResponseApi = mock(CardSelectionResponseApi.class);
    cardResponseApi = mock(CardResponseApi.class);

    // test with event notification executor service
    reader.register();
  }

  @After
  public void tearDown() {
    reader.unregister();
  }

  @Test
  public void initReader_addObserver_startDetection() {
    testSuite.initReader_addObserver_startDetection();
  }

  @Test
  public void removeObserver() {
    testSuite.removeObserver();
  }

  @Test
  public void clearObservers() {
    testSuite.clearObservers();
  }

  @Test
  public void insertCard_shouldNotify_CardInsertedEvent() {
    testSuite.insertCard_onWaitForCard_shouldNotify_CardInsertedEvent();
  }

  @Test
  public void finalizeCardProcessing_afterInsert_switchState() {
    testSuite.finalizeCardProcessing_afterInsert_switchState();
  }

  @Test
  public void removeCard_afterFinalize_shouldNotify_CardRemoved() {
    testSuite.removeCard_afterFinalize_shouldNotify_CardRemoved();
  }

  @Test
  public void removeCard_beforeFinalize_shouldNotify_CardRemoved() {
    testSuite.removeCard_beforeFinalize_shouldNotify_CardRemoved();
  }

  /*
   * Method of ObservableLocalReaderAdapter
   */
  @Test
  public void observerThrowsError_shouldBe_transferTo_handler() {
    RuntimeException e = new RuntimeException();
    testSuite.setObserver(new ReaderObserverSpiMock(e));
    testSuite.insertCard_onWaitForCard_shouldNotify_CardInsertedEvent();
    verify(handler, times(1)).onReaderObservationError(anyString(), eq(READER_NAME), eq(e));
  }

  /*
  @Test
   public void insertMatchingCard_shouldNotify_CardMatchedEvent() throws Exception {
     CardSelectorSpi cardSelector = ReaderAdapterTestUtils.getCardSelectorSpi();
     when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

     CardSelectionScenarioAdapter cardSelectionScenario = new CardSelectionScenarioAdapter(
             Collections.singletonList(cardSelectionRequestSpi),
             MultiSelectionProcessing.PROCESS_ALL,
             ChannelControl.KEEP_OPEN);

     reader.scheduleCardSelectionScenario(cardSelectionScenario,
             ObservableCardReader.NotificationMode.MATCHED_ONLY,
             ObservableCardReader.DetectionMode.SINGLESHOT);

     testSuite.insertMatchingCard_onWaitForCard_shouldNotify_CardMatchedEvent();
   }
   */

  /*@Test
  public void insertMatchingCard_shouldNotify_CardMatchedEvent() throws Exception {
    CardSelectorSpi cardSelector = ReaderAdapterTestUtils.getCardSelectorSpi();

    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);
    when(cardSelectionResponseApi.getCardResponse()).thenReturn(cardResponseApi);


    CardSelectionScenarioAdapter cardSelectionScenario = new CardSelectionScenarioAdapter(
            Collections.singletonList(cardSelectionRequestSpi),
            MultiSelectionProcessing.PROCESS_ALL,
            ChannelControl.KEEP_OPEN);

    reader.scheduleCardSelectionScenario(cardSelectionScenario,
            ObservableCardReader.NotificationMode.MATCHED_ONLY,
            ObservableCardReader.DetectionMode.SINGLESHOT);

    testSuite.insertMatchingCard_onWaitForCard_shouldNotify_CardMatchedEvent();
  }

  @Test
  public void insertNotMatchingCard_shouldNotify_CardInsertedEvent() throws Exception {
    byte[] selectResponseApdu = ByteArrayUtil.fromHex("123456786283");
    when(readerSpi.transmitApdu(ArgumentMatchers.<byte[]>any())).thenReturn(selectResponseApdu);

    CardSelectorSpi cardSelector = ReaderAdapterTestUtils.getCardSelectorSpi();
    when(cardSelectionRequestSpi.getCardSelector()).thenReturn(cardSelector);

    CardSelectionScenarioAdapter cardSelectionScenario = new CardSelectionScenarioAdapter(
            Collections.singletonList(cardSelectionRequestSpi),
            MultiSelectionProcessing.PROCESS_ALL,
            ChannelControl.KEEP_OPEN);

    reader.scheduleCardSelectionScenario(cardSelectionScenario,
            ObservableCardReader.NotificationMode.ALWAYS,
            ObservableCardReader.DetectionMode.SINGLESHOT);

    testSuite.insertCard_onWaitForCard_shouldNotify_CardInsertedEvent();
  }*/

}
