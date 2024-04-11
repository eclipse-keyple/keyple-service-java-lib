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

import static org.eclipse.keyple.core.service.JsonAdapter.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;
import org.eclipse.keyple.core.common.CommonApiProperties;
import org.eclipse.keyple.core.common.KeypleCardExtension;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtensionFactory;
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory;
import org.eclipse.keyple.core.distributed.local.DistributedLocalApiProperties;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceFactorySpi;
import org.eclipse.keyple.core.distributed.local.spi.LocalServiceSpi;
import org.eclipse.keyple.core.distributed.remote.DistributedRemoteApiProperties;
import org.eclipse.keyple.core.distributed.remote.spi.*;
import org.eclipse.keyple.core.plugin.PluginApiProperties;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.*;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.AbstractApduException;
import org.eclipse.keypop.card.ApduResponseApi;
import org.eclipse.keypop.card.CardApiProperties;
import org.eclipse.keypop.card.CardResponseApi;
import org.eclipse.keypop.card.CardSelectionResponseApi;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.ReaderApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SmartCardService}.
 *
 * @since 2.0.0
 */
final class SmartCardServiceAdapter implements SmartCardService {

  private static final Logger logger = LoggerFactory.getLogger(SmartCardServiceAdapter.class);

  private static final SmartCardServiceAdapter INSTANCE = new SmartCardServiceAdapter();

  private final Map<String, Plugin> plugins = new ConcurrentHashMap<String, Plugin>();
  private final Object pluginMonitor = new Object();

  private final Map<String, DistributedLocalService> distributedLocalServices =
      new ConcurrentHashMap<String, DistributedLocalService>();
  private final Object distributedLocalServiceMonitor = new Object();

  static {
    // Register additional JSON adapters.
    JsonUtil.registerTypeAdapter(AbstractApduException.class, new ApduExceptionJsonAdapter(), true);
    JsonUtil.registerTypeAdapter(
        CardSelectionResponseApi.class,
        new CardSelectionResponseApiJsonDeserializerAdapter(),
        false);
    JsonUtil.registerTypeAdapter(
        CardResponseApi.class, new CardResponseApiJsonDeserializerAdapter(), false);
    JsonUtil.registerTypeAdapter(ApduResponseApi.class, new ApduResponseApiJsonAdapter(), false);
  }

  /** Private constructor. */
  private SmartCardServiceAdapter() {}

  /**
   * Gets the single instance of SmartCardServiceAdapter.
   *
   * @return single instance of SmartCardServiceAdapter
   * @since 2.0.0
   */
  static SmartCardServiceAdapter getInstance() {
    return INSTANCE;
  }

  /**
   * Compare versions.
   *
   * @param providedVersion The provided version string.
   * @param localVersion The local version string.
   * @return 0 if providedVersion equals localVersion, &lt; 0 if providedVersion &lt; localVersion,
   *     &gt; 0 if providedVersion &gt; localVersion.
   */
  private int compareVersions(String providedVersion, String localVersion) {
    String[] providedVersions = providedVersion.split("[.]");
    String[] localVersions = localVersion.split("[.]");
    if (providedVersions.length != localVersions.length) {
      throw new IllegalStateException(
          "Inconsistent version numbers: provided = "
              + providedVersion
              + ", local = "
              + localVersion);
    }
    Integer provided = 0;
    int local = 0;
    try {
      for (String v : providedVersions) {
        provided += Integer.parseInt(v);
        provided *= 1000;
      }
      for (String v : localVersions) {
        local += Integer.parseInt(v);
        local *= 1000;
      }
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Bad version numbers: provided = " + providedVersion + ", local = " + localVersion, e);
    }
    return provided.compareTo(local);
  }

  /**
   * Checks for consistency in the versions of external APIs shared by the plugin and the service.
   *
   * <p>Generates warnings into the log.
   *
   * @param pluginFactorySpi The plugin factory SPI.
   */
  private void checkPluginVersion(PluginFactorySpi pluginFactorySpi) {
    if (compareVersions(pluginFactorySpi.getCommonApiVersion(), CommonApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Common API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          pluginFactorySpi.getPluginName(),
          pluginFactorySpi.getCommonApiVersion(),
          CommonApiProperties.VERSION);
    }
    if (compareVersions(pluginFactorySpi.getPluginApiVersion(), PluginApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Plugin API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          pluginFactorySpi.getPluginName(),
          pluginFactorySpi.getPluginApiVersion(),
          PluginApiProperties.VERSION);
    }
  }

