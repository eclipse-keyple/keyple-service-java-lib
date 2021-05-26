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
package org.eclipse.keyple.core.service.util;

import static java.lang.Thread.sleep;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.TaskCanceledException;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.WaitForCardInsertionBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.WaitForCardRemovalDuringProcessingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.WaitForCardRemovalBlockingSpi;

public class ObservableReaderBlockingSpiMock
    implements ObservableReaderSpi,
        WaitForCardInsertionBlockingSpi,
        WaitForCardRemovalBlockingSpi,
        WaitForCardRemovalDuringProcessingSpi,
        ObservableReaderSpiMock {

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
  public byte[] getAtr() {
    return new byte[0];
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
  public void unregister() {}

  /**
   * Wait for a one time card insertion
   *
   * @throws ReaderIOException
   * @throws TaskCanceledException
   */
  @Override
  public void waitForCardInsertion() throws ReaderIOException, TaskCanceledException {
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
  public void waitForCardRemoval() throws ReaderIOException, TaskCanceledException {
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
}
