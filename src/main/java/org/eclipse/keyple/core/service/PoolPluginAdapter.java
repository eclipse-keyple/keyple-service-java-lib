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

import java.util.SortedSet;

import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;

/**
 * (package-private)<br>
 * Implementation of {@link PoolPlugin}.
 *
 * @since 2.0
 */
final class PoolPluginAdapter<P> extends PluginAdapter<P> implements PoolPlugin {

  private final P poolPluginSpi;

  /**
   * (package-private)<br>
   * Creates an instance of {@link PoolPluginAdapter}.
   *
   * @param poolPluginSpi The plugin SPI.
   * @since 2.0
   */
  PoolPluginAdapter(P poolPluginSpi) {
    super(poolPluginSpi);
    this.poolPluginSpi = poolPluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public SortedSet<String> getReaderGroupReferences() {
    SortedSet<String> readerGroupReferences;
    try {
      readerGroupReferences = ((PoolPluginSpi) poolPluginSpi).getReaderGroupReferences();
    } catch (PluginIOException e) {
      throw new KeyplePluginException("Unable to get reader group references.", e);
    }
    return readerGroupReferences;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Reader allocateReader(String readerGroupReference) {
    Reader reader;
    try {
      reader =
          new LocalReaderAdapter(
              ((PoolPluginSpi) poolPluginSpi).allocateReader(readerGroupReference), getName());
    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          "Unable to allocate a reader for reference " + readerGroupReference, e);
    }
    return reader;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void releaseReader(Reader reader) {
    try {
      ((PoolPluginSpi) poolPluginSpi).releaseReader(((ReaderSpi) reader));
    } catch (PluginIOException e) {
      throw new KeyplePluginException("Unable to release reader " + reader.getName(), e);
    }
  }
}
