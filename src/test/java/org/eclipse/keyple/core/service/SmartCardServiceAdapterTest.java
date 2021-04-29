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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Collections;
import org.eclipse.keyple.core.card.spi.CardExtensionSpi;
import org.eclipse.keyple.core.common.*;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceFactorySpi;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginFactorySpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.*;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SmartCardServiceAdapter.class, LoggerFactory.class})
public class SmartCardServiceAdapterTest {

  private static Logger logger;
  private static SmartCardServiceAdapter service;

  private static final String PLUGIN_NAME = "plugin";
  private static final String OBSERVABLE_PLUGIN_NAME = "observablePlugin";
  private static final String AUTONOMOUS_OBSERVABLE_PLUGIN_NAME = "autonomousObservablePlugin";
  private static final String POOL_PLUGIN_NAME = "poolPlugin";
  private static final String REMOTE_PLUGIN_NAME = "remotePlugin";
  private static final String READER_NAME = "reader";
  private static final String LOCAL_SERVICE_NAME = "localService";

  private static final String SERVICE_API_VERSION = "2.0";
  private static final String COMMONS_API_VERSION = "2.0";
  private static final String PLUGIN_API_VERSION = "2.0";
  private static final String DISTRIBUTED_REMOTE_API_VERSION = "2.0";
  private static final String DISTRIBUTED_LOCAL_API_VERSION = "2.0";
  private static final String CARD_API_VERSION = "2.0";

  private PluginMock plugin;
  private ObservablePluginMock observablePlugin;
  private AutonomousObservablePluginMock autonomousObservablePlugin;
  private PoolPluginMock poolPlugin;
  private RemotePluginMock remotePlugin;
  private ReaderMock reader;
  private PluginFactoryMock pluginFactory;
  private PluginFactoryMock observablePluginFactory;
  private PluginFactoryMock autonomousObservablePluginFactory;
  private PoolPluginFactoryMock poolPluginFactory;
  private RemotePluginFactoryMock remotePluginFactory;
  private CardExtensionMock cardExtension;
  private DistributedLocalServiceMock localService;
  private DistributedLocalServiceFactoryMock localServiceFactory;

  interface ReaderMock extends Reader, ReaderSpi {}

  interface PluginMock extends KeyplePluginExtension, PluginSpi {}

  interface ObservablePluginMock extends KeyplePluginExtension, ObservablePluginSpi {}

  interface AutonomousObservablePluginMock
      extends KeyplePluginExtension, AutonomousObservablePluginSpi {}

  interface PoolPluginMock extends KeyplePluginExtension, PoolPluginSpi {}

  interface RemotePluginMock extends KeyplePluginExtension, RemotePluginSpi {}

  interface PluginFactoryMock extends KeyplePluginExtensionFactory, PluginFactorySpi {}

  interface PoolPluginFactoryMock extends KeyplePluginExtensionFactory, PoolPluginFactorySpi {}

  interface RemotePluginFactoryMock extends KeyplePluginExtensionFactory, RemotePluginFactorySpi {}

  interface CardExtensionMock extends KeypleCardExtension, CardExtensionSpi {}

  interface DistributedLocalServiceMock
      extends KeypleDistributedLocalServiceExtension, LocalServiceSpi {}
  ;

  interface DistributedLocalServiceFactoryMock
      extends KeypleDistributedLocalServiceExtensionFactory, LocalServiceFactorySpi {}
  ;

  @BeforeClass
  public static void beforeClass() throws Exception {
    mockStatic(LoggerFactory.class);
    logger = mock(Logger.class);
    when(LoggerFactory.getLogger(any(Class.class))).thenReturn(logger);
    service = SmartCardServiceAdapter.getInstance();
  }

