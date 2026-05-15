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

import org.eclipse.keyple.core.service.util.ControllableReaderSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.slf4j.Logger;

public class ObservableLocalReaderSuite {

  private ObservableLocalReaderAdapter reader;
  private ReaderObserverSpiMock observer;
  private CardReaderObservationExceptionHandlerSpi handler;

  ObservableLocalReaderSuite(
      ObservableLocalReaderAdapter reader,
      ControllableReaderSpiMock readerSpi,
      ReaderObserverSpiMock observer,
      CardReaderObservationExceptionHandlerSpi handler,
      Logger logger) {
    this.reader = reader;
    this.observer = observer;
    this.handler = handler;
  }

  public void addFirstObserver_should_startDetection() {
    reader.setReaderObservationExceptionHandler(handler);
    reader.addObserver(observer);
    reader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING);
    assertThat(reader.countObservers()).isEqualTo(1);
  }

  public void removeLastObserver_shoul_StopDetection() {
    addFirstObserver_should_startDetection();
    reader.removeObserver(observer);
    assertThat(reader.countObservers()).isZero();
  }

  public void clearObservers_shouldRemove_allObservers() {
    addFirstObserver_should_startDetection();
    reader.clearObservers();
    assertThat(reader.countObservers()).isZero();
  }

  void setObserver(ReaderObserverSpiMock observer) {
    this.observer = observer;
  }
}
