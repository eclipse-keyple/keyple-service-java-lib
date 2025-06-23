/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.util;

import static java.lang.Thread.sleep;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.TaskCanceledException;
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.CardPresenceMonitorBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi;

public class ObservableReaderBlockingSpiMock
    implements KeypleReaderExtension,
        ConfigurableReaderSpi,
        ObservableReaderSpi,
        CardInsertionWaiterBlockingSpi,
        CardRemovalWaiterBlockingSpi,
        CardPresenceMonitorBlockingSpi,
        ControllableReaderSpiMock {

  boolean detectionStarted;
  boolean physicalChannelOpen;
  AtomicBoolean cardPresent;
  AtomicInteger insertions;
  AtomicInteger removals;
  long waitInsertion;
  long waitRemoval;
  String name;

  public ObservableReaderBlockingSpiMock(String name, long waitInsertion, long waitRemoval) {
    this.detectionStarted = false;
    this.name = name;
    this.physicalChannelOpen = false;
    this.cardPresent = new AtomicBoolean(false);
    this.insertions = new AtomicInteger(0);
    this.removals = new AtomicInteger(0);
    this.waitInsertion = waitInsertion;
    this.waitRemoval = waitRemoval;
  }

  @Override
  public void setCardPresent(boolean cardPresent) {
    this.cardPresent.set(cardPresent);
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
  public void openPhysicalChannel() {
    physicalChannelOpen = true;
  }

  @Override
  public void closePhysicalChannel() {
    physicalChannelOpen = false;
  }

  @Override
  public boolean isPhysicalChannelOpen() {
    return physicalChannelOpen;
  }

  @Override
  public boolean checkCardPresence() {
    return cardPresent.get();
  }

  @Override
  public String getPowerOnData() {
    return "";
  }

  @Override
  public byte[] transmitApdu(byte[] apduIn) throws CardIOException {
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

  /**
   * Wait for a one time card insertion
   *
   * @throws ReaderIOException
   */
  @Override
  public void waitForCardInsertion() throws ReaderIOException {
    try {
      // card is detected after a timeout
      sleep(waitInsertion);
      insertions.incrementAndGet();
    } catch (InterruptedException e) {
    }
    // if card already inserted, throw ex
    if (insertions.get() > 1) {
      throw new ReaderIOException("no card present");
    }
  }

  @Override
  public void stopWaitForCardInsertion() {
    Thread.currentThread().interrupt();
  }

  @Override
  public void waitForCardRemoval() throws ReaderIOException {
    try {
      sleep(waitRemoval);
      // card removal is detected after a timeout
      removals.incrementAndGet();
    } catch (InterruptedException e) {
    }
    if (removals.get() > 1) {
      throw new ReaderIOException("card not removed ?!");
    }
  }

  @Override
  public void stopWaitForCardRemoval() {
    Thread.currentThread().interrupt();
  }

  @Override
  public void monitorCardPresenceDuringProcessing()
      throws ReaderIOException, TaskCanceledException {
    waitForCardRemoval();
  }

  @Override
  public void stopCardPresenceMonitoringDuringProcessing() {
    stopWaitForCardRemoval();
  }
}
