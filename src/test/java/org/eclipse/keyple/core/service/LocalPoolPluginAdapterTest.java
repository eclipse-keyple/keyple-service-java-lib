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
import static org.assertj.core.api.Assertions.shouldHaveThrown;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.*;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.spi.SmartCard;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.PoolReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi;
import org.junit.Before;
import org.junit.Test;

public class LocalPoolPluginAdapterTest {
  private PoolPluginSpiMock poolPluginSpi;
  private ReaderSpiMock readerSpi1;
  private ObservableReaderSpiMock observableReader;
  private SmartCard smartCard;

  private static final String POOL_PLUGIN_NAME = "poolPlugin";
  private static final String READER_NAME_1 = "reader1";
  private static final String READER_NAME_2 = "reader2";
  private static final String OBSERVABLE_READER_NAME = "observableReader";
  private static final String GROUP_1 = "group1";
  private static final String GROUP_2 = "group2";
  private static final String GROUP_3 = "group3";

  interface PoolPluginSpiMock extends KeyplePluginExtension, PoolPluginSpi {}

  interface ObservableReaderSpiMock
      extends KeypleReaderExtension,
          PoolReaderSpi,
          ObservableReaderSpi,
          CardInsertionWaiterBlockingSpi,
          CardRemovalWaiterBlockingSpi {}

  interface ReaderSpiMock extends KeypleReaderExtension, PoolReaderSpi {}

  @Before
  public void setUp() throws Exception {
    SortedSet<String> groupReferences = new TreeSet<String>(Arrays.asList(GROUP_1, GROUP_2));

    readerSpi1 = mock(ReaderSpiMock.class);
    when(readerSpi1.getName()).thenReturn(READER_NAME_1);

    ReaderSpiMock readerSpi2 = mock(ReaderSpiMock.class);
    when(readerSpi2.getName()).thenReturn(READER_NAME_2);

    observableReader = mock(ObservableReaderSpiMock.class);
    when(observableReader.getName()).thenReturn(OBSERVABLE_READER_NAME);

    poolPluginSpi = mock(PoolPluginSpiMock.class);
    when(poolPluginSpi.getName()).thenReturn(POOL_PLUGIN_NAME);
    when(poolPluginSpi.getReaderGroupReferences()).thenReturn(groupReferences);
    when(poolPluginSpi.allocateReader(GROUP_1)).thenReturn(readerSpi1);
    when(poolPluginSpi.allocateReader(GROUP_2)).thenReturn(readerSpi2);

    smartCard = mock(SmartCard.class);
  }

  @Test(expected = KeyplePluginException.class)
  public void getReaderGroupReferences_whenGettingReferencesFails_shouldKPE() throws Exception {
    when(poolPluginSpi.getReaderGroupReferences())
        .thenThrow(new PluginIOException("Plugin IO Exception"));
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    localPluginAdapter.getReaderGroupReferences();
  }

  @Test(expected = IllegalStateException.class)
  public void getReaderGroupReferences_whenNotRegistered_shouldISE() {
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.getReaderGroupReferences();
  }

  @Test
  public void getReaderGroupReferences_whenSucceeds_shouldReturnReferences() throws Exception {
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    SortedSet<String> groupReferences = localPluginAdapter.getReaderGroupReferences();
    assertThat(groupReferences).containsExactly(GROUP_1, GROUP_2);
  }

  @Test(expected = IllegalStateException.class)
  public void allocateReader_whenNotRegistered_shouldISE() {
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.allocateReader(GROUP_1);
  }

  @Test(expected = KeyplePluginException.class)
  public void allocateReader_whenAllocatingReaderFails_shouldKPE() throws Exception {
    when(poolPluginSpi.allocateReader(anyString()))
        .thenThrow(new PluginIOException("Plugin IO Exception"));
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    localPluginAdapter.allocateReader(GROUP_1);
  }

  @Test
  public void allocateReader_whenSucceeds_shouldReturnReader() throws Exception {
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    CardReader reader = localPluginAdapter.allocateReader(GROUP_1);
    assertThat(reader.getName()).isEqualTo(READER_NAME_1);
    assertThat(reader).isInstanceOf(CardReader.class).isInstanceOf(LocalReaderAdapter.class);
    assertThat(localPluginAdapter.getReaderNames()).containsExactly(READER_NAME_1);
    assertThat(localPluginAdapter.getReaders())
        .containsExactlyInAnyOrder(localPluginAdapter.getReader(READER_NAME_1));
  }

  @Test
  public void allocateReader_whenReaderIsObservable_shouldReturnObservableReader()
      throws Exception {
    when(poolPluginSpi.allocateReader(GROUP_3)).thenReturn(observableReader);
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    CardReader reader = localPluginAdapter.allocateReader(GROUP_3);
    assertThat(reader.getName()).isEqualTo(OBSERVABLE_READER_NAME);
    assertThat(reader)
        .isInstanceOf(CardReader.class)
        .isInstanceOf(ObservableLocalReaderAdapter.class);
    assertThat(localPluginAdapter.getReaders())
        .containsExactlyInAnyOrder(localPluginAdapter.getReader(OBSERVABLE_READER_NAME));
  }

  @Test(expected = IllegalStateException.class)
  public void releaseReader_whenNotRegistered_shouldISE() throws Exception {
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    CardReader reader = localPluginAdapter.allocateReader(GROUP_1);
    localPluginAdapter.unregister();
    localPluginAdapter.releaseReader(reader);
  }

  @Test
  public void releaseReader_whenSucceeds_shouldRemoveReader() throws Exception {
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    CardReader reader = localPluginAdapter.allocateReader(GROUP_1);
    localPluginAdapter.releaseReader(reader);
    assertThat(localPluginAdapter.getReaderNames()).isEmpty();
    assertThat(localPluginAdapter.getReaders()).isEmpty();
  }

  @Test
  public void releaseReader_whenReleaseReaderFails_shouldKPE_and_RemoveReader() throws Exception {
    doThrow(new PluginIOException("Plugin IO Exception"))
        .when(poolPluginSpi)
        .releaseReader(any(ReaderSpi.class));
    LocalPoolPluginAdapter localPluginAdapter = new LocalPoolPluginAdapter(poolPluginSpi);
    localPluginAdapter.register();
    CardReader reader = localPluginAdapter.allocateReader(GROUP_1);
    try {
      localPluginAdapter.releaseReader(reader);
      shouldHaveThrown(KeyplePluginException.class);
    } catch (KeyplePluginException e) {

    }
    assertThat(localPluginAdapter.getReaderNames()).isEmpty();
    assertThat(localPluginAdapter.getReaders()).isEmpty();
  }

  @Test
  public void getSelectedSmartCard_whenSelectedSmartCardIsNull_shouldReturnNull() throws Exception {
    when(readerSpi1.getSelectedSmartCard()).thenReturn(null);
    LocalPoolPluginAdapter plugin = new LocalPoolPluginAdapter(poolPluginSpi);
    plugin.register();
    CardReader reader = plugin.allocateReader(GROUP_1);
    assertThat(plugin.getSelectedSmartCard(reader)).isNull();
  }

  @Test
  public void getSelectedSmartCard_whenSelectedSmartCardIsNotNull_shouldReturnSelectedSmartCard()
      throws Exception {
    when(readerSpi1.getSelectedSmartCard()).thenReturn(smartCard);
    LocalPoolPluginAdapter plugin = new LocalPoolPluginAdapter(poolPluginSpi);
    plugin.register();
    CardReader reader = plugin.allocateReader(GROUP_1);
    assertThat(plugin.getSelectedSmartCard(reader)).isSameAs(smartCard);
  }
}
