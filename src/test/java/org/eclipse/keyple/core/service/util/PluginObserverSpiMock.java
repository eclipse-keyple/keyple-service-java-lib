/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.service.PluginEvent;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;

public class PluginObserverSpiMock implements PluginObserverSpi {

  Map<PluginEvent.Type, PluginEvent> eventTypeReceived =
      new ConcurrentHashMap<PluginEvent.Type, PluginEvent>();
  RuntimeException throwEx;

  public PluginObserverSpiMock(RuntimeException e) {
    this.throwEx = e;
  }

  @Override
  public void onPluginEvent(PluginEvent pluginEvent) {
    eventTypeReceived.put(pluginEvent.getType(), pluginEvent);
    if (throwEx != null) {
      throw throwEx;
    }
  }

  public Boolean hasReceived(PluginEvent.Type eventType) {
    return eventTypeReceived.keySet().contains(eventType);
  }

  public PluginEvent getLastEventOfType(PluginEvent.Type eventType) {
    return eventTypeReceived.get(eventType);
  }
}
