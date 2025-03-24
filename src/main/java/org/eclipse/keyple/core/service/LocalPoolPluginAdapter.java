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

import java.util.SortedSet;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.PoolReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a local {@link PoolPlugin}.
 *
 * @since 2.0.0
 */
final class LocalPoolPluginAdapter extends AbstractPluginAdapter implements PoolPlugin {

  private static final Logger logger = LoggerFactory.getLogger(LocalPoolPluginAdapter.class);

  private final PoolPluginSpi poolPluginSpi;

  /**
   * Constructor.
   *
   * @param poolPluginSpi The associated SPI.
   * @since 2.0.0
   */
  LocalPoolPluginAdapter(PoolPluginSpi poolPluginSpi) {
    super(poolPluginSpi.getName(), (KeyplePluginExtension) poolPluginSpi);
    this.poolPluginSpi = poolPluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Unregisters the associated SPI.
   *
   * @since 2.0.0
   */
  @Override
  void unregister() {
    try {
      poolPluginSpi.onUnregister();
    } catch (Exception e) {
      logger.warn("Error unregistering plugin extension [{}]: {}", getName(), e.getMessage());
    }
    super.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SortedSet<String> getReaderGroupReferences() {
    checkStatus();
    try {
      return poolPluginSpi.getReaderGroupReferences();
    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          String.format(
              "Pool plugin [%s] is unable to get reader group references: %s",
              getName(), e.getMessage()),
          e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardReader allocateReader(String readerGroupReference) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Pool plugin [{}] allocates reader of group reference [{}]",
          getName(),
          readerGroupReference);
    }
    Assert.getInstance().notEmpty(readerGroupReference, "readerGroupReference");

    ReaderSpi readerSpi;
    try {
      readerSpi = poolPluginSpi.allocateReader(readerGroupReference);
    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          String.format(
              "Pool plugin [%s] unable to allocate reader of reader group reference [%s]: %s",
              getName(), readerGroupReference, e.getMessage()),
          e);
    }

    LocalReaderAdapter localReaderAdapter = buildLocalReaderAdapter(readerSpi);
    getReadersMap().put(localReaderAdapter.getName(), localReaderAdapter);
    localReaderAdapter.register();
    return localReaderAdapter;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.2.0
   */
  @Override
  public SmartCard getSelectedSmartCard(CardReader reader) {
    Assert.getInstance().notNull(reader, "reader");
    PoolReaderSpi poolReaderSpi =
        (PoolReaderSpi) getReaderExtension(KeypleReaderExtension.class, reader.getName());
    return (SmartCard) poolReaderSpi.getSelectedSmartCard();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void releaseReader(CardReader reader) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Pool plugin [{}] releases reader [{}]",
          getName(),
          reader != null ? reader.getName() : null);
    }
    Assert.getInstance().notNull(reader, "reader");

    try {
      poolPluginSpi.releaseReader(
          ((LocalReaderAdapter) reader).getReaderSpi()); // NOSONAR nullity check is done above
    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          String.format(
              "Pool plugin [%s] unable to release reader [%s]: %s",
              getName(), reader.getName(), e.getMessage()),
          e);
    } finally {
      getReadersMap().remove(reader.getName());
      ((LocalReaderAdapter) reader).unregister();
    }
  }
}
