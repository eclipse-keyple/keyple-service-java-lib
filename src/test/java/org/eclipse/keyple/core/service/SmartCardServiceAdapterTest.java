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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import org.eclipse.keyple.core.common.*;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceFactorySpi;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceSpi;
import org.eclipse.keyple.core.distributed.remote.spi.ObservableRemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginFactorySpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePoolPluginSpi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.*;
import org.eclipse.keyple.core.plugin.spi.reader.PoolReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SmartCardServiceAdapterTest {
  private static SmartCardServiceAdapter service;

  private static final String PLUGIN_NAME = "plugin";
  private static final String OBSERVABLE_PLUGIN_NAME = "observablePlugin";
  private static final String AUTONOMOUS_OBSERVABLE_PLUGIN_NAME = "autonomousObservablePlugin";
  private static final String POOL_PLUGIN_NAME = "poolPlugin";
  private static final String REMOTE_PLUGIN_NAME = "remotePlugin";
  private static final String READER_NAME = "reader";
  private static final String LOCAL_SERVICE_NAME = "localService";

  private static final String COMMON_API_VERSION = "2.0";
  private static final String PLUGIN_API_VERSION = "2.3";
  private static final String DISTRIBUTED_REMOTE_API_VERSION = "3.0";
  private static final String DISTRIBUTED_LOCAL_API_VERSION = "2.1";
  private static final String READER_API_VERSION = "2.0";
  private static final String CARD_API_VERSION = "2.0";

  private PluginMock plugin;
  private ReaderMock reader;
  private PluginFactoryMock pluginFactory;
  private PluginFactoryMock observablePluginFactory;
  private PluginFactoryMock autonomousObservablePluginFactory;
  private PoolPluginFactoryMock poolPluginFactory;
  private RemotePluginFactoryMock remotePluginFactory;
  private ObservableRemotePluginFactoryMock observableRemotePluginFactory;
  private RemotePoolPluginFactoryMock remotePoolPluginFactory;
  private CardExtensionMock cardExtension;
  private DistributedLocalServiceFactoryMock localServiceFactory;

  interface ReaderMock extends KeypleReaderExtension, PoolReaderSpi {}

  interface PluginMock extends KeyplePluginExtension, PluginSpi {}

  interface ObservablePluginMock extends KeyplePluginExtension, ObservablePluginSpi {}

  interface AutonomousObservablePluginMock
      extends KeyplePluginExtension, AutonomousObservablePluginSpi {}

  interface PoolPluginMock extends KeyplePluginExtension, PoolPluginSpi {}

  interface RemotePluginMock extends KeyplePluginExtension, RemotePluginSpi {}

  interface ObservableRemotePluginMock extends KeyplePluginExtension, ObservableRemotePluginSpi {}

  interface RemotePoolPluginMock extends KeyplePluginExtension, RemotePoolPluginSpi {}

  interface PluginFactoryMock extends KeyplePluginExtensionFactory, PluginFactorySpi {}

  interface PoolPluginFactoryMock extends KeyplePluginExtensionFactory, PoolPluginFactorySpi {}

  interface RemotePluginFactoryMock extends KeyplePluginExtensionFactory, RemotePluginFactorySpi {}

  interface ObservableRemotePluginFactoryMock
      extends KeyplePluginExtensionFactory, RemotePluginFactorySpi {}

  interface RemotePoolPluginFactoryMock
      extends KeyplePluginExtensionFactory, RemotePluginFactorySpi {}

  interface CardExtensionMock extends KeypleCardExtension {}

  interface DistributedLocalServiceMock
      extends KeypleDistributedLocalServiceExtension, LocalServiceSpi {}

  interface DistributedLocalServiceFactoryMock
      extends KeypleDistributedLocalServiceExtensionFactory, LocalServiceFactorySpi {}

  @BeforeClass
  public static void beforeClass() throws Exception {
    service = SmartCardServiceAdapter.getInstance();
  }

  @Before
  public void setUp() throws Exception {

    reader = mock(ReaderMock.class);
    when(reader.getName()).thenReturn(READER_NAME);

    plugin = mock(PluginMock.class);
    when(plugin.getName()).thenReturn(PLUGIN_NAME);
    when(plugin.searchAvailableReaders()).thenReturn(Collections.<ReaderSpi>emptySet());

    ObservablePluginMock observablePlugin = mock(ObservablePluginMock.class);
    when(observablePlugin.getName()).thenReturn(OBSERVABLE_PLUGIN_NAME);
    when(observablePlugin.searchAvailableReaders()).thenReturn(Collections.<ReaderSpi>emptySet());

    AutonomousObservablePluginMock autonomousObservablePlugin =
        mock(AutonomousObservablePluginMock.class);
    when(autonomousObservablePlugin.getName()).thenReturn(AUTONOMOUS_OBSERVABLE_PLUGIN_NAME);
    when(autonomousObservablePlugin.searchAvailableReaders())
        .thenReturn(Collections.<ReaderSpi>emptySet());

    PoolPluginMock poolPlugin = mock(PoolPluginMock.class);
    when(poolPlugin.getName()).thenReturn(POOL_PLUGIN_NAME);
    when(poolPlugin.allocateReader(anyString())).thenReturn(reader);

    RemotePluginMock remotePlugin = mock(RemotePluginMock.class);
    when(remotePlugin.getName()).thenReturn(REMOTE_PLUGIN_NAME);

    ObservableRemotePluginMock observableRemotePlugin = mock(ObservableRemotePluginMock.class);
    when(observableRemotePlugin.getName()).thenReturn(REMOTE_PLUGIN_NAME);

    RemotePoolPluginMock remotePoolPlugin = mock(RemotePoolPluginMock.class);
    when(remotePoolPlugin.getName()).thenReturn(REMOTE_PLUGIN_NAME);

    pluginFactory = mock(PluginFactoryMock.class);
    when(pluginFactory.getPluginName()).thenReturn(PLUGIN_NAME);
    when(pluginFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(pluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(pluginFactory.getPlugin()).thenReturn(plugin);

    observablePluginFactory = mock(PluginFactoryMock.class);
    when(observablePluginFactory.getPluginName()).thenReturn(OBSERVABLE_PLUGIN_NAME);
    when(observablePluginFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(observablePluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(observablePluginFactory.getPlugin()).thenReturn(observablePlugin);

    autonomousObservablePluginFactory = mock(PluginFactoryMock.class);
    when(autonomousObservablePluginFactory.getPluginName())
        .thenReturn(AUTONOMOUS_OBSERVABLE_PLUGIN_NAME);
    when(autonomousObservablePluginFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(autonomousObservablePluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(autonomousObservablePluginFactory.getPlugin()).thenReturn(autonomousObservablePlugin);

    poolPluginFactory = mock(PoolPluginFactoryMock.class);
    when(poolPluginFactory.getPoolPluginName()).thenReturn(POOL_PLUGIN_NAME);
    when(poolPluginFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(poolPluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(poolPluginFactory.getPoolPlugin()).thenReturn(poolPlugin);

    remotePluginFactory = mock(RemotePluginFactoryMock.class);
    when(remotePluginFactory.getRemotePluginName()).thenReturn(REMOTE_PLUGIN_NAME);
    when(remotePluginFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(remotePluginFactory.getDistributedRemoteApiVersion())
        .thenReturn(DISTRIBUTED_REMOTE_API_VERSION);
    when(remotePluginFactory.getRemotePlugin()).thenReturn(remotePlugin);

    observableRemotePluginFactory = mock(ObservableRemotePluginFactoryMock.class);
    when(observableRemotePluginFactory.getRemotePluginName()).thenReturn(REMOTE_PLUGIN_NAME);
    when(observableRemotePluginFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(observableRemotePluginFactory.getDistributedRemoteApiVersion())
        .thenReturn(DISTRIBUTED_REMOTE_API_VERSION);
    when(observableRemotePluginFactory.getRemotePlugin()).thenReturn(observableRemotePlugin);

    remotePoolPluginFactory = mock(RemotePoolPluginFactoryMock.class);
    when(remotePoolPluginFactory.getRemotePluginName()).thenReturn(REMOTE_PLUGIN_NAME);
    when(remotePoolPluginFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(remotePoolPluginFactory.getDistributedRemoteApiVersion())
        .thenReturn(DISTRIBUTED_REMOTE_API_VERSION);
    when(remotePoolPluginFactory.getRemotePlugin()).thenReturn(remotePoolPlugin);

    cardExtension = mock(CardExtensionMock.class);
    when(cardExtension.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(cardExtension.getCardApiVersion()).thenReturn(CARD_API_VERSION);
    when(cardExtension.getReaderApiVersion()).thenReturn(READER_API_VERSION);

    DistributedLocalServiceMock localService = mock(DistributedLocalServiceMock.class);
    when(localService.getName()).thenReturn(LOCAL_SERVICE_NAME);

    localServiceFactory = mock(DistributedLocalServiceFactoryMock.class);
    when(localServiceFactory.getLocalServiceName()).thenReturn(LOCAL_SERVICE_NAME);
    when(localServiceFactory.getCommonApiVersion()).thenReturn(COMMON_API_VERSION);
    when(localServiceFactory.getDistributedLocalApiVersion())
        .thenReturn(DISTRIBUTED_LOCAL_API_VERSION);
    when(localServiceFactory.getLocalService()).thenReturn(localService);
  }

  @After
  public void tearDown() throws Exception {
    service.unregisterPlugin(PLUGIN_NAME);
    service.unregisterPlugin(OBSERVABLE_PLUGIN_NAME);
    service.unregisterPlugin(AUTONOMOUS_OBSERVABLE_PLUGIN_NAME);
    service.unregisterPlugin(POOL_PLUGIN_NAME);
    service.unregisterPlugin(REMOTE_PLUGIN_NAME);
    service.unregisterDistributedLocalService(LOCAL_SERVICE_NAME);
  }

  @Test
  public void getInstance_whenIsInvokedTwice_shouldReturnSameInstance() {
    assertThat(SmartCardServiceAdapter.getInstance()).isEqualTo(service);
  }

  // Register regular plugin

  @Test
  public void registerPlugin_whenPluginIsCorrect_shouldProducePlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(pluginFactory);
    assertThat(p).isInstanceOf(Plugin.class).isInstanceOf(LocalPluginAdapter.class);
    assertThat(service.getPluginNames()).contains(PLUGIN_NAME);
  }

  @Test
  public void
      registerPlugin_whenPluginIsObservable_shouldProduceObservablePlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(observablePluginFactory);
    assertThat(p)
        .isInstanceOf(ObservablePlugin.class)
        .isInstanceOf(ObservableLocalPluginAdapter.class);
    assertThat(service.getPluginNames()).contains(OBSERVABLE_PLUGIN_NAME);
  }

  @Test
  public void
      registerPlugin_whenPluginIsAutonomousObservable_shouldProduceAutonomousObservablePlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(autonomousObservablePluginFactory);
    assertThat(p)
        .isInstanceOf(ObservablePlugin.class)
        .isInstanceOf(AutonomousObservableLocalPluginAdapter.class);
    assertThat(service.getPluginNames()).contains(AUTONOMOUS_OBSERVABLE_PLUGIN_NAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void registerPlugin_whenFactoryIsNull_shouldThrowIAE() {
    service.registerPlugin(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void registerPlugin_whenFactoryDoesNotImplementSpi_shouldThrowIAE() {
    service.registerPlugin(new KeyplePluginExtensionFactory() {});
  }

  @Test
  public void registerPlugin_whenFactoryPluginNameMismatchesPluginName_shouldIAE_and_notRegister() {
    when(pluginFactory.getPluginName()).thenReturn("otherPluginName");
    try {
      service.registerPlugin(pluginFactory);
      shouldHaveThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }
    assertThat(service.getPluginNames()).doesNotContain(PLUGIN_NAME);
  }

  @Test
  public void registerPlugin_whenCommonApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(pluginFactory.getCommonApiVersion()).thenReturn("1.9");
    service.registerPlugin(pluginFactory);
    assertThat(service.getPluginNames()).contains(PLUGIN_NAME);
  }

  @Test
  public void registerPlugin_whenPluginApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(pluginFactory.getPluginApiVersion()).thenReturn("1.9");
    service.registerPlugin(pluginFactory);
    assertThat(service.getPluginNames()).contains(PLUGIN_NAME);
  }

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_whenInvokedTwice_shouldISE() {
    service.registerPlugin(pluginFactory);
    service.registerPlugin(pluginFactory);
  }

  @Test(expected = KeyplePluginException.class)
  public void registerPlugin_whenIoException_shouldThrowKeyplePluginException() throws Exception {
    when(plugin.searchAvailableReaders()).thenThrow(new PluginIOException("Plugin IO Exception"));
    service.registerPlugin(pluginFactory);
  }

  // Register pool plugin

  @Test
  public void
      registerPlugin_Pool_whenPluginIsCorrect_shouldProducePlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(poolPluginFactory);
    assertThat(p).isInstanceOf(PoolPlugin.class).isInstanceOf(LocalPoolPluginAdapter.class);
    assertThat(service.getPluginNames()).contains(POOL_PLUGIN_NAME);
  }

  @Test
  public void registerPlugin_Pool_whenPluginIsObservable_shouldBeRegistered_withoutWarning() {
    service.registerPlugin(poolPluginFactory);
    assertThat(service.getPluginNames()).contains(POOL_PLUGIN_NAME);
  }

  @Test
  public void
      registerPlugin_Pool_whenFactoryPluginNameMismatchesPluginName_shouldIAE_and_notRegister() {
    when(poolPluginFactory.getPoolPluginName()).thenReturn("otherPluginName");
    try {
      service.registerPlugin(poolPluginFactory);
      shouldHaveThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }
    assertThat(service.getPluginNames()).doesNotContain(POOL_PLUGIN_NAME);
  }

  @Test
  public void registerPlugin_Pool_whenCommonApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(poolPluginFactory.getCommonApiVersion()).thenReturn("1.9");
    service.registerPlugin(poolPluginFactory);
    assertThat(service.getPluginNames()).contains(POOL_PLUGIN_NAME);
  }

  @Test
  public void registerPlugin_Pool_whenPluginApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(poolPluginFactory.getPluginApiVersion()).thenReturn("1.9");
    service.registerPlugin(poolPluginFactory);
    assertThat(service.getPluginNames()).contains(POOL_PLUGIN_NAME);
  }

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_Pool_whenInvokedTwice_shouldISE() {
    service.registerPlugin(poolPluginFactory);
    service.registerPlugin(poolPluginFactory);
  }

  // Register remote plugin

  @Test
  public void registerPlugin_Remote_whenPluginIsCorrect_shouldBeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(remotePluginFactory);
    assertThat(p).isInstanceOf(Plugin.class).isInstanceOf(RemotePluginAdapter.class);
    assertThat(service.getPluginNames()).contains(REMOTE_PLUGIN_NAME);
  }

  @Test
  public void
      registerPlugin_Remote_whenPluginIsObservable_shouldProduceObservablePlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(observableRemotePluginFactory);
    assertThat(p)
        .isInstanceOf(ObservablePlugin.class)
        .isInstanceOf(ObservableRemotePluginAdapter.class);
    assertThat(service.getPluginNames()).contains(REMOTE_PLUGIN_NAME);
  }

  @Test
  public void
      registerPlugin_Remote_whenPluginIsPool_shouldProducePoolPlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(remotePoolPluginFactory);
    assertThat(p).isInstanceOf(PoolPlugin.class).isInstanceOf(RemotePoolPluginAdapter.class);
    assertThat(service.getPluginNames()).contains(REMOTE_PLUGIN_NAME);
  }

  @Test
  public void
      registerPlugin_Remote_whenFactoryPluginNameMismatchesPluginName_shouldIAE_and_notRegister() {
    when(remotePluginFactory.getRemotePluginName()).thenReturn("otherPluginName");
    try {
      service.registerPlugin(remotePluginFactory);
      shouldHaveThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }
    assertThat(service.getPluginNames()).doesNotContain(REMOTE_PLUGIN_NAME);
  }

  @Test
  public void registerPlugin_Remote_whenCommonApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(remotePluginFactory.getCommonApiVersion()).thenReturn("0.9");
    service.registerPlugin(remotePluginFactory);
    assertThat(service.getPluginNames()).contains(REMOTE_PLUGIN_NAME);
  }

  @Test
  public void registerPlugin_Remote_whenPluginApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(remotePluginFactory.getDistributedRemoteApiVersion()).thenReturn("2.0");
    service.registerPlugin(remotePluginFactory);
    assertThat(service.getPluginNames()).contains(REMOTE_PLUGIN_NAME);
  }

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_Remote_whenInvokedTwice_shouldISE() {
    service.registerPlugin(remotePluginFactory);
    service.registerPlugin(remotePluginFactory);
  }

  // Bad version format

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_whenApiVersionHasBadLength_shouldISE() {
    when(pluginFactory.getCommonApiVersion()).thenReturn("2.0.0");
    service.registerPlugin(pluginFactory);
  }

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_whenApiVersionHasBadFormat_shouldISE() {
    when(pluginFactory.getCommonApiVersion()).thenReturn("2.A");
    service.registerPlugin(pluginFactory);
  }

  // Unregister regular plugin

  @Test
  public void unregisterPlugin_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(PLUGIN_NAME);
  }

  @Test
  public void unregisterPlugin_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(pluginFactory);
    assertThat(service.getPluginNames()).contains(PLUGIN_NAME);
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(PLUGIN_NAME);
  }

  // Unregister pool plugin

  @Test
  public void unregisterPlugin_Pool_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(POOL_PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(POOL_PLUGIN_NAME);
  }

  @Test
  public void unregisterPlugin_Pool_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(poolPluginFactory);
    assertThat(service.getPluginNames()).contains(POOL_PLUGIN_NAME);
    service.unregisterPlugin(POOL_PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(POOL_PLUGIN_NAME);
  }

  // Unregister remote plugin

  @Test
  public void unregisterPlugin_Remote_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(REMOTE_PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(REMOTE_PLUGIN_NAME);
  }

  @Test
  public void unregisterPlugin_Remote_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(remotePluginFactory);
    assertThat(service.getPluginNames()).contains(REMOTE_PLUGIN_NAME);
    service.unregisterPlugin(REMOTE_PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(REMOTE_PLUGIN_NAME);
  }

  @Test
  public void getPlugin_fromPluginName_whenPluginIsNotRegistered_shouldReturnNull() {
    assertThat(service.getPlugin(PLUGIN_NAME)).isNull();
  }

  @Test
  public void getPlugin_fromPluginName_whenPluginIsRegistered_shouldReturnPluginInstance() {
    service.registerPlugin(pluginFactory);
    assertThat(service.getPlugin(PLUGIN_NAME)).isNotNull();
  }

  @Test
  public void getPlugin_fromCardReader_whenPluginIsNotRegistered_shouldReturnNull() {
    assertThat(service.getPlugin(mock(CardReader.class))).isNull();
  }

  @Test
  public void getPlugin_fromCardReader_whenReaderIsNotFound_shouldReturnNull() {
    service.registerPlugin(pluginFactory);
    assertThat(service.getPlugin(mock(CardReader.class))).isNull();
  }

  @Test
  public void getPlugin_fromCardReader_whenPluginIsRegistered_shouldReturnPluginInstance()
      throws Exception {
    when(plugin.searchAvailableReaders())
        .thenReturn(new HashSet<ReaderSpi>(Collections.singletonList(reader)));
    Plugin p = service.registerPlugin(pluginFactory);
    CardReader cardReader = service.getPlugin(PLUGIN_NAME).getReaders().iterator().next();
    assertThat(service.getPlugin(cardReader)).isSameAs(p);
  }

  @Test
  public void getReader_whenReaderDoesNotExist_shouldReturnNull() {
    assertThat(service.getReader(READER_NAME)).isNull();
  }

  @Test
  public void getReader_whenReaderExists_shouldReturnReaderInstance() throws Exception {
    when(plugin.searchAvailableReaders())
        .thenReturn(new HashSet<ReaderSpi>(Collections.singletonList(reader)));
    service.registerPlugin(pluginFactory);
    CardReader cardReader = service.getPlugin(PLUGIN_NAME).getReaders().iterator().next();
    assertThat(service.getReader(READER_NAME)).isSameAs(cardReader);
  }

  @Test
  public void findReader_whenReaderNameRegexMatches_returnsExistingReader() throws Exception {

    String readerNameRegex = "testReader.*";
    String readerName = "testReader123";

    when(reader.getName()).thenReturn(readerName);
    when(plugin.searchAvailableReaders())
        .thenReturn(new HashSet<ReaderSpi>(Collections.singletonList(reader)));

    service.registerPlugin(pluginFactory);

    // Act
    CardReader foundedReader = service.findReader(readerNameRegex);

    // Assert
    assertThat(foundedReader).isNotNull();
    assertThat(foundedReader.getName()).isEqualTo(readerName);
  }

  @Test
  public void findReader_whenNoReaderNameMatches_returnsNull() throws Exception {

    String readerNameRegex = "testReader.*";
    String readerName = "differentReader123";

    when(reader.getName()).thenReturn(readerName);
    when(plugin.searchAvailableReaders())
        .thenReturn(new HashSet<ReaderSpi>(Collections.singletonList(reader)));

    service.registerPlugin(pluginFactory);

    // Act
    CardReader foundedReader = service.findReader(readerNameRegex);

    // Assert
    assertThat(foundedReader).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void findReader_whenReaderNameRegexIsNotAValidPattern_throwsIllegalArgumentsException()
      throws Exception {

    String invalidReaderNameRegex = "[";

    when(plugin.searchAvailableReaders())
        .thenReturn(new HashSet<ReaderSpi>(Collections.singletonList(reader)));

    service.registerPlugin(pluginFactory);

    service.findReader(invalidReaderNameRegex);
  }

  @Test
  public void getPlugins_whenNoPluginRegistered_shouldReturnEmptyList() {
    assertThat(service.getPlugins()).isEmpty();
  }

  @Test
  public void getPlugins_whenTwoPluginsRegistered_shouldHaveTwoPlugins() {
    service.registerPlugin(pluginFactory);
    service.registerPlugin(poolPluginFactory);
    assertThat(service.getPlugins()).hasSize(2);
    assertThat(service.getPluginNames()).contains(PLUGIN_NAME);
    assertThat(service.getPluginNames()).contains(POOL_PLUGIN_NAME);
  }

  // Check card extension APIs

  @Test
  public void checkCardExtension_whenCommonApiDiffers_shouldLogWarn() {
    when(cardExtension.getCommonApiVersion()).thenReturn("0.9");
    service.checkCardExtension(cardExtension);
  }

  @Test
  public void checkCardExtension_whenReaderApiDiffers_shouldLogWarn() {
    when(cardExtension.getReaderApiVersion()).thenReturn("0.9");
    service.checkCardExtension(cardExtension);
  }

  @Test
  public void checkCardExtension_whenCardApiDiffers_shouldLogWarn() {
    when(cardExtension.getCardApiVersion()).thenReturn("0.9");
    service.checkCardExtension(cardExtension);
  }

  // Register distributed local service

  @Test
  public void
      registerDistributedLocalService_whenLocalServiceIsCorrect_shouldBeRegistered_withoutWarning() {
    service.registerDistributedLocalService(localServiceFactory);
  }

  @Test(expected = IllegalArgumentException.class)
  public void
      registerDistributedLocalService_whenFactoryServiceNameMismatchesServiceName_shouldIAE_and_notRegister() {
    when(localServiceFactory.getLocalServiceName()).thenReturn("otherServiceName");
    service.registerDistributedLocalService(localServiceFactory);
  }

  @Test
  public void
      registerDistributedLocalService_whenCommonApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(localServiceFactory.getCommonApiVersion()).thenReturn("0.9");
    service.registerDistributedLocalService(localServiceFactory);
  }

  @Test
  public void
      registerDistributedLocalService_whenDistributedLocalApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(localServiceFactory.getDistributedLocalApiVersion()).thenReturn("0.9");
    service.registerDistributedLocalService(localServiceFactory);
  }

  @Test(expected = IllegalStateException.class)
  public void registerDistributedLocalService_whenInvokedTwice_shouldISE() {
    service.registerDistributedLocalService(localServiceFactory);
    service.registerDistributedLocalService(localServiceFactory);
  }

  // Bad version format

  @Test(expected = IllegalStateException.class)
  public void registerDistributedLocalService_whenApiVersionHasBadLength_shouldISE() {
    when(localServiceFactory.getCommonApiVersion()).thenReturn("2.0.0");
    service.registerDistributedLocalService(localServiceFactory);
  }

  @Test(expected = IllegalStateException.class)
  public void registerDistributedLocalService_whenApiVersionHasBadFormat_shouldISE() {
    when(localServiceFactory.getCommonApiVersion()).thenReturn("2.A");
    service.registerDistributedLocalService(localServiceFactory);
  }

  // Unregister local service

  @Test
  public void unregisterDistributedLocalService_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(PLUGIN_NAME);
  }

  @Test
  public void unregisterDistributedLocalService_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(pluginFactory);
    assertThat(service.getPluginNames()).contains(PLUGIN_NAME);
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.getPluginNames()).doesNotContain(PLUGIN_NAME);
  }

  @Test
  public void getDistributedLocalService_whenServiceIsNotRegistered_shouldReturnNull() {
    assertThat(service.getDistributedLocalService(LOCAL_SERVICE_NAME)).isNull();
  }

  @Test
  public void getDistributedLocalService_whenServiceIsRegistered_shouldReturnPluginInstance() {
    service.registerDistributedLocalService(localServiceFactory);
    assertThat(service.getDistributedLocalService(LOCAL_SERVICE_NAME)).isNotNull();
  }

  @Test
  public void getReaderApiFactory_shouldReturnReaderApiFactoryInstance() {
    assertThat(service.getReaderApiFactory()).isInstanceOf(ReaderApiFactory.class);
  }
}
