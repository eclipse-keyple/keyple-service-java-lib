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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract class for all plugins.
 *
 * @since 2.0
 */
abstract class AbstractPluginAdapter implements Plugin {

  private static final Logger logger = LoggerFactory.getLogger(AbstractPluginAdapter.class);
  private final String pluginName;
  private final KeyplePluginExtension pluginExtension;
  private boolean isRegistered;
  private final Map<String, Reader> readers;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param pluginName The name of the plugin.
   * @param pluginExtension The associated plugin extension SPI.
   * @since 2.0
   */
  AbstractPluginAdapter(String pluginName, KeyplePluginExtension pluginExtension) {
    this.pluginName = pluginName;
    this.pluginExtension = pluginExtension;
    this.readers = new ConcurrentHashMap<String, Reader>();
  }

  /**
   * (package-private)<br>
   * Check if the plugin is registered.
   *
   * @throws IllegalStateException is thrown when plugin is not or no longer registered.
   * @since 2.0
   */
  final void checkStatus() {
    if (!isRegistered) {
      throw new IllegalStateException(
          String.format("The plugin '%s' is not or no longer registered.", pluginName));
    }
  }

  /**
   * (package-private)<br>
   * Changes the plugin status to registered.
   *
   * @throws PluginIOException If registration failed.
   * @since 2.0
   */
  void register() throws PluginIOException {
    isRegistered = true;
  }

  /**
   * (package-private)<br>
   * Unregisters the plugin and the readers present in its list.
   *
   * @since 2.0
   */
  void unregister() {
    for (Reader reader : readers.values()) {
      try {
        ((AbstractReaderAdapter) reader).unregister();
      } catch (Exception e) {
        logger.error("Error during the unregistration of reader '{}'", reader.getName(), e);
      }
    }
    readers.clear();
    isRegistered = false;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final String getName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final <T extends KeyplePluginExtension> T getExtension(Class<T> pluginExtensionClass) {
    checkStatus();
    return (T) pluginExtension;
  }

  /**
   * (package-private)<br>
   * Gets the Map of all connected readers.
   *
   * @since 2.0
   */
  final Map<String, Reader> getReadersMap() {
    return readers;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Set<String> getReaderNames() {
    checkStatus();
    return new HashSet<String>(readers.keySet());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Set<Reader> getReaders() {
    checkStatus();
    return new HashSet<Reader>(readers.values());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Reader getReader(String name) {
    checkStatus();
    return readers.get(name);
  }
}
