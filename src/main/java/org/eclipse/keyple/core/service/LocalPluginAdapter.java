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

import java.util.Set;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;

/**
 * (package-private)<br>
 * Implementation of a local {@link Plugin}.
 *
 * @since 2.0
 */
class LocalPluginAdapter extends AbstractPluginAdapter {

  private final PluginSpi pluginSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param pluginSpi The associated SPI.
   * @since 2.0
   */
  LocalPluginAdapter(PluginSpi pluginSpi) {
    super(pluginSpi.getName(), pluginSpi);
    this.pluginSpi = pluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Populates its list of available readers and registers each of them.
   *
   * @since 2.0
   */
  @Override
  final void register() throws PluginIOException {

    super.register();

    Set<ReaderSpi> readerSpiList = pluginSpi.searchAvailableReaders();

    for (ReaderSpi readerSpi : readerSpiList) {
      LocalReaderAdapter localReaderAdapter;
      if (readerSpi instanceof ObservableReaderSpi) {
        localReaderAdapter =
            new ObservableLocalReaderAdapter((ObservableReaderSpi) readerSpi, getName());
      } else {
        localReaderAdapter = new LocalReaderAdapter(readerSpi, getName());
      }
      getReadersMap().put(readerSpi.getName(), localReaderAdapter);
      localReaderAdapter.register();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Unregisters the associated SPI.
   *
   * @since 2.0
   */
  @Override
  void unregister() {
    super.unregister();
    pluginSpi.unregister();
  }
}
