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
import org.eclipse.keyple.core.plugin.AutonomousObservablePluginApi;
import org.eclipse.keyple.core.plugin.spi.AutonomousObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of an autonomous local {@link ObservablePlugin}.
 *
 * @since 2.0
 */
final class AutonomousObservableLocalPluginAdapter extends AbstractObservableLocalPluginAdapter
    implements AutonomousObservablePluginApi {

  private static final Logger logger =
      LoggerFactory.getLogger(AutonomousObservableLocalPluginAdapter.class);

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param autonomousObservablePluginSpi The associated plugin SPI.
   * @since 2.0
   */
  AutonomousObservableLocalPluginAdapter(
      AutonomousObservablePluginSpi autonomousObservablePluginSpi) {
    super(autonomousObservablePluginSpi);
    autonomousObservablePluginSpi.connect(this);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onReaderConnected(Set<ReaderSpi> readers) {
    Set<String> readerNames = new HashSet<String>();
    for (ReaderSpi readerSpi : readers) {
      readerNames.add(readerSpi.getName());
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Notifying connection(s): {}", readerNames);
    }
    notifyObservers(
        new PluginEvent(getName(), readerNames, PluginEvent.EventType.READER_CONNECTED));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onReaderDisconnected(Set<String> readerNames) {
    if (logger.isTraceEnabled()) {
      logger.trace("Notifying disconnection(s): {}", readerNames);
    }
    notifyObservers(
        new PluginEvent(getName(), readerNames, PluginEvent.EventType.READER_DISCONNECTED));
  }
}
