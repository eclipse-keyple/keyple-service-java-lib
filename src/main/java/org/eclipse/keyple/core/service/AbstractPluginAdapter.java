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
import java.util.regex.PatternSyntaxException;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for all plugins.
 *
 * @since 2.0.0
 */
abstract class AbstractPluginAdapter implements Plugin {

  private static final Logger logger = LoggerFactory.getLogger(AbstractPluginAdapter.class);
  static final String REMOTE_READER_NAME_SUFFIX = " (Remote)";
  private final String pluginName;
  private final KeyplePluginExtension pluginExtension;
  private boolean isRegistered;
  private final Map<String, CardReader> readers;

  /**
   * Constructor.
   *
   * @param pluginName The name of the plugin.
   * @param pluginExtension The associated plugin extension SPI.
   * @since 2.0.0
   */
  AbstractPluginAdapter(String pluginName, KeyplePluginExtension pluginExtension) {
    this.pluginName = pluginName;
    this.pluginExtension = pluginExtension;
    this.readers = new ConcurrentHashMap<String, CardReader>();
  }

  /**
   * Builds a local reader adapter using the provided SPI.
   *
   * @param readerSpi The associated reader SPI.
   * @return A new instance.
   */
  final LocalReaderAdapter buildLocalReaderAdapter(ReaderSpi readerSpi) {
    LocalReaderAdapter adapter;
    if (readerSpi instanceof ObservableReaderSpi) {
      if (readerSpi instanceof ConfigurableReaderSpi) {
        adapter =
            new ObservableLocalConfigurableReaderAdapter(
                (ConfigurableReaderSpi) readerSpi, getName());
      } else {
        adapter = new ObservableLocalReaderAdapter((ObservableReaderSpi) readerSpi, getName());
      }
    } else if (readerSpi instanceof ConfigurableReaderSpi) {
      adapter = new LocalConfigurableReaderAdapter((ConfigurableReaderSpi) readerSpi, getName());
    } else {
      adapter = new LocalReaderAdapter(readerSpi, getName());
    }
    return adapter;
  }

  /**
   * Check if the plugin is registered.
   *
   * @throws IllegalStateException is thrown when plugin is not or no longer registered.
   * @since 2.0.0
   */
  final void checkStatus() {
    if (!isRegistered) {
      throw new IllegalStateException(
          String.format("Plugin '%s' is not or no longer registered.", pluginName));
    }
  }

  /**
   * Changes the plugin status to registered.
   *
   * @throws PluginIOException If registration failed.
   * @since 2.0.0
   */
  void register() throws PluginIOException {
    isRegistered = true;
  }

  /**
   * Unregisters the plugin and the readers present in its list.
   *
   * @since 2.0.0
   */
  void unregister() {
    for (CardReader reader : readers.values()) {
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
   * @since 2.0.0
   */
  @Override
  public final String getName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final <T extends KeyplePluginExtension> T getExtension(Class<T> pluginExtensionClass) {
    checkStatus();
    return (T) pluginExtension;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.0
   */
  @Override
  public final <T extends KeypleReaderExtension> T getReaderExtension(
      Class<T> readerExtensionClass, String readerName) {
    checkStatus();
    AbstractReaderAdapter reader = (AbstractReaderAdapter) getReader(readerName);
    if (reader == null) {
      throw new IllegalArgumentException("Reader '" + readerName + "'not found!");
    }
    return reader.getExtension(readerExtensionClass);
  }

  /**
   * Gets the Map of all connected readers.
   *
   * @since 2.0.0
   */
  final Map<String, CardReader> getReadersMap() {
    return readers;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final Set<String> getReaderNames() {
    checkStatus();
    return new HashSet<String>(readers.keySet());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final Set<CardReader> getReaders() {
    checkStatus();
    return new HashSet<CardReader>(readers.values());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public final CardReader getReader(String name) {
    checkStatus();
    return readers.get(name);
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.1.0
   */
  @Override
  public final CardReader findReader(String readerNameRegex) {
    for (CardReader reader : readers.values()) {
      try {
        if (reader.getName().matches(readerNameRegex)) {
          return reader;
        }
      } catch (PatternSyntaxException e) {
        throw new IllegalArgumentException("readerNameRegex is invalid: " + e.getMessage(), e);
      }
    }
    return null;
  }
}
