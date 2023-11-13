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
package org.eclipse.keyple.core.service.util;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterNonBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterNonBlockingSpi;

public class ObservableReaderNonBlockingSpiMock
    implements KeypleReaderExtension,
        ConfigurableReaderSpi,
        ObservableReaderSpi,
        CardInsertionWaiterNonBlockingSpi,
        CardRemovalWaiterNonBlockingSpi,
        ControllableReaderSpiMock {

  boolean detectionStarted;
  boolean physicalChannelOpen;
  AtomicBoolean cardPresent;
  String name;

  public ObservableReaderNonBlockingSpiMock(String name) {
    this.detectionStarted = false;
    this.name = name;
    this.physicalChannelOpen = false;
    this.cardPresent = new AtomicBoolean(false);
  }

  @Override
  public void onStartDetection() {
    detectionStarted = true;
  }

  @Override
  public void onStopDetection() {
    detectionStarted = false;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isProtocolSupported(String readerProtocol) {
    return true;
  }

  @Override
  public void activateProtocol(String readerProtocol) {}

  @Override
  public void deactivateProtocol(String readerProtocol) {}

  @Override
  public boolean isCurrentProtocol(String readerProtocol) {
    return false;
  }

  @Override
  public void openPhysicalChannel() throws ReaderIOException, CardIOException {
    physicalChannelOpen = true;
  }

  @Override
  public void closePhysicalChannel() throws ReaderIOException {
    physicalChannelOpen = false;
  }

  @Override
  public boolean isPhysicalChannelOpen() {
    return physicalChannelOpen;
  }

  @Override
  public boolean checkCardPresence() throws ReaderIOException {
    return cardPresent.get();
  }

  @Override
  public String getPowerOnData() {
    return "";
  }

  @Override
  public byte[] transmitApdu(byte[] apduIn) throws ReaderIOException, CardIOException {
    if (cardPresent.get()) {
      return new byte[0];
    } else {
      throw new CardIOException("card is not present");
    }
  }

  @Override
  public boolean isContactless() {
    return false;
  }

  @Override
  public void onUnregister() {}

  @Override
  public void setCardPresent(boolean cardPresent) {
    this.cardPresent.set(cardPresent);
  }

  @Override
  public int getCardInsertionMonitoringSleepDuration() {
    return 10;
  }

  @Override
  public int getCardRemovalMonitoringSleepDuration() {
    return 10;
  }
}
