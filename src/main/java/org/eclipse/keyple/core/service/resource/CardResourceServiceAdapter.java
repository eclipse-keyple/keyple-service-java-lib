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
package org.eclipse.keyple.core.service.resource;

import static org.eclipse.keyple.core.service.resource.PluginsConfigurator.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.service.spi.ReaderObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of {@link CardResourceService}.
 *
 * @since 2.0
 */
final class CardResourceServiceAdapter
    implements CardResourceService, PluginObserverSpi, ReaderObserverSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardResourceServiceAdapter.class);

  /** Singleton instance */
  private static final CardResourceServiceAdapter instance = new CardResourceServiceAdapter();

  /** Map an accepted reader of a "regular" plugin to a reader manager. */
  private final Map<Reader, ReaderManagerAdapter> readerToReaderManagerMap =
      new ConcurrentHashMap<Reader, ReaderManagerAdapter>();

  /** Map a configured card profile name to a card profile manager. */
  private final Map<String, CardProfileManagerAdapter> cardProfileNameToCardProfileManagerMap =
      new ConcurrentHashMap<String, CardProfileManagerAdapter>();

  /**
   * Map a card resource to a "pool plugin".<br>
   * A card resource associated to a "pool plugin" is only present in this map for the time of its
   * use and is not referenced by any card profile manager.
   */
  private final Map<CardResource, PoolPlugin> cardResourceToPoolPluginMap =
      new ConcurrentHashMap<CardResource, PoolPlugin>();

  /**
   * Map a "regular" plugin to its accepted observable readers referenced by at least one card
   * profile manager.<br>
   * This map is useful to observe only the accepted readers in case of a card monitoring request.
   */
  private final Map<Plugin, Set<ObservableReader>> pluginToObservableReadersMap =
      new ConcurrentHashMap<Plugin, Set<ObservableReader>>();

  /** The current configuration. */
  private CardResourceServiceConfiguratorAdapter configurator;

  /** The current status of the card resource service. */
  private volatile boolean isStarted;

  /**
   * (package-private)<br>
   * Gets the unique instance.
   *
   * @return A not null reference.
   * @since 2.0
   */
  static CardResourceServiceAdapter getInstance() {
    return instance;
  }

  /**
   * (package-private)<br>
   * Gets a string representation of the provided card resource.
   *
   * @param cardResource The card resource.
   * @return Null if the provided card resource is null.
   * @since 2.0
   */
  static String getCardResourceInfo(CardResource cardResource) {
    if (cardResource != null) {
      return new StringBuilder()
          .append("card resource (")
          .append(Integer.toHexString(System.identityHashCode(cardResource)))
          .append(") - reader '")
          .append(cardResource.getReader().getName())
          .append("' (")
          .append(Integer.toHexString(System.identityHashCode(cardResource.getReader())))
          .append(") - smart card (")
          .append(Integer.toHexString(System.identityHashCode(cardResource.getSmartCard())))
          .append(")")
          .toString();
    }
    return null;
  }

  /**
   * (package-private)<br>
   * Gets the reader manager associated to the provided reader.
   *
   * @param reader The associated reader.
   * @return Null if there is no reader manager associated.
   * @since 2.0
   */
  ReaderManagerAdapter getReaderManager(Reader reader) {
    return readerToReaderManagerMap.get(reader);
  }

  /**
   * (package-private)<br>
   * Associates a card resource to a "pool" plugin.
   *
   * @param cardResource The card resource to register.
   * @param poolPlugin The associated pool plugin.
   * @since 2.0
   */
  void registerPoolCardResource(CardResource cardResource, PoolPlugin poolPlugin) {
    cardResourceToPoolPluginMap.put(cardResource, poolPlugin);
  }

  /**
   * (package-private)<br>
   * Configures the card resource service.
   *
   * <p>If service is started, then stops the service, applies the configuration and starts the
   * service.
   *
   * <p>If not, then only applies the configuration.
   *
   * @since 2.0
   */
  void configure(CardResourceServiceConfiguratorAdapter configurator) {
    logger.info("Applying a new configuration...");
    if (isStarted) {
      stop();
      this.configurator = configurator;
      start();
    } else {
      this.configurator = configurator;
    }
    logger.info("New configuration applied");
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator getConfigurator() {
    return new CardResourceServiceConfiguratorAdapter();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void start() {
    if (configurator == null) {
      throw new IllegalStateException("The card resource service is not configured.");
    }
    if (isStarted) {
      stop();
    }
    logger.info("Starting...");
    initializeReaderManagers();
    initializeCardProfileManagers();
    removeUnusedReaderManagers();
    startMonitoring();
    isStarted = true;
    logger.info("Started");
  }

  /**
   * (private)<br>
   * Initializes a reader manager for each reader of each configured "regular" plugin.
   */
  private void initializeReaderManagers() {
    for (Plugin plugin : configurator.getPlugins()) {
      for (Reader reader : plugin.getReaders()) {
        registerReader(reader, plugin);
      }
    }
  }

  /**
   * (private)<br>
   * Creates and registers a reader manager associated to the provided reader and its associated
   * plugin.<br>
   * If the provided reader is observable, then add it to the map of used observable readers.
   *
   * @param reader The reader to register.
   * @param plugin The associated plugin.
   * @return A not null reference.
   */
  private ReaderManagerAdapter registerReader(Reader reader, Plugin plugin) {

    // Get the reader configurator if a monitoring is requested for this reader.
    ReaderConfiguratorSpi readerConfiguratorSpi = null;
    for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {
      if (configuredPlugin.getPlugin() == plugin) {
        readerConfiguratorSpi = configuredPlugin.getReaderConfiguratorSpi();
        break;
      }
    }

    ReaderManagerAdapter readerManager =
        new ReaderManagerAdapter(
            reader, plugin, readerConfiguratorSpi, configurator.getUsageTimeoutMillis());
    readerToReaderManagerMap.put(reader, readerManager);

    if (reader instanceof ObservableReader) {
      Set<ObservableReader> usedObservableReaders = pluginToObservableReadersMap.get(plugin);
      if (usedObservableReaders == null) {
        usedObservableReaders =
            Collections.newSetFromMap(new ConcurrentHashMap<ObservableReader, Boolean>(1));
        pluginToObservableReadersMap.put(plugin, usedObservableReaders);
      }
      usedObservableReaders.add((ObservableReader) reader);
    }

    return readerManager;
  }

  /**
   * (private)<br>
   * Creates and registers a card profile manager for each configured card profile and creates all
   * available card resources.
   */
  private void initializeCardProfileManagers() {
    for (CardResourceProfileConfigurator profile :
        configurator.getCardResourceProfileConfigurators()) {
      cardProfileNameToCardProfileManagerMap.put(
          profile.getProfileName(), new CardProfileManagerAdapter(profile, configurator));
    }
  }

  /**
   * (private)<br>
   * Removes all reader managers whose reader is not accepted by any card profile manager and
   * unregisters their associated readers.
   */
  private void removeUnusedReaderManagers() {

    List<ReaderManagerAdapter> readerManagers =
        new ArrayList<ReaderManagerAdapter>(readerToReaderManagerMap.values());

    for (ReaderManagerAdapter readerManager : readerManagers) {
      if (!readerManager.isActive()) {
        unregisterReader(readerManager.getReader(), readerManager.getPlugin());
      }
    }
  }

  /**
   * (private)<br>
   * Removes the registered reader manager associated to the provided reader and stops the
   * observation of the reader if the reader is observable and the observation started.
   *
   * @param reader The reader to unregister.
   * @param plugin The associated plugin.
   */
  private void unregisterReader(Reader reader, Plugin plugin) {

    readerToReaderManagerMap.remove(reader);
    Set<ObservableReader> usedObservableReaders = pluginToObservableReadersMap.get(plugin);

    if (usedObservableReaders != null && reader instanceof ObservableReader) {
      ((ObservableReader) reader).removeObserver(this);
      usedObservableReaders.remove(reader);
    }
  }

  /**
   * (private)<br>
   * Starts the observation of observable plugins and/or observable readers if requested.<br>
   * The observation of the readers is performed only for those accepted by at least one card
   * profile manager.
   */
  private void startMonitoring() {
    for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {

      if (configuredPlugin.isWithPluginMonitoring()
          && configuredPlugin.getPlugin() instanceof ObservablePlugin) {

        logger.info("Start the monitoring of plugin '{}'", configuredPlugin.getPlugin().getName());
        startPluginObservation(configuredPlugin);
      }

      if (configuredPlugin.isWithReaderMonitoring()
          && pluginToObservableReadersMap.containsKey(configuredPlugin.getPlugin())) {

        for (ObservableReader reader :
            pluginToObservableReadersMap.get(configuredPlugin.getPlugin())) {

          logger.info("Start the monitoring of reader '{}'", reader.getName());
          startReaderObservation(reader, configuredPlugin);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void stop() {
    isStarted = false;
    stopMonitoring();
    readerToReaderManagerMap.clear();
    cardProfileNameToCardProfileManagerMap.clear();
    cardResourceToPoolPluginMap.clear();
    pluginToObservableReadersMap.clear();
    logger.info("Stopped");
  }

  /**
   * (private)<br>
   * Stops the observation of all observable plugins and observable readers configured.
   */
  private void stopMonitoring() {
    for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {

      if (configuredPlugin.isWithPluginMonitoring()
          && configuredPlugin.getPlugin() instanceof ObservablePlugin) {

        logger.info("Stop the monitoring of plugin '{}'", configuredPlugin.getPlugin().getName());
        ((ObservablePlugin) configuredPlugin.getPlugin()).removeObserver(this);
      }

      if (configuredPlugin.isWithReaderMonitoring()
          && pluginToObservableReadersMap.containsKey(configuredPlugin.getPlugin())) {

        for (ObservableReader reader :
            pluginToObservableReadersMap.get(configuredPlugin.getPlugin())) {

          logger.info("Stop the monitoring of reader '{}'", reader.getName());
          reader.removeObserver(this);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResource getCardResource(String cardResourceProfileName) {

    if (logger.isDebugEnabled()) {
      logger.debug("Searching a card resource for profile '{}'...", cardResourceProfileName);
    }
    if (!isStarted) {
      throw new IllegalStateException("The card resource service is not started.");
    }
    Assert.getInstance().notEmpty(cardResourceProfileName, "cardResourceProfileName");

    CardProfileManagerAdapter cardProfileManager =
        cardProfileNameToCardProfileManagerMap.get(cardResourceProfileName);

    Assert.getInstance().notNull(cardProfileManager, "cardResourceProfileName");

    CardResource cardResource = cardProfileManager.getCardResource();

    if (logger.isDebugEnabled()) {
      logger.debug("Found : {}", getCardResourceInfo(cardResource));
    }

    return cardResource;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void releaseCardResource(CardResource cardResource) {

    if (logger.isDebugEnabled()) {
      logger.debug("Releasing {}...", getCardResourceInfo(cardResource));
    }
    if (!isStarted) {
      throw new IllegalStateException("The card resource service is not started.");
    }
    Assert.getInstance().notNull(cardResource, "cardResource");

    // For regular or pool plugin ?
    ReaderManagerAdapter readerManager =
        readerToReaderManagerMap.get(
            cardResource.getReader()); // NOSONAR card resource cannot be null here

    if (readerManager != null) {
      readerManager.unlock();

    } else {
      PoolPlugin poolPlugin = cardResourceToPoolPluginMap.get(cardResource);
      if (poolPlugin != null) {
        cardResourceToPoolPluginMap.remove(cardResource);
        poolPlugin.releaseReader(cardResource.getReader());
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Card resource released");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeCardResource(CardResource cardResource) {

    if (logger.isDebugEnabled()) {
      logger.debug("Removing {}...", getCardResourceInfo(cardResource));
    }

    releaseCardResource(cardResource); // NOSONAR false positive

    // For regular plugin ?
    ReaderManagerAdapter readerManager = readerToReaderManagerMap.get(cardResource.getReader());
    if (readerManager != null) {
      readerManager.removeCardResource(cardResource);
      for (CardProfileManagerAdapter cardProfileManager :
          cardProfileNameToCardProfileManagerMap.values()) {
        cardProfileManager.removeCardResource(cardResource);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Card resource removed");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onPluginEvent(PluginEvent pluginEvent) {
    if (!isStarted) {
      return;
    }
    Plugin plugin = SmartCardServiceProvider.getService().getPlugin(pluginEvent.getPluginName());
    if (pluginEvent.getEventType() == PluginEvent.EventType.READER_CONNECTED) {
      for (String readerName : pluginEvent.getReaderNames()) {
        // Get the new reader from the plugin because it is not yet registered in the service.
        Reader reader = plugin.getReader(readerName);
        if (reader != null) {
          synchronized (reader) {
            onReaderConnected(reader, plugin);
          }
        }
      }
    } else {
      for (String readerName : pluginEvent.getReaderNames()) {
        // Get the reader back from the service because it is no longer registered in the plugin.
        Reader reader = getReader(readerName);
        if (reader != null) {
          // The reader is registered in the service.
          synchronized (reader) {
            onReaderDisconnected(reader, plugin);
          }
        }
      }
    }
  }

  /**
   * (private)<br>
   * Gets the reader having the provided name if it is registered.
   *
   * @param readerName The name of the reader.
   * @return Null if the reader is not or no longer registered.
   */
  private Reader getReader(String readerName) {
    for (Reader reader : readerToReaderManagerMap.keySet()) {
      if (reader.getName().equals(readerName)) {
        return reader;
      }
    }
    return null;
  }

  /**
   * (private)<br>
   * Invoked when a new reader is connected.<br>
   * Notifies all card profile managers about the new available reader.<br>
   * If the new reader is accepted by at least one card profile manager, then a new reader manager
   * is registered to the service.
   *
   * @param reader The new reader.
   * @param plugin The associated plugin.
   */
  private void onReaderConnected(Reader reader, Plugin plugin) {
    ReaderManagerAdapter readerManager = registerReader(reader, plugin);
    for (CardProfileManagerAdapter cardProfileManager :
        cardProfileNameToCardProfileManagerMap.values()) {
      cardProfileManager.onReaderConnected(readerManager);
    }
    if (readerManager.isActive()) {
      startMonitoring(reader, plugin);
    } else {
      unregisterReader(reader, plugin);
    }
  }

  /**
   * (private)<br>
   * Starts the observation of the provided reader only if it is observable, if the monitoring is
   * requested for the provided plugin and if the reader is accepted by at least one card profile
   * manager.
   *
   * @param reader The reader to observe.
   * @param plugin The associated plugin.
   */
  private void startMonitoring(Reader reader, Plugin plugin) {

    if (reader instanceof ObservableReader) {

      for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {

        if (configuredPlugin.getPlugin() == plugin && configuredPlugin.isWithReaderMonitoring()) {

          logger.info("Start the monitoring of reader '{}'", reader.getName());
          startReaderObservation((ObservableReader) reader, configuredPlugin);
        }
      }
    }
  }

  /**
   * (private)<br>
   * Starts the observation of the "regular" plugin.
   *
   * @param configuredPlugin The associated configuration.
   */
  private void startPluginObservation(ConfiguredPlugin configuredPlugin) {
    ObservablePlugin observablePlugin = (ObservablePlugin) configuredPlugin.getPlugin();
    observablePlugin.setPluginObservationExceptionHandler(
        configuredPlugin.getPluginObservationExceptionHandlerSpi());
    observablePlugin.addObserver(this);
  }

  /**
   * (private)<br>
   * Starts the observation of the reader associated to a "regular" plugin.
   *
   * @param observableReader The observable reader to observe.
   * @param configuredPlugin The associated configuration.
   */
  private void startReaderObservation(
      ObservableReader observableReader, ConfiguredPlugin configuredPlugin) {
    observableReader.setReaderObservationExceptionHandler(
        configuredPlugin.getReaderObservationExceptionHandlerSpi());
    observableReader.addObserver(this);
    observableReader.startCardDetection(ObservableReader.PollingMode.REPEATING);
  }

  /**
   * (private)<br>
   * Invoked when an accepted reader is no more available because it was disconnected or
   * unregistered.<br>
   * Removes its reader manager and all associated created card resources from all card profile
   * managers.
   *
   * @param reader The disconnected reader.
   * @param plugin The associated plugin.
   */
  private void onReaderDisconnected(Reader reader, Plugin plugin) {
    ReaderManagerAdapter readerManager = readerToReaderManagerMap.get(reader);
    if (readerManager != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Remove disconnected reader '{}' and all associated card resources", reader.getName());
      }
      onCardRemoved(readerManager);
      unregisterReader(reader, plugin);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onReaderEvent(ReaderEvent readerEvent) {
    if (!isStarted) {
      return;
    }
    Reader reader = getReader(readerEvent.getReaderName());
    if (reader != null) {
      // The reader is registered in the service.
      synchronized (reader) {
        ReaderManagerAdapter readerManager = readerToReaderManagerMap.get(reader);
        if (readerManager != null) {
          onReaderEvent(readerEvent, readerManager);
        }
      }
    }
  }

  /**
   * (private)<br>
   * Invoked when a card is inserted, removed or the associated reader unregistered.<br>
   *
   * @param readerEvent The reader event.
   * @param readerManager The reader manager associated to the reader.
   */
  private void onReaderEvent(ReaderEvent readerEvent, ReaderManagerAdapter readerManager) {
    if (readerEvent.getEventType() == ReaderEvent.EventType.CARD_INSERTED
        || readerEvent.getEventType() == ReaderEvent.EventType.CARD_MATCHED) {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Create new card resources associated with reader '{}' matching the new card inserted",
            readerManager.getReader().getName());
      }
      onCardInserted(readerManager);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Remove all card resources associated with reader '{}' caused by a card removal or reader unregistration",
            readerManager.getReader().getName());
      }
      onCardRemoved(readerManager);
    }
  }

  /**
   * (private)<br>
   * Invoked when a card is inserted on a reader.<br>
   * Notifies all card profile managers about the insertion of the card.<br>
   * Each card profile manager interested by the card reader will try to create a card resource.
   *
   * @param readerManager The associated reader manager.
   */
  private void onCardInserted(ReaderManagerAdapter readerManager) {
    for (CardProfileManagerAdapter cardProfileManager :
        cardProfileNameToCardProfileManagerMap.values()) {
      cardProfileManager.onCardInserted(readerManager);
    }
  }

  /**
   * (private)<br>
   * Invoked when a card is removed or the associated reader unregistered.<br>
   * Removes all created card resources associated to the reader.
   *
   * @param readerManager The associated reader manager.
   */
  private void onCardRemoved(ReaderManagerAdapter readerManager) {

    Set<CardResource> cardResourcesToRemove =
        new HashSet<CardResource>(readerManager.getCardResources());

    for (CardResource cardResource : cardResourcesToRemove) {
      removeCardResource(cardResource);
    }
  }
}
