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

import static org.eclipse.keyple.core.service.CardResourceServiceConfiguratorAdapter.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.eclipse.keyple.core.card.ProxyReader;
import org.eclipse.keyple.core.card.spi.CardResourceProfileExtensionSpi;
import org.eclipse.keyple.core.card.spi.SmartCardSpi;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;
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

  /** Singleton */
  private static final CardResourceServiceAdapter instance = new CardResourceServiceAdapter();

  private final Map<Reader, ReaderManager> readerManagers =
      new ConcurrentHashMap<Reader, ReaderManager>();

  private final Map<String, CardProfileManager> cardProfileManagers =
      new ConcurrentHashMap<String, CardProfileManager>();

  private final Map<CardResource, Plugin> cardResources =
      new ConcurrentHashMap<CardResource, Plugin>();

  private final Map<Plugin, Set<ObservableReader>> usedObservableReaders =
      new ConcurrentHashMap<Plugin, Set<ObservableReader>>();

  private CardResourceServiceConfiguratorAdapter configurator;
  private boolean isStarted;

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
   * Configures the card resource service.
   *
   * <p>If service is started, then stops the service, applies the configuration and starts the
   * service.<br>
   * If not, then only applies the configuration.
   *
   * @since 2.0
   */
  void configure(CardResourceServiceConfiguratorAdapter configurator) {
    logger.info("The card resource service is applying a new configuration.");
    if (isStarted) {
      stop();
      this.configurator = configurator;
      start();
    } else {
      this.configurator = configurator;
    }
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
    for (CardProfile cardProfile : configurator.getCardProfiles()) {
      cardProfileManagers.put(cardProfile.getName(), new CardProfileManager(cardProfile));
    }
    startMonitoring();
    isStarted = true;
  }

  private void startMonitoring() {
    for (ConfiguredRegularPlugin configuredRegularPlugin :
        configurator.getConfiguredRegularPlugins()) {
      if (configuredRegularPlugin.isWithReaderMonitoring()
          && configuredRegularPlugin.getPlugin() instanceof ObservablePlugin) {
        ((ObservablePlugin) configuredRegularPlugin.getPlugin()).addObserver(this);
      }
      if (configuredRegularPlugin.isWithCardMonitoring()
          && usedObservableReaders.containsKey(configuredRegularPlugin.getPlugin())) {
        for (ObservableReader reader :
            usedObservableReaders.get(configuredRegularPlugin.getPlugin())) {
          reader.addObserver(this);
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
    readerManagers.clear();
    cardProfileManagers.clear();
    cardResources.clear();
    usedObservableReaders.clear();
  }

  private void stopMonitoring() {
    for (ConfiguredRegularPlugin configuredRegularPlugin :
        configurator.getConfiguredRegularPlugins()) {
      if (configuredRegularPlugin.isWithReaderMonitoring()
          && configuredRegularPlugin.getPlugin() instanceof ObservablePlugin) {
        ((ObservablePlugin) configuredRegularPlugin.getPlugin()).removeObserver(this);
      }
      if (configuredRegularPlugin.isWithCardMonitoring()
          && usedObservableReaders.containsKey(configuredRegularPlugin.getPlugin())) {
        for (ObservableReader reader :
            usedObservableReaders.get(configuredRegularPlugin.getPlugin())) {
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

    if (!isStarted) {
      throw new IllegalStateException("The card resource service is not started.");
    }
    Assert.getInstance().notEmpty(cardResourceProfileName, "cardResourceProfileName");

    CardProfileManager cardProfileManager = cardProfileManagers.get(cardResourceProfileName);
    Assert.getInstance().notNull(cardProfileManager, "cardResourceProfileName");

    return cardProfileManager.getCardResource();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void releaseCardResource(CardResource cardResource) {
    Assert.getInstance().notNull(cardResource, "cardResource");
    final Reader reader = cardResource.getReader();
    synchronized (reader) {
      Plugin plugin = cardResources.get(cardResource);
      if (plugin == null) {
        // Unknown card resource.
        return;
      }
      if (plugin instanceof PoolPlugin) {
        ((PoolPlugin) plugin).releaseReader(reader);
        cardResources.remove(cardResource);
      } else {
        readerManagers.get(reader).unlock();
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeCardResource(CardResource cardResource) {
    releaseCardResource(cardResource);
    final Reader reader = cardResource.getReader();
    synchronized (reader) {
      Plugin plugin = cardResources.get(cardResource);
      if (plugin == null) {
        // Pool plugin or unknown card resource.
        return;
      }
      // Regular plugin.
      cardResources.remove(cardResource);
      for (CardProfileManager cardProfileManager : cardProfileManagers.values()) {
        cardProfileManager.removeCardResource(cardResource);
      }
      boolean removeReader = true;
      for (CardResource cr : cardResources.keySet()) {
        if (cr.getReader() == reader) {
          removeReader = false;
          break;
        }
      }
      if (removeReader) {
        unregisterReader(reader, plugin);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onPluginEvent(PluginEvent pluginEvent) {
    Plugin plugin = SmartCardServiceProvider.getService().getPlugin(pluginEvent.getPluginName());
    if (pluginEvent.getEventType() == PluginEvent.EventType.READER_CONNECTED) {
      for (String readerName : pluginEvent.getReadersNames()) {
        Reader reader = plugin.getReader(readerName);
        if (reader != null) {
          synchronized (reader) {
            onReaderConnected(reader, plugin);
          }
        }
      }
    } else {
      for (String readerName : pluginEvent.getReadersNames()) {
        Reader reader = getReader(readerName);
        if (reader != null) {
          synchronized (reader) {
            onReaderDisconnected(reader, plugin);
          }
        }
      }
    }
  }

  private void onReaderConnected(Reader reader, Plugin plugin) {
    ReaderManager readerManager = getOrCreateReaderManager(reader, plugin);
    for (CardProfileManager cardProfileManager : cardProfileManagers.values()) {
      cardProfileManager.onReaderConnected(readerManager);
    }
    if (!readerManager.isActive()) {
      unregisterReader(reader, plugin);
    }
  }

  private void onReaderDisconnected(Reader reader, Plugin plugin) {
    ReaderManager readerManager = readerManagers.get(reader);
    if (readerManager != null) {
      for (CardProfileManager cardProfileManager : cardProfileManagers.values()) {
        cardProfileManager.onReaderDisconnected(readerManager);
      }
      unregisterReader(readerManager.getReader(), plugin);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void onReaderEvent(ReaderEvent readerEvent) {
    final Reader reader = getReader(readerEvent.getReaderName());
    if (reader != null) {
      synchronized (reader) {
        ReaderManager readerManager = readerManagers.get(reader);
        if (readerManager != null) {
          if (readerEvent.getEventType() == ReaderEvent.EventType.CARD_INSERTED
              || readerEvent.getEventType() == ReaderEvent.EventType.CARD_MATCHED) {
            onCardInserted(readerManager);
          } else {
            onCardRemoved(readerManager);
          }
        }
      }
    }
  }

  private void onCardInserted(ReaderManager readerManager) {
    for (CardProfileManager cardProfileManager : cardProfileManagers.values()) {
      cardProfileManager.onCardInserted(readerManager);
    }
  }

  private void onCardRemoved(ReaderManager readerManager) {
    for (CardProfileManager cardProfileManager : cardProfileManagers.values()) {
      cardProfileManager.onCardRemoved(readerManager);
    }
    readerManager.unlock();
  }

  private class ReaderManager {

    private final Reader reader;
    private final Plugin plugin;
    private boolean isBusy;
    private CardResource selectedCardResource;
    private boolean isActive;

    private ReaderManager(Reader reader, Plugin plugin) {
      this.reader = reader;
      this.plugin = plugin;
      this.isBusy = false;
      this.selectedCardResource = null;
      this.isActive = false;
    }

    public Reader getReader() {
      return reader;
    }

    public Plugin getPlugin() {
      return plugin;
    }

    public void setActive(boolean active) {
      this.isActive = active;
    }

    public boolean isActive() {
      return isActive;
    }

    private CardResource matches(CardResourceProfileExtensionSpi extension) {
      CardResource cardResource = null;
      SmartCardSpi smartCard = extension.matches((ProxyReader) reader);
      if (smartCard != null) {
        cardResource = getOrCreateCardResource((SmartCard) smartCard);
        selectedCardResource = cardResource;
      }
      isBusy = false;
      return cardResource;
    }

    private boolean lock(CardResource cardResource, CardResourceProfileExtensionSpi extension) {
      if (isBusy) {
        return false;
      }
      if (selectedCardResource != cardResource) {
        SmartCardSpi smartCard = extension.matches((ProxyReader) reader);
        if (smartCard == null
            || !Arrays.equals(
                cardResource.getSmartCard().getAtrBytes(), ((SmartCard) smartCard).getAtrBytes())
            || !Arrays.equals(
                cardResource.getSmartCard().getFciBytes(), ((SmartCard) smartCard).getFciBytes())) {
          throw new IllegalStateException(
              "No card is inserted or its profile does not match the associated data.");
        }
        selectedCardResource = cardResource;
      }
      isBusy = true;
      return true;
    }

    private void unlock() {
      isBusy = false;
    }

    private CardResource getOrCreateCardResource(SmartCard smartCard) {
      // Check if an identical card resource is already created.
      for (CardResource cardResource : cardResources.keySet()) {
        if (cardResource.getReader() == reader
            && Arrays.equals(cardResource.getSmartCard().getAtrBytes(), smartCard.getAtrBytes())
            && Arrays.equals(cardResource.getSmartCard().getFciBytes(), smartCard.getFciBytes())) {
          return cardResource;
        }
      }
      // If none, then create a new one.
      CardResource cardResource = new CardResource(reader, smartCard);
      cardResources.put(cardResource, plugin);
      return cardResource;
    }
  }

  /**
   * (private)<br>
   * Inner class of a card resource profile manager.
   */
  private class CardProfileManager {

    private final CardProfile cardProfile;
    private final List<Plugin> plugins;
    private final List<PoolPlugin> poolPlugins;
    private final List<CardResource> profileCardResources;
    private final Pattern readerNameRegexPattern;

    /**
     * (private)<br>
     * Constructor.
     */
    private CardProfileManager(CardProfile cardProfile) {

      this.cardProfile = cardProfile;
      this.plugins = new ArrayList<Plugin>(0);
      this.poolPlugins = new ArrayList<PoolPlugin>(0);
      this.profileCardResources = new ArrayList<CardResource>();

      // Prepare filter on reader name if requested.
      if (cardProfile.getReaderNameRegex() != null) {
        this.readerNameRegexPattern = Pattern.compile(cardProfile.getReaderNameRegex());
      } else {
        this.readerNameRegexPattern = null;
      }

      // Initialize all available card resources.
      if (!cardProfile.getPlugins().isEmpty()) {
        initializeCardResourcesUsingProfilePlugins();
      } else {
        initializeCardResourcesUsingDefaultPlugins();
      }
    }

    private void initializeCardResourcesUsingProfilePlugins() {
      for (Plugin plugin : cardProfile.getPlugins()) {
        if (plugin instanceof PoolPlugin) {
          poolPlugins.add((PoolPlugin) plugin);
        } else {
          plugins.add(plugin);
          initializeCardResources(plugin);
        }
      }
    }

    private void initializeCardResourcesUsingDefaultPlugins() {
      for (ConfiguredPoolPlugin configuredPoolPlugin : configurator.getConfiguredPoolPlugins()) {
        poolPlugins.add(configuredPoolPlugin.getPoolPlugin());
      }
      for (ConfiguredRegularPlugin configuredRegularPlugin :
          configurator.getConfiguredRegularPlugins()) {
        plugins.add(configuredRegularPlugin.getPlugin());
        initializeCardResources(configuredRegularPlugin.getPlugin());
      }
    }

    private void initializeCardResources(Plugin plugin) {
      for (final Reader reader : plugin.getReaders().values()) {
        synchronized (reader) {
          ReaderManager readerManager = getOrCreateReaderManager(reader, plugin);
          initializeCardResource(readerManager);
        }
      }
    }

    private void initializeCardResource(ReaderManager readerManager) {
      if (isReaderAccepted(readerManager.getReader())) {
        readerManager.setActive(true);
        CardResource cardResource =
            readerManager.matches(cardProfile.getCardResourceProfileExtension());
        if (cardResource != null) {
          profileCardResources.add(cardResource); // TODO improve insertion index
        }
      }
    }

    private boolean isReaderAccepted(Reader reader) {
      return readerNameRegexPattern == null
          || readerNameRegexPattern.matcher(reader.getName()).matches();
    }

    private void removeCardResource(CardResource cardResource) {
      profileCardResources.remove(cardResource);
    }

    private void onReaderConnected(ReaderManager readerManager) {
      if (!cardProfile.getPlugins().isEmpty()) {
        for (Plugin profilePlugin : cardProfile.getPlugins()) {
          if (profilePlugin == readerManager.getPlugin()) {
            initializeCardResource(readerManager);
            break;
          }
        }
      } else {
        initializeCardResource(readerManager);
      }
    }

    private void onReaderDisconnected(ReaderManager readerManager) {
      CardResource cardResourceToRemove = null;
      for (CardResource cardResource : profileCardResources) {
        if (cardResource.getReader() == readerManager.getReader()) {
          cardResourceToRemove = cardResource;
          break;
        }
      }
      if (cardResourceToRemove != null) {
        profileCardResources.remove(cardResourceToRemove);
      }
    }

    private void onCardInserted(ReaderManager readerManager) {
      onReaderConnected(readerManager);
    }

    private void onCardRemoved(ReaderManager readerManager) {
      onReaderDisconnected(readerManager);
    }

    private CardResource getCardResource() {
      CardResource cardResource;
      long maxTime = System.currentTimeMillis() + configurator.getTimeoutMillis();
      do {
        if (!plugins.isEmpty()) {
          if (!poolPlugins.isEmpty()) {
            cardResource = getRegularOrPoolCardResource();
          } else {
            cardResource = getRegularCardResource();
          }
        } else {
          cardResource = getPoolCardResource();
        }
        pauseIfNeeded(cardResource);
      } while (cardResource == null
          && configurator.isBlockingAllocationMode()
          && System.currentTimeMillis() <= maxTime);
      return cardResource;
    }

    private CardResource getRegularOrPoolCardResource() {
      CardResource cardResource;
      if (configurator.getPoolAllocationStrategy() == PoolAllocationStrategy.POOL_FIRST) {
        cardResource = getPoolCardResource();
        if (cardResource == null) {
          cardResource = getRegularCardResource();
        }
      } else {
        cardResource = getRegularCardResource();
        if (cardResource == null) {
          cardResource = getPoolCardResource();
        }
      }
      return cardResource;
    }

    private void pauseIfNeeded(CardResource cardResource) {
      if (cardResource == null && configurator.isBlockingAllocationMode()) {
        try {
          Thread.sleep(configurator.getCycleDurationMillis());
        } catch (InterruptedException e) {
          logger.error("Unexpected sleep interruption", e);
          Thread.currentThread().interrupt();
        }
      }
    }

    private CardResource getRegularCardResource() {
      CardResource result = null;
      List<CardResource> unusableCardResources = new ArrayList<CardResource>(0);
      for (CardResource cardResource : profileCardResources) {
        Reader reader = cardResource.getReader();
        synchronized (reader) {
          ReaderManager readerManager = readerManagers.get(reader);
          if (readerManager != null) {
            try {
              if (readerManager.lock(cardResource, cardProfile.getCardResourceProfileExtension())) {
                int cardResourceIndex = profileCardResources.indexOf(cardResource);
                updateCardResourcesOrder(cardResourceIndex);
                result = cardResource;
                break;
              }
            } catch (IllegalStateException e) {
              unusableCardResources.add(cardResource);
            }
          } else {
            unusableCardResources.add(cardResource);
          }
        }
      }
      // Remove unusable card resources identified.
      for (CardResource cardResource : unusableCardResources) {
        removeCardResource(cardResource);
      }
      return result;
    }

    private void updateCardResourcesOrder(int cardResourceIndex) {
      if (configurator.getAllocationStrategy() == AllocationStrategy.CYCLIC) {
        Collections.rotate(profileCardResources, -cardResourceIndex - 1);
      } else if (configurator.getAllocationStrategy() == AllocationStrategy.RANDOM) {
        Collections.shuffle(profileCardResources);
      }
    }

    private CardResource getPoolCardResource() {
      for (PoolPlugin poolPlugin : poolPlugins) {
        try {
          Reader reader = poolPlugin.allocateReader(cardProfile.getReaderGroupReference());
          if (reader != null) {
            SmartCardSpi smartCardSpi =
                cardProfile.getCardResourceProfileExtension().matches((ProxyReader) reader);
            if (smartCardSpi != null) {
              CardResource cardResource = new CardResource(reader, (SmartCard) smartCardSpi);
              cardResources.put(cardResource, poolPlugin);
              return cardResource;
            }
          }
        } catch (KeyplePluginException e) {
          // Continue.
        }
      }
      return null;
    }
  }

  private Reader getReader(String readerName) {
    for (Reader reader : readerManagers.keySet()) {
      if (reader.getName().equals(readerName)) {
        return reader;
      }
    }
    return null;
  }

  private ReaderManager getOrCreateReaderManager(Reader reader, Plugin plugin) {
    ReaderManager readerManager = readerManagers.get(reader);
    if (readerManager == null) {
      readerManager = registerReader(reader, plugin);
    }
    return readerManager;
  }

  private ReaderManager registerReader(Reader reader, Plugin plugin) {
    ReaderManager readerManager = new ReaderManager(reader, plugin);
    readerManagers.put(reader, readerManager);
    if (reader instanceof ObservableReader) {
      Set<ObservableReader> usedReaders = usedObservableReaders.get(plugin);
      if (usedReaders == null) {
        usedReaders =
            Collections.newSetFromMap(new ConcurrentHashMap<ObservableReader, Boolean>(1));
        usedObservableReaders.put(plugin, usedReaders);
      }
      usedReaders.add((ObservableReader) reader);
    }
    return readerManager;
  }

  private void unregisterReader(Reader reader, Plugin plugin) {
    readerManagers.remove(reader);
    Set<ObservableReader> usedReaders = usedObservableReaders.get(plugin);
    if (usedReaders != null && reader instanceof ObservableReader) {
      ((ObservableReader) reader).removeObserver(this);
      usedReaders.remove(reader);
    }
  }
}
