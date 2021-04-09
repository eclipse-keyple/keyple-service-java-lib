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
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;

/**
 * (package-private)<br>
 * Implementation of a local {@link PoolPlugin}.
 *
 * @since 2.0
 */
final class LocalPoolPluginAdapter extends AbstractPluginAdapter implements PoolPlugin {

  private final PoolPluginSpi poolPluginSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param poolPluginSpi The associated SPI.
   * @since 2.0
   */
  LocalPoolPluginAdapter(PoolPluginSpi poolPluginSpi) {
    super(poolPluginSpi.getName(), poolPluginSpi);
    this.poolPluginSpi = poolPluginSpi;
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
    poolPluginSpi.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public SortedSet<String> getReaderGroupReferences() {
    try {
      return poolPluginSpi.getReaderGroupReferences();
    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          String.format(
              "The pool plugin '%s' is unable to get reader group references : %s",
              getName(), e.getMessage()),
          e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Reader allocateReader(String readerGroupReference) {

    checkStatus();

    ReaderSpi readerSpi;
    try {
      readerSpi = poolPluginSpi.allocateReader(readerGroupReference);
    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          String.format(
              "The pool plugin '%s' is unable to allocate a reader of the reader group reference '%s' : %s",
              getName(), readerGroupReference, e.getMessage()),
          e);
    }

    Reader reader;
    if (readerSpi instanceof ObservableReaderSpi) {
      reader = new ObservableLocalReaderAdapter((ObservableReaderSpi) readerSpi, getName());
    } else {
      reader = new LocalReaderAdapter(readerSpi, getName());
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
      poolPluginSpi.releaseReader(((ReaderSpi) reader));
    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          String.format(
              "The pool plugin '%s' is unable to release the reader '%s' : %s",
              getName(), reader.getName(), e.getMessage()),
          e);
    }
  }
}
