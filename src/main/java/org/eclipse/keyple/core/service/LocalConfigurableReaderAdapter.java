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

import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keypop.reader.ConfigurableCardReader;

/**
 * Local configurable reader adapter.
 *
 * @since 2.0.0
 */
final class LocalConfigurableReaderAdapter extends LocalReaderAdapter
    implements ConfigurableCardReader {

  /**
   * Constructor.
   *
   * @param configurableReaderSpi The configurable reader SPI.
   * @param pluginName The name of the plugin.
   * @since 2.0.0
   */
  LocalConfigurableReaderAdapter(ConfigurableReaderSpi configurableReaderSpi, String pluginName) {
    super(configurableReaderSpi, pluginName);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void activateProtocol(String readerProtocol, String applicationProtocol) {
    activateReaderProtocol(readerProtocol, applicationProtocol);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void deactivateProtocol(String readerProtocol) {
    deactivateReaderProtocol(readerProtocol);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.2
   */
  @Override
  public String getCurrentProtocol() {
    return getCurrentPhysicalProtocolName();
  }
}
