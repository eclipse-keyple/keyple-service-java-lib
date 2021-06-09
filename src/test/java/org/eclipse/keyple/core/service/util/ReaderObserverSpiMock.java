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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.spi.CardReaderObserverSpi;
import org.eclipse.keyple.core.service.ReaderEvent;

public class ReaderObserverSpiMock implements CardReaderObserverSpi {

  Map<CardReaderEvent.Type, CardReaderEvent> eventTypeReceived =
      new ConcurrentHashMap<CardReaderEvent.Type, CardReaderEvent>();
  RuntimeException throwEx;

  public ReaderObserverSpiMock(RuntimeException e) {
    this.throwEx = e;
  }

  @Override
  public void onReaderEvent(CardReaderEvent readerEvent) {
    eventTypeReceived.put(readerEvent.getType(), readerEvent);
    if (throwEx != null) {
      throw throwEx;
    }
  }

  public Boolean hasReceived(ReaderEvent.Type eventType) {
    return eventTypeReceived.keySet().contains(eventType);
  }

  public CardReaderEvent getLastEventOfType(ReaderEvent.Type eventType) {
    return eventTypeReceived.get(eventType);
  }
}