  /**
   * Checks for consistency in the versions of external APIs shared by the remote plugin and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param remotePluginFactorySpi The remote plugin factory SPI.
   */
  private void checkRemotePluginVersion(RemotePluginFactorySpi remotePluginFactorySpi) {
    if (compareVersions(remotePluginFactorySpi.getCommonApiVersion(), CommonApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Common API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          remotePluginFactorySpi.getRemotePluginName(),
          remotePluginFactorySpi.getCommonApiVersion(),
          CommonApiProperties.VERSION);
    }
    if (compareVersions(
            remotePluginFactorySpi.getDistributedRemoteApiVersion(),
            DistributedRemoteApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Distributed Remote Plugin API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          remotePluginFactorySpi.getRemotePluginName(),
          remotePluginFactorySpi.getDistributedRemoteApiVersion(),
          DistributedRemoteApiProperties.VERSION);
    }
  }

  /**
   * Checks for consistency in the versions of external APIs shared by the pool plugin and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param poolPluginFactorySpi The plugin factory SPI.
   */
  private void checkPoolPluginVersion(PoolPluginFactorySpi poolPluginFactorySpi) {
    if (compareVersions(poolPluginFactorySpi.getCommonApiVersion(), CommonApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Common API used by the provided pool plugin ({}:{}) mismatches the version used by the service ({}).",
          poolPluginFactorySpi.getPoolPluginName(),
          poolPluginFactorySpi.getCommonApiVersion(),
          CommonApiProperties.VERSION);
    }
    if (compareVersions(poolPluginFactorySpi.getPluginApiVersion(), PluginApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Plugin API used by the provided pool plugin ({}:{}) mismatches the version used by the service ({}).",
          poolPluginFactorySpi.getPoolPluginName(),
          poolPluginFactorySpi.getPluginApiVersion(),
          PluginApiProperties.VERSION);
    }
  }

  /**
   * Checks for consistency in the versions of external APIs shared by the card extension and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param cardExtension The card extension.
   */
  private void checkCardExtensionVersion(KeypleCardExtension cardExtension) {
    if (compareVersions(cardExtension.getCommonApiVersion(), CommonApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Common API used by the provided card extension ({}:{}) mismatches the version used by the service ({}).",
          cardExtension.getClass().getSimpleName(),
          cardExtension.getCommonApiVersion(),
          CommonApiProperties.VERSION);
    }
    if (compareVersions(cardExtension.getCardApiVersion(), CardApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Card API used by the provided card extension ({}:{}) mismatches the version used by the service ({}).",
          cardExtension.getClass().getSimpleName(),
          cardExtension.getCardApiVersion(),
          CardApiProperties.VERSION);
    }
    if (compareVersions(cardExtension.getReaderApiVersion(), ReaderApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Reader API used by the provided card extension ({}:{}) mismatches the version used by the service ({}).",
          cardExtension.getClass().getSimpleName(),
          cardExtension.getReaderApiVersion(),
          ReaderApiProperties.VERSION);
    }
  }

  /**
   * Checks for consistency in the versions of external APIs shared by the distributed local service
   * and the service.
   *
   * <p>Generates warnings into the log.
   *
   * @param localServiceFactorySpi The distributed local service factory SPI.
   */
  private void checkDistributedLocalServiceVersion(LocalServiceFactorySpi localServiceFactorySpi) {
    if (compareVersions(localServiceFactorySpi.getCommonApiVersion(), CommonApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Common API used by the provided distributed local service ({}:{}) mismatches the version used by the service ({}).",
          localServiceFactorySpi.getLocalServiceName(),
          localServiceFactorySpi.getCommonApiVersion(),
          CommonApiProperties.VERSION);
    }
    if (compareVersions(
            localServiceFactorySpi.getDistributedLocalApiVersion(),
            DistributedLocalApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Distributed Local API used by the provided distributed local service ({}:{}) mismatches the version used by the service ({}).",
          localServiceFactorySpi.getLocalServiceName(),
          localServiceFactorySpi.getDistributedLocalApiVersion(),
          DistributedLocalApiProperties.VERSION);
    }
  }

  /**
   * Checks if the plugin is already registered.
   *
   * @param pluginName The plugin name.
   * @throws IllegalStateException if the plugin is already registered.
   */
  private void checkPluginRegistration(String pluginName) {
    logger.info("Registering a new Plugin to the service : {}", pluginName);
    Assert.getInstance().notEmpty(pluginName, "pluginName");
    if (plugins.containsKey(pluginName)) {
      throw new IllegalStateException(
          String.format("Plugin '%s' has already been registered to the service.", pluginName));
    }
  }