  @Before
  public void setUp() throws Exception {
    reader = mock(ReaderMock.class);
    when(reader.getName()).thenReturn(READER_NAME);

    plugin = mock(PluginMock.class);
    when(plugin.getName()).thenReturn(PLUGIN_NAME);
    when(plugin.searchAvailableReaders()).thenReturn(Collections.<ReaderSpi>emptySet());

    observablePlugin = mock(ObservablePluginMock.class);
    when(observablePlugin.getName()).thenReturn(OBSERVABLE_PLUGIN_NAME);
    when(observablePlugin.searchAvailableReaders()).thenReturn(Collections.<ReaderSpi>emptySet());

    autonomousObservablePlugin = mock(AutonomousObservablePluginMock.class);
    when(autonomousObservablePlugin.getName()).thenReturn(AUTONOMOUS_OBSERVABLE_PLUGIN_NAME);
    when(autonomousObservablePlugin.searchAvailableReaders())
        .thenReturn(Collections.<ReaderSpi>emptySet());

    poolPlugin = mock(PoolPluginMock.class);
    when(poolPlugin.getName()).thenReturn(POOL_PLUGIN_NAME);
    when(poolPlugin.allocateReader(anyString())).thenReturn(reader);

    remotePlugin = mock(RemotePluginMock.class);
    when(remotePlugin.getName()).thenReturn(REMOTE_PLUGIN_NAME);

    pluginFactory = mock(PluginFactoryMock.class);
    when(pluginFactory.getPluginName()).thenReturn(PLUGIN_NAME);
    when(pluginFactory.getCommonsApiVersion()).thenReturn(COMMONS_API_VERSION);
    when(pluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(pluginFactory.getPlugin()).thenReturn(plugin);

    observablePluginFactory = mock(PluginFactoryMock.class);
    when(observablePluginFactory.getPluginName()).thenReturn(OBSERVABLE_PLUGIN_NAME);
    when(observablePluginFactory.getCommonsApiVersion()).thenReturn(COMMONS_API_VERSION);
    when(observablePluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(observablePluginFactory.getPlugin()).thenReturn(observablePlugin);

    autonomousObservablePluginFactory = mock(PluginFactoryMock.class);
    when(autonomousObservablePluginFactory.getPluginName())
        .thenReturn(AUTONOMOUS_OBSERVABLE_PLUGIN_NAME);
    when(autonomousObservablePluginFactory.getCommonsApiVersion()).thenReturn(COMMONS_API_VERSION);
    when(autonomousObservablePluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(autonomousObservablePluginFactory.getPlugin()).thenReturn(autonomousObservablePlugin);

    poolPluginFactory = mock(PoolPluginFactoryMock.class);
    when(poolPluginFactory.getPoolPluginName()).thenReturn(POOL_PLUGIN_NAME);
    when(poolPluginFactory.getCommonsApiVersion()).thenReturn(COMMONS_API_VERSION);
    when(poolPluginFactory.getPluginApiVersion()).thenReturn(PLUGIN_API_VERSION);
    when(poolPluginFactory.getPoolPlugin()).thenReturn(poolPlugin);

    remotePluginFactory = mock(RemotePluginFactoryMock.class);
    when(remotePluginFactory.getRemotePluginName()).thenReturn(REMOTE_PLUGIN_NAME);
    when(remotePluginFactory.getCommonsApiVersion()).thenReturn(COMMONS_API_VERSION);
    when(remotePluginFactory.getDistributedRemoteApiVersion())
        .thenReturn(DISTRIBUTED_REMOTE_API_VERSION);
    when(remotePluginFactory.getRemotePlugin()).thenReturn(remotePlugin);

    cardExtension = mock(CardExtensionMock.class);
    when(cardExtension.getCommonsApiVersion()).thenReturn(COMMONS_API_VERSION);
    when(cardExtension.getCardApiVersion()).thenReturn(CARD_API_VERSION);
    when(cardExtension.getServiceApiVersion()).thenReturn(SERVICE_API_VERSION);

    localService = mock(DistributedLocalServiceMock.class);
    when(localService.getName()).thenReturn(LOCAL_SERVICE_NAME);

    localServiceFactory = mock(DistributedLocalServiceFactoryMock.class);
    when(localServiceFactory.getLocalServiceName()).thenReturn(LOCAL_SERVICE_NAME);
    when(localServiceFactory.getCommonsApiVersion()).thenReturn(COMMONS_API_VERSION);
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
    reset(logger);
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
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void
      registerPlugin_whenPluginIsObservable_shouldProduceObservablePlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(observablePluginFactory);
    assertThat(p)
        .isInstanceOf(ObservablePlugin.class)
        .isInstanceOf(ObservableLocalPluginAdapter.class);
    assertThat(service.isPluginRegistered(OBSERVABLE_PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void
      registerPlugin_whenPluginIsAutonomousObservable_shouldProduceAutonomousObservablePlugin_BeRegistered_withoutWarning() {
    Plugin p = service.registerPlugin(autonomousObservablePluginFactory);
    assertThat(p)
        .isInstanceOf(ObservablePlugin.class)
        .isInstanceOf(AutonomousObservableLocalPluginAdapter.class);
    assertThat(service.isPluginRegistered(AUTONOMOUS_OBSERVABLE_PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void registerPlugin_whenFactoryIsNull_shouldThrowIAE() {
    service.registerPlugin(null);
  }

  @Test
  public void registerPlugin_whenFactoryPluginNameMismatchesPluginName_shouldIAE_and_notRegister() {
    when(pluginFactory.getPluginName()).thenReturn("otherPluginName");
    try {
      service.registerPlugin(pluginFactory);
      shouldHaveThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isFalse();
  }

  @Test
  public void registerPlugin_whenCommonsApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(pluginFactory.getCommonsApiVersion()).thenReturn("2.1");
    service.registerPlugin(pluginFactory);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isTrue();
    verify(logger).warn(anyString(), eq(PLUGIN_NAME), eq("2.1"), eq(COMMONS_API_VERSION));
  }

  @Test
  public void registerPlugin_whenPluginApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(pluginFactory.getPluginApiVersion()).thenReturn("2.1");
    service.registerPlugin(pluginFactory);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isTrue();
    verify(logger).warn(anyString(), eq(PLUGIN_NAME), eq("2.1"), eq(PLUGIN_API_VERSION));
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
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void registerPlugin_Pool_whenPluginIsObservable_shouldBeRegistered_withoutWarning() {
    service.registerPlugin(poolPluginFactory);
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
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
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isFalse();
  }

  @Test
  public void registerPlugin_Pool_whenCommonsApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(poolPluginFactory.getCommonsApiVersion()).thenReturn("2.1");
    service.registerPlugin(poolPluginFactory);
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isTrue();
    verify(logger).warn(anyString(), eq(POOL_PLUGIN_NAME), eq("2.1"), eq(COMMONS_API_VERSION));
  }

  @Test
  public void registerPlugin_Pool_whenPluginApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(poolPluginFactory.getPluginApiVersion()).thenReturn("2.1");
    service.registerPlugin(poolPluginFactory);
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isTrue();
    verify(logger).warn(anyString(), eq(POOL_PLUGIN_NAME), eq("2.1"), eq(PLUGIN_API_VERSION));
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
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void
      registerPlugin_Remote_whenPluginIsObservable_shouldProduceObservablePlugin_BeRegistered_withoutWarning() {
    when(remotePlugin.isObservable()).thenReturn(true);
    Plugin p = service.registerPlugin(remotePluginFactory);
    assertThat(p)
        .isInstanceOf(ObservablePlugin.class)
        .isInstanceOf(ObservableRemotePluginAdapter.class);
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void
      registerPlugin_Remote_whenPluginIsPool_shouldProducePoolPlugin_BeRegistered_withoutWarning() {
    when(remotePluginFactory.isPoolPlugin()).thenReturn(true);
    Plugin p = service.registerPlugin(remotePluginFactory);
    assertThat(p).isInstanceOf(PoolPlugin.class).isInstanceOf(RemotePoolPluginAdapter.class);
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
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
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isFalse();
  }

  @Test
  public void registerPlugin_Remote_whenCommonsApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(remotePluginFactory.getCommonsApiVersion()).thenReturn("2.1");
    service.registerPlugin(remotePluginFactory);
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isTrue();
    verify(logger).warn(anyString(), eq(REMOTE_PLUGIN_NAME), eq("2.1"), eq(COMMONS_API_VERSION));
  }

  @Test
  public void registerPlugin_Remote_whenPluginApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(remotePluginFactory.getDistributedRemoteApiVersion()).thenReturn("2.1");
    service.registerPlugin(remotePluginFactory);
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isTrue();
    verify(logger)
        .warn(anyString(), eq(REMOTE_PLUGIN_NAME), eq("2.1"), eq(DISTRIBUTED_REMOTE_API_VERSION));
  }

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_Remote_whenInvokedTwice_shouldISE() {
    service.registerPlugin(remotePluginFactory);
    service.registerPlugin(remotePluginFactory);
  }

  // Bad version format

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_whenApiVersionHasBadLength_shouldISE() {
    when(pluginFactory.getCommonsApiVersion()).thenReturn("2.0.0");
    service.registerPlugin(pluginFactory);
  }

  @Test(expected = IllegalStateException.class)
  public void registerPlugin_whenApiVersionHasBadFormat_shouldISE() {
    when(pluginFactory.getCommonsApiVersion()).thenReturn("2.A");
    service.registerPlugin(pluginFactory);
  }

  // Unregister regular plugin

  @Test
  public void unregisterPlugin_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isFalse();
  }

  @Test
  public void unregisterPlugin_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(pluginFactory);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isTrue();
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isFalse();
  }

  // Unregister pool plugin

  @Test
  public void unregisterPlugin_Pool_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(POOL_PLUGIN_NAME);
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isFalse();
  }

  @Test
  public void unregisterPlugin_Pool_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(poolPluginFactory);
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isTrue();
    service.unregisterPlugin(POOL_PLUGIN_NAME);
    assertThat(service.isPluginRegistered(POOL_PLUGIN_NAME)).isFalse();
  }

  // Unregister remote plugin

  @Test
  public void unregisterPlugin_Remote_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(REMOTE_PLUGIN_NAME);
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isFalse();
  }

  @Test
  public void unregisterPlugin_Remote_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(remotePluginFactory);
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isTrue();
    service.unregisterPlugin(REMOTE_PLUGIN_NAME);
    assertThat(service.isPluginRegistered(REMOTE_PLUGIN_NAME)).isFalse();
  }

  @Test
  public void getPlugin_whenPluginIsNotRegistered_shouldReturnNull() {
    assertThat(service.getPlugin(PLUGIN_NAME)).isNull();
  }

  @Test
  public void getPlugin_whenPluginIsRegistered_shouldPluginInstance() {
    service.registerPlugin(pluginFactory);
    assertThat(service.getPlugin(PLUGIN_NAME)).isNotNull();
  }

  @Test
  public void getPlugins_whenNoPluginRegistered_shouldReturnEmptyList() {
    assertThat(service.getPlugins()).isEmpty();
  }

  @Test
  public void getPlugins_whenTwoPluginsRegistered_shouldTwoPlugins() {
    service.registerPlugin(pluginFactory);
    service.registerPlugin(poolPluginFactory);
    assertThat(service.getPlugins()).hasSize(2);
    assertThat(service.getPlugins().get(PLUGIN_NAME)).isNotNull();
    assertThat(service.getPlugins().get(POOL_PLUGIN_NAME)).isNotNull();
  }

  // Check card extension APIs

  @Test
  public void checkCardExtension_whenCommonsApiDiffers_shouldLogWarn() {
    when(cardExtension.getCommonsApiVersion()).thenReturn("2.1");
    service.checkCardExtension(cardExtension);
    verify(logger).warn(anyString(), eq("2.1"), eq(COMMONS_API_VERSION));
  }

  @Test
  public void checkCardExtension_whenServiceApiDiffers_shouldLogWarn() {
    when(cardExtension.getServiceApiVersion()).thenReturn("2.1");
    service.checkCardExtension(cardExtension);
    verify(logger).warn(anyString(), eq("2.1"), eq(SERVICE_API_VERSION));
  }

  @Test
  public void checkCardExtension_whenCardApiDiffers_shouldLogWarn() {
    when(cardExtension.getCardApiVersion()).thenReturn("2.1");
    service.checkCardExtension(cardExtension);
    verify(logger).warn(anyString(), eq("2.1"), eq(CARD_API_VERSION));
  }

  // Register distributed local service

  @Test
  public void
      registerDistributedLocalService_whenLocalServiceIsCorrect_shouldBeRegistered_withoutWarning() {
    service.registerDistributedLocalService(localServiceFactory);
    assertThat(service.isDistributedLocalServiceRegistered(LOCAL_SERVICE_NAME)).isTrue();
    verify(logger, times(0)).warn(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void
      registerDistributedLocalService_whenFactoryServiceNameMismatchesServiceName_shouldIAE_and_notRegister() {
    when(localServiceFactory.getLocalServiceName()).thenReturn("otherServiceName");
    try {
      service.registerDistributedLocalService(localServiceFactory);
      shouldHaveThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
    }
    assertThat(service.isDistributedLocalServiceRegistered(LOCAL_SERVICE_NAME)).isFalse();
  }

  @Test
  public void
      registerDistributedLocalService_whenCommonsApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(localServiceFactory.getCommonsApiVersion()).thenReturn("2.1");
    service.registerDistributedLocalService(localServiceFactory);
    assertThat(service.isDistributedLocalServiceRegistered(LOCAL_SERVICE_NAME)).isTrue();
    verify(logger).warn(anyString(), eq(LOCAL_SERVICE_NAME), eq("2.1"), eq(COMMONS_API_VERSION));
  }

  @Test
  public void
      registerDistributedLocalService_whenDistributedLocalApiVersionDiffers_shouldRegister_and_LogWarn() {
    when(localServiceFactory.getDistributedLocalApiVersion()).thenReturn("2.1");
    service.registerDistributedLocalService(localServiceFactory);
    assertThat(service.isDistributedLocalServiceRegistered(LOCAL_SERVICE_NAME)).isTrue();
    verify(logger)
        .warn(anyString(), eq(LOCAL_SERVICE_NAME), eq("2.1"), eq(DISTRIBUTED_LOCAL_API_VERSION));
  }

  @Test(expected = IllegalStateException.class)
  public void registerDistributedLocalService_whenInvokedTwice_shouldISE() {
    service.registerDistributedLocalService(localServiceFactory);
    service.registerDistributedLocalService(localServiceFactory);
  }

  // Bad version format

  @Test(expected = IllegalStateException.class)
  public void registerDistributedLocalService_whenApiVersionHasBadLength_shouldISE() {
    when(localServiceFactory.getCommonsApiVersion()).thenReturn("2.0.0");
    service.registerDistributedLocalService(localServiceFactory);
  }

  @Test(expected = IllegalStateException.class)
  public void registerDistributedLocalService_whenApiVersionHasBadFormat_shouldISE() {
    when(localServiceFactory.getCommonsApiVersion()).thenReturn("2.A");
    service.registerDistributedLocalService(localServiceFactory);
  }

  // Unregister local service

  @Test
  public void unregisterDistributedLocalService_whenPluginIsNotRegistered_shouldDoNothing() {
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isFalse();
  }

  @Test
  public void unregisterDistributedLocalService_whenPluginIsRegistered_shouldUnregister() {
    service.registerPlugin(pluginFactory);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isTrue();
    service.unregisterPlugin(PLUGIN_NAME);
    assertThat(service.isPluginRegistered(PLUGIN_NAME)).isFalse();
  }

  @Test
  public void getDistributedLocalService_whenServiceIsNotRegistered_shouldReturnNull() {
    assertThat(service.getDistributedLocalService(LOCAL_SERVICE_NAME)).isNull();
  }

  @Test
  public void getDistributedLocalService_whenServiceIsRegistered_shouldPluginInstance() {
    service.registerDistributedLocalService(localServiceFactory);
    assertThat(service.getDistributedLocalService(LOCAL_SERVICE_NAME)).isNotNull();
  }
}
