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

import java.util.HashSet;
import java.util.Set;
import org.eclipse.keyple.core.plugin.AutonomousObservablePluginApi;
import org.eclipse.keyple.core.plugin.spi.AutonomousObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an autonomous local {@link ObservablePlugin}.
 *
 * @since 2.0.0
 */
final class AutonomousObservableLocalPluginAdapter extends AbstractObservableLocalPluginAdapter
    implements AutonomousObservablePluginApi {

  private static final Logger logger =
      LoggerFactory.getLogger(AutonomousObservableLocalPluginAdapter.class);

  /**
   * Constructor.
   *
   * @param autonomousObservablePluginSpi The associated plugin SPI.
   * @since 2.0.0
   */
  AutonomousObservableLocalPluginAdapter(
      AutonomousObservablePluginSpi autonomousObservablePluginSpi) {
    super(autonomousObservablePluginSpi);
    try {
      autonomousObservablePluginSpi.setCallback(this);
    } catch (Exception e) {
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[plugin={}] Method 'setCallback(...)' unavailable for legacy plugin", getName());
      }
      autonomousObservablePluginSpi.connect(this);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onReaderConnected(Set<ReaderSpi> readers) {
    Assert.getInstance().notEmpty(readers, "readers");
    Set<String> notifyReaders = new HashSet<>();

    for (ReaderSpi readerSpi : readers) {
      // add reader to plugin
      this.addReader(readerSpi);
      notifyReaders.add(readerSpi.getName());
    }

    notifyObservers(
        new PluginEventAdapter(getName(), notifyReaders, PluginEvent.Type.READER_CONNECTED));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onReaderDisconnected(Set<String> readerNames) {
    Assert.getInstance().notEmpty(readerNames, "readerNames");
    Set<String> notifyReaders = new HashSet<>();

    for (String readerName : readerNames) {
      CardReader reader = this.getReader(readerName);
      if (reader == null) {
        logger.warn(
            "[plugin={}] Unable to remove unknown reader [reader={}]", getName(), readerName);
      } else {
        // unregister and remove reader
        ((LocalReaderAdapter) reader).unregister();
        getReadersMap().remove(reader.getName());
        notifyReaders.add(readerName);
        logger.info(
            "[plugin={}] Reader removed from readers list [reader={}]",
            getName(),
            reader.getName());
      }
    }

    notifyObservers(
        new PluginEventAdapter(getName(), notifyReaders, PluginEvent.Type.READER_DISCONNECTED));
  }

  /**
   * Create and add a reader to the reader list from a readerSpi
   *
   * @param readerSpi spi to create the reader from
   */
  private void addReader(ReaderSpi readerSpi) {
    LocalReaderAdapter reader = buildLocalReaderAdapter(readerSpi);
    reader.register();
    getReadersMap().put(reader.getName(), reader);
    logger.info(
        "[plugin={}] Reader added to readers list [reader={}]", getName(), readerSpi.getName());
  }
}
