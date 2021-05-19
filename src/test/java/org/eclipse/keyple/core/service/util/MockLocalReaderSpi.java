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

import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;

public class MockLocalReaderSpi implements ReaderSpi {

  protected String readerName;

  public MockLocalReaderSpi(String readerName) {
    this.readerName = readerName;
  }

  @Override
  public String getName() {
    return readerName;
  }

  @Override
  public boolean isProtocolSupported(String readerProtocol) {
    return false;
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
  public void openPhysicalChannel() throws ReaderIOException, CardIOException {}

  @Override
  public void closePhysicalChannel() throws ReaderIOException {}

  @Override
  public boolean isPhysicalChannelOpen() {
    return false;
  }

  @Override
  public boolean checkCardPresence() throws ReaderIOException {
    return false;
  }

  @Override
  public byte[] getAtr() {
    return new byte[0];
  }

  @Override
  public byte[] transmitApdu(byte[] apduIn) throws ReaderIOException, CardIOException {
    return new byte[0];
  }

  @Override
  public boolean isContactless() {
    return false;
  }

  @Override
  public void unregister() {}
}
