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
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.READER_NAME;
import static org.mockito.Mockito.mock;

import org.eclipse.keyple.core.service.util.ObservableReaderBlockingSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservableLocalReaderBlockingAdapterTest {

  private static final Logger logger =
      LoggerFactory.getLogger(ObservableLocalReaderBlockingAdapterTest.class);

  ObservableLocalReaderAdapter reader;
  ObservableReaderBlockingSpiMock readerSpi;
  ReaderObserverSpiMock observer;
  CardReaderObservationExceptionHandlerSpi handler;
  ObservableLocalReaderSuite testSuite;

  long waitInsertion = 1000;
  long waitRemoval = 1000;

  @Before
  public void setUp() {
    readerSpi = new ObservableReaderBlockingSpiMock(READER_NAME, waitInsertion, waitRemoval);
    handler = mock(CardReaderObservationExceptionHandlerSpi.class);
    reader = new ObservableLocalReaderAdapter(readerSpi, PLUGIN_NAME);
    observer = new ReaderObserverSpiMock(null);
    testSuite = new ObservableLocalReaderSuite(reader, readerSpi, observer, handler, logger);
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
}
