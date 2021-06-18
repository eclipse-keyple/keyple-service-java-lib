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
import org.calypsonet.terminal.card.AbstractApduException;
import org.calypsonet.terminal.card.CardApiProperties;
import org.calypsonet.terminal.reader.ReaderApiProperties;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.eclipse.keyple.core.common.CommonsApiProperties;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of {@link SmartCardService}.
 *
 * @since 2.0
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
    JsonUtil.registerTypeAdapter(
        AbstractApduException.class, new ApduExceptionJsonSerializerAdapter(), true);
  }

  /** Private constructor. */
  private SmartCardServiceAdapter() {}

  /**
   * (package-private)<br>
   * Gets the single instance of SmartCardServiceAdapter.
   *
   * @return single instance of SmartCardServiceAdapter
   * @since 2.0
   */
  static SmartCardServiceAdapter getInstance() {
    return INSTANCE;
  }

  /**
   * (private)<br>
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
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the plugin and the service.
   *
   * <p>Generates warnings into the log.
   *
   * @param pluginFactorySpi The plugin factory SPI.
   */
  private void checkPluginVersion(PluginFactorySpi pluginFactorySpi) {
    if (compareVersions(pluginFactorySpi.getCommonsApiVersion(), CommonsApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Commons API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          pluginFactorySpi.getPluginName(),
          pluginFactorySpi.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
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
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the remote plugin and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param remotePluginFactorySpi The remote plugin factory SPI.
   */
  private void checkRemotePluginVersion(RemotePluginFactorySpi remotePluginFactorySpi) {
    if (compareVersions(remotePluginFactorySpi.getCommonsApiVersion(), CommonsApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Commons API used by the provided plugin ({}:{}) mismatches the version used by the service ({}).",
          remotePluginFactorySpi.getRemotePluginName(),
          remotePluginFactorySpi.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
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
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the pool plugin and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param poolPluginFactorySpi The plugin factory SPI.
   */
  private void checkPoolPluginVersion(PoolPluginFactorySpi poolPluginFactorySpi) {
    if (compareVersions(poolPluginFactorySpi.getCommonsApiVersion(), CommonsApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Commons API used by the provided pool plugin ({}:{}) mismatches the version used by the service ({}).",
          poolPluginFactorySpi.getPoolPluginName(),
          poolPluginFactorySpi.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
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
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the card extension and the
   * service.
   *
   * <p>Generates warnings into the log.
   *
   * @param cardExtension The card extension.
   */
  private void checkCardExtensionVersion(KeypleCardExtension cardExtension) {
    if (compareVersions(cardExtension.getCommonsApiVersion(), CommonsApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Commons API used by the provided card extension ({}) mismatches the version used by the service ({}).",
          cardExtension.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
    }
    if (compareVersions(cardExtension.getCardApiVersion(), CardApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Card API used by the provided card extension ({}) mismatches the version used by the service ({}).",
          cardExtension.getCardApiVersion(),
          CardApiProperties.VERSION);
    }
    if (compareVersions(cardExtension.getReaderApiVersion(), ReaderApiProperties.VERSION) != 0) {
      logger.warn(
          "The version of Service API used by the provided card extension ({}) mismatches the version used by the service ({}).",
          cardExtension.getReaderApiVersion(),
          ReaderApiProperties.VERSION);
    }
  }

  /**
   * (private)<br>
   * Checks for consistency in the versions of external APIs shared by the distributed local service
   * and the service.
   *
   * <p>Generates warnings into the log.
   *
   * @param localServiceFactorySpi The distributed local service factory SPI.
   */
  private void checkDistributedLocalServiceVersion(LocalServiceFactorySpi localServiceFactorySpi) {
    if (compareVersions(localServiceFactorySpi.getCommonsApiVersion(), CommonsApiProperties.VERSION)
        != 0) {
      logger.warn(
          "The version of Commons API used by the provided distributed local service ({}:{}) mismatches the version used by the service ({}).",
          localServiceFactorySpi.getLocalServiceName(),
          localServiceFactorySpi.getCommonsApiVersion(),
          CommonsApiProperties.VERSION);
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
          String.format("The plugin '%s' has already been registered to the service.", pluginName));
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
    if (isDistributedLocalServiceRegistered(distributedLocalServiceName)) {
      throw new IllegalStateException(
          String.format(
              "The distributed local service '%s' has already been registered to the service.",
              distributedLocalServiceName));
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
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

        plugin.register();
        plugins.put(plugin.getName(), plugin);
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
   * (private)<br>
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
              "The plugin name '%s' mismatches the expected name '%s' provided by the factory",
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
   * (private)<br>
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
              "The pool plugin name '%s' mismatches the expected name '%s' provided by the factory",
              poolPluginSpi.getName(), poolPluginFactorySpi.getPoolPluginName()));
    }

    return new LocalPoolPluginAdapter(poolPluginSpi);
  }

  /**
   * (private)<br>
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
              "The remote plugin name '%s' mismatches the expected name '%s' provided by the factory",
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
   * @since 2.0
   */
  @Override
  public void unregisterPlugin(String pluginName) {
    logger.info("Unregistering a plugin from the service : {}", pluginName);
    synchronized (pluginMonitor) {
      Plugin removedPlugin = plugins.remove(pluginName);
      if (removedPlugin != null) {
        ((AbstractPluginAdapter) removedPlugin).unregister();
      } else {
        logger.warn("The plugin '{}' is not registered", pluginName);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Set<String> getPluginNames() {
    synchronized (pluginMonitor) {
      return new HashSet<String>(plugins.keySet());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Set<Plugin> getPlugins() {
    synchronized (pluginMonitor) {
      return new HashSet<Plugin>(plugins.values());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Plugin getPlugin(String pluginName) {
    synchronized (pluginMonitor) {
      return plugins.get(pluginName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  public void checkCardExtension(KeypleCardExtension cardExtension) {
    checkCardExtensionVersion(cardExtension);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
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
   * @since 2.0
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
        logger.warn(
            "The distributed local service '{}' is not registered", distributedLocalServiceName);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public boolean isDistributedLocalServiceRegistered(String distributedLocalServiceName) {
    synchronized (distributedLocalServiceMonitor) {
      return distributedLocalServices.containsKey(distributedLocalServiceName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public DistributedLocalService getDistributedLocalService(String distributedLocalServiceName) {
    synchronized (distributedLocalServiceMonitor) {
      return distributedLocalServices.get(distributedLocalServiceName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardSelectionManager createCardSelectionManager() {
    return new CardSelectionManagerAdapter();
  }
}
