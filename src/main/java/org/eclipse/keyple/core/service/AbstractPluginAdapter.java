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
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;

/**
 * (package-private)<br>
 * Abstract class for all plugins.
 *
 * @since 2.0
 */
abstract class AbstractPluginAdapter implements Plugin {

  private final String pluginName;
  private final Object pluginExtension;
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
  AbstractPluginAdapter(String pluginName, Object pluginExtension) {
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
//    if (pluginSpi instanceof PluginSpi) {
//      // retrieve the current readers of the local plugin
//      Set<ReaderSpi> readerSpis = ((PluginSpi) pluginSpi).searchAvailableReaders();
//      // create and keep the local readers, register it
//      for (ReaderSpi readerSpi : readerSpis) {
//        LocalReaderAdapter localReaderAdapter = null;
//        if(readerSpi instanceof ObservableReaderSpi){
//          localReaderAdapter = new ObservableLocalReaderAdapter(((ObservableReaderSpi)readerSpi), pluginName);
//        }
//        else{
//          localReaderAdapter = new LocalReaderAdapter(readerSpi, pluginName);
//        }
//        readers.put(readerSpi.getName(), localReaderAdapter);
//        localReaderAdapter.register();
//      }
//    } else {
//      // remote plugin
//    }
  }

  /**
   * (package-private)<br>
   * Unregisters the plugin and the readers present in its list.
   *
   * @since 2.0
   */
  void unregister() {
    isRegistered = false;
    for (String key : readers.keySet()) {
      Reader reader = readers.remove(key);
      ((AbstractReaderAdapter) reader).unregister();
    }
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
  public final <T extends KeyplePluginExtension> T getExtension(Class<T> pluginExtensionType) {
    checkStatus();
    return (T) pluginExtension;
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
  public final Reader getReader(String name) {
    checkStatus();
    return readers.get(name);
  }
}
