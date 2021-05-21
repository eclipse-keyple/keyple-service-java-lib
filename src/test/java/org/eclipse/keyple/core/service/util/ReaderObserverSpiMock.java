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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.service.ReaderEvent;
import org.eclipse.keyple.core.service.spi.ReaderObserverSpi;

public class ReaderObserverSpiMock implements ReaderObserverSpi {

  Map<ReaderEvent.EventType, ReaderEvent> eventTypeReceived =
      new ConcurrentHashMap<ReaderEvent.EventType, ReaderEvent>();
  RuntimeException throwEx;

  public ReaderObserverSpiMock(RuntimeException e) {
    this.throwEx = e;
  }

  @Override
  public void onReaderEvent(ReaderEvent readerEvent) {
    eventTypeReceived.put(readerEvent.getEventType(), readerEvent);
    if (throwEx != null) {
      throw throwEx;
    }
  }

  public Boolean hasReceived(ReaderEvent.EventType eventType) {
    return eventTypeReceived.keySet().contains(eventType);
  }

  public ReaderEvent getLastEventOfType(ReaderEvent.EventType eventType) {
    return eventTypeReceived.get(eventType);
  }
}
