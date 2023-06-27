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
import org.eclipse.keyple.core.service.util.ObservableReaderAsynchronousSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.eclipse.keypop.card.CardResponseApi;
import org.eclipse.keypop.card.CardSelectionResponseApi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservableLocalReaderAsynchronousAdapterTest {
  private static final Logger logger =
      LoggerFactory.getLogger(ObservableLocalReaderAsynchronousAdapterTest.class);

  ObservableLocalReaderAdapter reader;
  ObservableReaderAsynchronousSpiMock readerSpi;
  ReaderObserverSpiMock observer;
  CardReaderObservationExceptionHandlerSpi handler;
  ObservableLocalReaderSuite testSuite;
  ExecutorService notificationExecutorService;

  CardSelectionRequestSpi cardSelectionRequestSpi;
  CardSelectionResponseApi cardSelectionResponseApi;
  CardResponseApi cardResponseApi;
  CardReaderEvent event;
  @Before
  public void seTup() {
    notificationExecutorService = Executors.newSingleThreadExecutor();
    readerSpi = Mockito.spy(new ObservableReaderAsynchronousSpiMock(READER_NAME));
    handler = Mockito.spy(CardReaderObservationExceptionHandlerSpi.class);
    reader = new ObservableLocalReaderAdapter(readerSpi, PLUGIN_NAME);
    observer = new ReaderObserverSpiMock(null);
    testSuite = new ObservableLocalReaderSuite(reader, readerSpi, observer, handler, logger);
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
    testSuite.addFirstObserver_should_startDetection();
  }

  @Test
  public void removeObserver() {
    testSuite.removeLastObserver_shoul_StopDetection();
  }

  @Test
  public void clearObservers() {
    testSuite.clearObservers_shouldRemove_allObservers();
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
}
