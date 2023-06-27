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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.junit.Before;
import org.junit.Test;

public class LocalPluginAdapterTest {
  private PluginSpiMock pluginSpi;
  private ReaderSpiMock readerSpi1;
  private ReaderSpiMock readerSpi2;
  private ObservableReaderSpiMock observableReader;

  interface PluginSpiMock extends KeyplePluginExtension, PluginSpi {}

  interface ReaderSpiMock extends KeypleReaderExtension, ReaderSpi {}

  interface ObservableReaderSpiMock
      extends KeypleReaderExtension,
          ObservableReaderSpi,
          CardInsertionWaiterBlockingSpi,
          CardRemovalWaiterBlockingSpi {}

  @Before
  public void setUp() throws Exception {
    pluginSpi = mock(PluginSpiMock.class);
    when(pluginSpi.getName()).thenReturn(PLUGIN_NAME);
    when(pluginSpi.searchAvailableReaders()).thenReturn(Collections.<ReaderSpi>emptySet());

    readerSpi1 = mock(ReaderSpiMock.class);
    when(readerSpi1.getName()).thenReturn(READER_NAME_1);

    readerSpi2 = mock(ReaderSpiMock.class);
    when(readerSpi2.getName()).thenReturn(READER_NAME_2);

    observableReader = mock(ObservableReaderSpiMock.class);
    when(observableReader.getName()).thenReturn(OBSERVABLE_READER_NAME);
  }

  @Test(expected = PluginIOException.class)
  public void register_whenSearchReaderFails_shouldPIO() throws Exception {
    when(pluginSpi.searchAvailableReaders())
        .thenThrow(new PluginIOException("Plugin IO Exception"));
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.register();
  }

  @Test
  public void register_whenSearchReaderReturnsReader_shouldRegisterReader() throws Exception {
    Set<ReaderSpi> readerSpis = new HashSet<ReaderSpi>();
    readerSpis.add(readerSpi1);
    readerSpis.add(readerSpi2);
    when(pluginSpi.searchAvailableReaders()).thenReturn(readerSpis);
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    assertThat(localPluginAdapter.getName()).isEqualTo(PLUGIN_NAME);
    localPluginAdapter.register();
    localPluginAdapter.checkStatus();
    assertThat(localPluginAdapter.getReaderNames())
        .containsExactlyInAnyOrder(READER_NAME_1, READER_NAME_2);
    assertThat(localPluginAdapter.getReaders()).hasSize(2);
    assertThat(localPluginAdapter.getReaders())
        .containsExactlyInAnyOrder(
            localPluginAdapter.getReader(READER_NAME_1),
            localPluginAdapter.getReader(READER_NAME_2));
    assertThat(localPluginAdapter.getReader(READER_NAME_1)).isInstanceOf(CardReader.class);
    assertThat(localPluginAdapter.getReader(READER_NAME_1)).isInstanceOf(LocalReaderAdapter.class);
    assertThat(localPluginAdapter.getReader(READER_NAME_2)).isInstanceOf(CardReader.class);
    assertThat(localPluginAdapter.getReader(READER_NAME_2)).isInstanceOf(LocalReaderAdapter.class);
    assertThat(localPluginAdapter.getReader(READER_NAME_2))
        .isNotEqualTo(localPluginAdapter.getReader(READER_NAME_1));
  }

  @Test
  public void register_whenSearchReaderReturnsObservableReader_shouldRegisterObservableReader()
      throws Exception {
    Set<ReaderSpi> readerSpis = new HashSet<ReaderSpi>();
    readerSpis.add(observableReader);
    when(pluginSpi.searchAvailableReaders()).thenReturn(readerSpis);
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.register();
    localPluginAdapter.checkStatus();
    assertThat(localPluginAdapter.getReaderNames())
        .containsExactlyInAnyOrder(OBSERVABLE_READER_NAME);
    assertThat(localPluginAdapter.getReaders()).hasSize(1);
    assertThat(localPluginAdapter.getReader(OBSERVABLE_READER_NAME))
        .isInstanceOf(ObservableCardReader.class);
    assertThat(localPluginAdapter.getReader(OBSERVABLE_READER_NAME))
        .isInstanceOf(ObservableLocalReaderAdapter.class);
  }

  @Test(expected = IllegalStateException.class)
  public void getReaders_whenNotRegistered_shouldISE() {
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.getReaders();
  }

  @Test(expected = IllegalStateException.class)
  public void getReader_whenNotRegistered_shouldISE() {
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.getReader(READER_NAME_1);
  }

  @Test(expected = IllegalStateException.class)
  public void getReaderNames_whenNotRegistered_shouldISE() {
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.getReaderNames();
  }

  @Test(expected = IllegalStateException.class)
  public void unregister_shouldDisableMethodsWithISE() throws Exception {
    Set<ReaderSpi> readerSpis = new HashSet<ReaderSpi>();
    readerSpis.add(readerSpi1);
    when(pluginSpi.searchAvailableReaders()).thenReturn(readerSpis);
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.register();
    localPluginAdapter.unregister();
    localPluginAdapter.getReaders();
  }

  @Test(expected = IllegalStateException.class)
  public void getExtension_whenNotRegistered_shouldISE() {
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    assertThat(localPluginAdapter.getExtension(PluginSpiMock.class))
        .isInstanceOf(PluginSpiMock.class);
  }

  @Test
  public void getExtension_whenRegistered_shouldReturnExtension() throws Exception {
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.register();
    assertThat(localPluginAdapter.getExtension(PluginSpiMock.class))
        .isInstanceOf(PluginSpiMock.class);
  }

  @Test
  public void getReaderExtension_whenReaderIsRegistered_shouldReturnExtension() throws Exception {
    Set<ReaderSpi> readerSpis = new HashSet<ReaderSpi>();
    readerSpis.add(readerSpi1);
    when(pluginSpi.searchAvailableReaders()).thenReturn(readerSpis);
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.register();
    assertThat(localPluginAdapter.getReaderExtension(ReaderSpiMock.class, READER_NAME_1))
        .isEqualTo(readerSpi1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getReaderExtension_whenReaderNameIsUnknown_shouldReturnIAE() throws Exception {
    Set<ReaderSpi> readerSpis = new HashSet<ReaderSpi>();
    readerSpis.add(readerSpi1);
    when(pluginSpi.searchAvailableReaders()).thenReturn(readerSpis);
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.register();
    localPluginAdapter.getReaderExtension(ReaderSpiMock.class, "UNKNOWN");
  }

  @Test(expected = IllegalStateException.class)
  public void getReaderExtension_whenPluginIsNotRegistered_shouldISE() throws Exception {
    Set<ReaderSpi> readerSpis = new HashSet<ReaderSpi>();
    readerSpis.add(readerSpi1);
    when(pluginSpi.searchAvailableReaders()).thenReturn(readerSpis);
    LocalPluginAdapter localPluginAdapter = new LocalPluginAdapter(pluginSpi);
    localPluginAdapter.getReaderExtension(ReaderSpiMock.class, READER_NAME_1);
  }
}