  /**
   * Checks if the distributed local service is already registered.
   *
   * @param distributedLocalServiceName The distributed local service name.
   * @throws IllegalStateException if the distributed local service is already registered.
   */
  private void checkDistributedLocalServiceRegistration(String distributedLocalServiceName) {
    logger.info(
        "Registering a new distributed local service to the service : {}",
        distributedLocalServiceName);
    Assert.getInstance().notEmpty(distributedLocalServiceName, "distributedLocalServiceName");
    if (distributedLocalServices.containsKey(distributedLocalServiceName)) {
      throw new IllegalStateException(
          String.format(
              "Service '%s' has already been registered to the service.",
              distributedLocalServiceName));
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Plugin registerPlugin(KeyplePluginExtensionFactory pluginFactory) {

    Assert.getInstance().notNull(pluginFactory, "pluginFactory");

    AbstractPluginAdapter plugin = null;
    try {
      synchronized (pluginMonitor) {
        if (pluginFactory instanceof PluginFactorySpi) {
          plugin = createLocalPlugin((PluginFactorySpi) pluginFactory);

        } else if (pluginFactory instanceof PoolPluginFactorySpi) {
          plugin = createLocalPoolPlugin((PoolPluginFactorySpi) pluginFactory);

        } else if (pluginFactory instanceof RemotePluginFactorySpi) {
          plugin = createRemotePlugin((RemotePluginFactorySpi) pluginFactory);

        } else {
          throw new IllegalArgumentException("The factory doesn't implement the right SPI.");
        }

        plugins.put(plugin.getName(), plugin);
        plugin.register();
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "The provided plugin factory doesn't implement the plugin API properly.", e);

    } catch (PluginIOException e) {
      throw new KeyplePluginException(
          String.format(
              "Unable to register the plugin '%s' : %s", plugin.getName(), e.getMessage()),
          e);
    }
    return plugin;
  }

  /**
   * Creates an instance of local plugin.
   *
   * @param pluginFactorySpi The plugin factory SPI.
   * @return A not null reference.
   */
  private AbstractPluginAdapter createLocalPlugin(PluginFactorySpi pluginFactorySpi) {

    checkPluginRegistration(pluginFactorySpi.getPluginName());
    checkPluginVersion(pluginFactorySpi);

    PluginSpi pluginSpi = pluginFactorySpi.getPlugin();

    if (!pluginSpi.getName().equals(pluginFactorySpi.getPluginName())) {
      throw new IllegalArgumentException(
          String.format(
              "Plugin name '%s' mismatches the expected name '%s' provided by the factory",
              pluginSpi.getName(), pluginFactorySpi.getPluginName()));
    }

    AbstractPluginAdapter plugin;
    if (pluginSpi instanceof ObservablePluginSpi) {
      plugin = new ObservableLocalPluginAdapter((ObservablePluginSpi) pluginSpi);
    } else if (pluginSpi instanceof AutonomousObservablePluginSpi) {
      plugin =
          new AutonomousObservableLocalPluginAdapter((AutonomousObservablePluginSpi) pluginSpi);
    } else {
      plugin = new LocalPluginAdapter(pluginSpi);
    }
    return plugin;
  }

  /**
   * Creates an instance of local pool plugin.
   *
   * @param poolPluginFactorySpi The pool plugin factory SPI.
   * @return A not null reference.
   */
  private AbstractPluginAdapter createLocalPoolPlugin(PoolPluginFactorySpi poolPluginFactorySpi) {

    checkPluginRegistration(poolPluginFactorySpi.getPoolPluginName());
    checkPoolPluginVersion(poolPluginFactorySpi);

    PoolPluginSpi poolPluginSpi = poolPluginFactorySpi.getPoolPlugin();

    if (!poolPluginSpi.getName().equals(poolPluginFactorySpi.getPoolPluginName())) {
      throw new IllegalArgumentException(
          String.format(
              "Pool plugin name '%s' mismatches the expected name '%s' provided by the factory",
              poolPluginSpi.getName(), poolPluginFactorySpi.getPoolPluginName()));
    }

    return new LocalPoolPluginAdapter(poolPluginSpi);
  }

  /**
   * Creates an instance of remote plugin.
   *
   * @param remotePluginFactorySpi The plugin factory SPI.
   * @return A not null reference.
   */
  private AbstractPluginAdapter createRemotePlugin(RemotePluginFactorySpi remotePluginFactorySpi) {

    checkPluginRegistration(remotePluginFactorySpi.getRemotePluginName());
    checkRemotePluginVersion(remotePluginFactorySpi);

    AbstractRemotePluginSpi remotePluginSpi = remotePluginFactorySpi.getRemotePlugin();

    if (!remotePluginSpi.getName().equals(remotePluginFactorySpi.getRemotePluginName())) {
      throw new IllegalArgumentException(
          String.format(
              "Remote plugin name '%s' mismatches the expected name '%s' provided by the factory",
              remotePluginSpi.getName(), remotePluginFactorySpi.getRemotePluginName()));
    }

    AbstractPluginAdapter plugin;
    if (remotePluginSpi instanceof RemotePoolPluginSpi) {
      plugin = new RemotePoolPluginAdapter((RemotePoolPluginSpi) remotePluginSpi);
    } else if (remotePluginSpi instanceof ObservableRemotePluginSpi) {
      plugin = new ObservableRemotePluginAdapter((ObservableRemotePluginSpi) remotePluginSpi);
    } else {
      plugin = new RemotePluginAdapter((RemotePluginSpi) remotePluginSpi);
    }
    return plugin;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void unregisterPlugin(String pluginName) {
    logger.info("Unregistering a plugin from the service : {}", pluginName);
    synchronized (pluginMonitor) {
      Plugin plugin = plugins.get(pluginName);
      if (plugin != null) {
        try {
          ((AbstractPluginAdapter) plugin).unregister();
        } finally {
          plugins.remove(pluginName);
        }
      } else {
        logger.warn("Plugin '{}' is not registered", pluginName);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Set<String> getPluginNames() {
    return new HashSet<String>(plugins.keySet());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Set<Plugin> getPlugins() {
    return new HashSet<Plugin>(plugins.values());
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Plugin getPlugin(String pluginName) {
    return plugins.get(pluginName);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.0
   */
  @Override
  public Plugin getPlugin(CardReader cardReader) {
    for (Plugin plugin : plugins.values()) {
      for (CardReader reader : plugin.getReaders()) {
        if (reader == cardReader) {
          return plugin;
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.0
   */
  @Override
  public CardReader getReader(String readerName) {
    for (Plugin plugin : plugins.values()) {
      for (CardReader reader : plugin.getReaders()) {
        if (reader.getName().equals(readerName)) {
          return reader;
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.1.0
   */
  @Override
  public CardReader findReader(String readerNameRegex) {
    for (Plugin plugin : plugins.values()) {
      for (CardReader reader : plugin.getReaders()) {
        try {
          if (reader.getName().matches(readerNameRegex)) {
            return reader;
          }
        } catch (PatternSyntaxException e) {
          throw new IllegalArgumentException("readerNameRegex is invalid: " + e.getMessage(), e);
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  public void checkCardExtension(KeypleCardExtension cardExtension) {
    checkCardExtensionVersion(cardExtension);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public DistributedLocalService registerDistributedLocalService(
      KeypleDistributedLocalServiceExtensionFactory distributedLocalServiceExtensionFactory) {

    Assert.getInstance()
        .notNull(
            distributedLocalServiceExtensionFactory, "distributedLocalServiceExtensionFactory");

    DistributedLocalServiceAdapter distributedLocalService;
    try {
      if (!(distributedLocalServiceExtensionFactory instanceof LocalServiceFactorySpi)) {
        throw new IllegalArgumentException("The factory doesn't implement the right SPI.");
      }

      LocalServiceFactorySpi factory =
          (LocalServiceFactorySpi) distributedLocalServiceExtensionFactory;

      synchronized (distributedLocalServiceMonitor) {
        String localServiceName = factory.getLocalServiceName();

        checkDistributedLocalServiceRegistration(localServiceName);
        checkDistributedLocalServiceVersion(factory);

        LocalServiceSpi localServiceSpi = factory.getLocalService();

        if (!localServiceSpi.getName().equals(localServiceName)) {
          throw new IllegalArgumentException(
              String.format(
                  "The local service name '%s' mismatches the expected name '%s' provided by the factory",
                  localServiceSpi.getName(), localServiceName));
        }

        distributedLocalService = new DistributedLocalServiceAdapter(localServiceSpi);
        distributedLocalService.register();

        distributedLocalServices.put(localServiceName, distributedLocalService);
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "The provided distributed local service factory doesn't implement the distributed local service API properly.",
          e);
    }
    return distributedLocalService;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void unregisterDistributedLocalService(String distributedLocalServiceName) {
    logger.info(
        "Unregistering a distributed local service from the service : {}",
        distributedLocalServiceName);
    synchronized (distributedLocalServiceMonitor) {
      DistributedLocalService localService =
          distributedLocalServices.remove(distributedLocalServiceName);
      if (localService != null) {
        ((DistributedLocalServiceAdapter) localService).unregister();
      } else {
        logger.warn("Service '{}' is not registered", distributedLocalServiceName);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public DistributedLocalService getDistributedLocalService(String distributedLocalServiceName) {
    return distributedLocalServices.get(distributedLocalServiceName);
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public ReaderApiFactory getReaderApiFactory() {
    return new ReaderApiFactoryAdapter();
  }
}
