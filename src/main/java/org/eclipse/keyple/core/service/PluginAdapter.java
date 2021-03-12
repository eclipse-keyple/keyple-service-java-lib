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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;

/**
 * (package-private)<br>
 * Implementation of a local or remote {@link Plugin}.
 *
 * @param <P> The type of plugin.
 * @since 2.0
 */
class PluginAdapter<P> implements Plugin {
  private final Map<String, Reader> readers;
  private final P pluginSpi;
  private final String pluginName;
  private boolean isRegistered;

  /**
   * (package-private)<br>
   * Creates an instance of {@link PluginAdapter}.
   *
   * <p>The expected plugin SPI should be either a {@link PluginSpi} or a {@link RemotePluginSpi}.
   *
   * @param pluginSpi The specific plugin SPI.
   * @throws IllegalArgumentException if the SPI is null or of an unexpected type.
   * @since 2.0
   */
  PluginAdapter(P pluginSpi) {
    this.pluginSpi = pluginSpi;
    if (pluginSpi instanceof PluginSpi) {
      pluginName = ((PluginSpi) pluginSpi).getName();
    } else {
      throw new IllegalArgumentException("Unexpected plugin SPI type.");
    }
    // TODO add remote case
    readers = new ConcurrentHashMap<String, Reader>();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String getName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public <T extends KeyplePluginExtension> T getExtension(Class<T> pluginExtensionType) {
    return (T) pluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Set<String> getReadersNames() {
    checkStatus();
    return readers.keySet();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Map<String, Reader> getReaders() {
    checkStatus();
    return readers;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public final Reader getReader(String name) {
    checkStatus();
    Reader reader = readers.get(name);
    if (reader == null) {
      throw new KeypleReaderNotFoundException(name);
    }
    return reader;
  }

  /**
   * @param jsonData
   * @since 2.0
   */
  public final void onReaderEvent(String jsonData) {}

  /**
   * (package-private)<br>
   * Check if the plugin is registered.
   *
   * @throws IllegalStateException is thrown when plugin is not (or no longer) registered.
   * @since 2.0
   */
  void checkStatus() {
    if (!isRegistered)
      throw new IllegalStateException(
          String.format("This plugin, %s, is not registered", getName()));
  }

  /**
   * (package-private)<br>
   * Registers the plugin, populates its list of readers and registers each of them.
   *
   * @since 2.0
   */
  void register() throws PluginIOException {
    isRegistered = true;
    if (pluginSpi instanceof PluginSpi) {
      // retrieve the current readers of the local plugin
      Set<ReaderSpi> readerSpis = ((PluginSpi) pluginSpi).searchAvailableReaders();
      // create and keep the local readers, register it
      for (ReaderSpi readerSpi : readerSpis) {
        LocalReaderAdapter localReaderAdapter = new LocalReaderAdapter(readerSpi, pluginName);
        readers.put(readerSpi.getName(), localReaderAdapter);
        localReaderAdapter.register();
      }
    } else {
      // remote plugin
    }
  }

  /**
   * (package-private)<br>
   * Unregisters the plugin and the readers present in its list.
   *
   * @throws IllegalStateException is thrown when plugin is already unregistered.
   * @since 2.0
   */
  void unregister() {
    checkStatus();
    isRegistered = false;
    for (String key : readers.keySet()) {
      Reader reader = readers.remove(key);
      ((LocalReaderAdapter) reader).unregister();
    }
  }
}
