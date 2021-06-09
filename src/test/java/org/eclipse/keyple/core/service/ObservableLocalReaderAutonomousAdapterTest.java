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
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.READER_NAME_1;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  /*
   *  With ObservableReaderAutonomousSpi
   */
  @Before
  public void seTup() {
    notificationExecutorService = Executors.newSingleThreadExecutor();
    readerSpi = new ObservableReaderAutonomousSpiMock(READER_NAME_1);
    handler = Mockito.spy(CardReaderObservationExceptionHandlerSpi.class);
    reader = new ObservableLocalReaderAdapter(readerSpi, PLUGIN_NAME);
    observer = new ReaderObserverSpiMock(null);
    testSuite = new ObservableLocalReaderSuiteTest(reader, readerSpi, observer, handler, logger);

    // test with event notification executor service
    reader.register();
    reader.setEventNotificationExecutorService(notificationExecutorService);
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
    verify(handler, times(1)).onReaderObservationError(anyString(), eq(READER_NAME_1), eq(e));
  }
}
