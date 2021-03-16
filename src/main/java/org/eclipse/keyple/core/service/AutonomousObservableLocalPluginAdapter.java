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
package org.eclipse.keyple.core.service;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.keyple.core.plugin.AutonomousObservablePluginManager;
import org.eclipse.keyple.core.plugin.spi.AutonomousObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of {@link ObservablePlugin} for an autonomous observable local plugin.
 *
 * @since 2.0
 */
final class AutonomousObservableLocalPluginAdapter
    extends AbstractObservablePluginAdapter<AutonomousObservablePluginSpi>
    implements AutonomousObservablePluginManager {
  private static final Logger logger =
      LoggerFactory.getLogger(AutonomousObservableLocalPluginAdapter.class);

  /**
   * (package-private)<br>
   * Creates an instance of {@link AutonomousObservableLocalPluginAdapter}.
   *
   * @param autonomousObservablePluginSpi The plugin SPI.
   * @since 2.0
   */
  AutonomousObservableLocalPluginAdapter(
      AutonomousObservablePluginSpi autonomousObservablePluginSpi) {
    super(autonomousObservablePluginSpi);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onReaderConnected(Set<ReaderSpi> readers) {
    Set<String> readersNames = new HashSet<String>();
    for (ReaderSpi readerSpi : readers) {
      readersNames.add(readerSpi.getName());
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Notifying connection(s): {}", readersNames);
    }
    notifyObservers(
        new PluginEvent(getName(), readersNames, PluginEvent.EventType.READER_CONNECTED));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onReaderDisconnected(Set<String> readersNames) {
    if (logger.isTraceEnabled()) {
      logger.trace("Notifying disconnection(s): {}", readersNames);
    }
    notifyObservers(
        new PluginEvent(getName(), readersNames, PluginEvent.EventType.READER_DISCONNECTED));
  }
}
