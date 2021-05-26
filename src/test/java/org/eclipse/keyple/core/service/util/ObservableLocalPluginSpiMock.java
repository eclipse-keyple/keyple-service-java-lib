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
package org.eclipse.keyple.core.service.util;

import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.ObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.mockito.Mockito;

public class ObservableLocalPluginSpiMock implements ObservablePluginSpi {

  private final String name;
  private final int monitoringCycleDuration = 0;
  private final Map<String, ReaderSpi> stubReaders;
  private final PluginIOException pluginError;
  /**
   * (package-private )constructor
   *
   * @param name name of the plugin
   * @since 2.0
   */
  public ObservableLocalPluginSpiMock(String name, PluginIOException pluginError) {
    this.name = name;
    this.pluginError = pluginError;
    this.stubReaders = new ConcurrentHashMap<String, ReaderSpi>();
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public int getMonitoringCycleDuration() {
    return monitoringCycleDuration;
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Set<String> searchAvailableReadersNames() throws PluginIOException {
    if (pluginError != null) {
      throw pluginError;
    }
    return new HashSet<String>(stubReaders.keySet());
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ReaderSpi searchReader(String readerName) throws PluginIOException {
    if (pluginError != null) {
      throw pluginError;
    }
    return stubReaders.get(readerName);
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String getName() {
    return name;
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Set<ReaderSpi> searchAvailableReaders() throws PluginIOException {
    return new HashSet(stubReaders.values());
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void unregister() {
    // NO-OP
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void addReaderName(String... name) {
    for (String readerName : name) {
      ReaderSpi readerSpi = Mockito.mock(ReaderSpi.class);
      when(readerSpi.getName()).thenReturn(readerName);
      stubReaders.put(readerName, readerSpi);
    }
  }
  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void removeReaderName(String... name) {
    for (String readerName : name) {
      stubReaders.remove(readerName);
    }
  }
}
