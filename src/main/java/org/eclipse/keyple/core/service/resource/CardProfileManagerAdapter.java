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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.keyple.core.card.ProxyReader;
import org.eclipse.keyple.core.card.spi.SmartCardSpi;
import org.eclipse.keyple.core.service.KeyplePluginException;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Manager of a card profile.
 *
 * <p>It contains the profile configuration and associated card resources.
 *
 * @since 2.0
 */
final class CardProfileManagerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(CardProfileManagerAdapter.class);

  /** The associated card profile. */
  private final CardResourceProfileConfigurator cardProfile;

  /** The global configuration of the card resource service. */
  private final CardResourceServiceConfiguratorAdapter globalConfiguration;

  /** The unique instance of the card resource service. */
  private final CardResourceServiceAdapter service;

  /** The ordered list of "regular" plugins to use. */
  private final List<Plugin> plugins;

  /** The ordered list of "pool" plugins to use. */
  private final List<PoolPlugin> poolPlugins;

  /** The current available card resources associated with "regular" plugins. */
  private final List<CardResource> cardResources;

  /** The filter on the reader name if set. */
  private final Pattern readerNameRegexPattern;

  /**
   * (package-private)<br>
   * Creates a new card profile manager using the provided card profile and initializes all
   * available card resources.
   *
   * @param cardProfile The associated card profile.
   * @param globalConfiguration The global configuration of the service.
   * @since 2.0
   */
  CardProfileManagerAdapter(
      CardResourceProfileConfigurator cardProfile,
      CardResourceServiceConfiguratorAdapter globalConfiguration) {

    this.cardProfile = cardProfile;
    this.globalConfiguration = globalConfiguration;
    this.service = CardResourceServiceAdapter.getInstance();
    this.plugins = new ArrayList<Plugin>(0);
    this.poolPlugins = new ArrayList<PoolPlugin>(0);
    this.cardResources = new ArrayList<CardResource>();

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

  /**
   * (private)<br>
   * Initializes card resources using the plugins configured on the card profile.
   */
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

  /**
   * (private)<br>
   * Initializes card resources using the plugins configured on the card resource service.
   */
  private void initializeCardResourcesUsingDefaultPlugins() {
    poolPlugins.addAll(globalConfiguration.getPoolPlugins());
    for (Plugin plugin : globalConfiguration.getPlugins()) {
      plugins.add(plugin);
      initializeCardResources(plugin);
    }
  }

  /**
   * (private)<br>
   * Initializes all available card resources by analysing all readers of the provided "regular"
   * plugin.
   *
   * @param plugin The "regular" plugin to analyse.
   */
  private void initializeCardResources(Plugin plugin) {
    for (Reader reader : plugin.getReaders().values()) {
      ReaderManagerAdapter readerManager = service.getReaderManager(reader);
      initializeCardResource(readerManager);
    }
  }

  /**
   * (private)<br>
   * Tries to initialize a card resource for the provided reader manager only if the reader is
   * accepted by the profile.
   *
   * <p>If the reader is accepted, then activates the provided reader manager if it is not already
   * activated.
   *
   * @param readerManager The reader manager to use.
   */
  private void initializeCardResource(ReaderManagerAdapter readerManager) {

    if (isReaderAccepted(readerManager.getReader())) {

      readerManager.activate();

      CardResource cardResource =
          readerManager.matches(cardProfile.getCardResourceProfileExtensionSpi());

      // The returned card resource may already be present in the current list if the service starts
      // with an observable reader in which a card has been inserted.
      if (cardResource != null) {
        if (!cardResources.contains(cardResource)) {
          cardResources.add(cardResource);
          if (logger.isDebugEnabled()) {
            logger.debug(
                "Add {} to card resource profile '{}'",
                CardResourceServiceAdapter.getCardResourceInfo(cardResource),
                cardProfile.getProfileName());
          }
        } else if (logger.isDebugEnabled()) {
          logger.debug(
              "{} already present in card resource profile '{}'",
              CardResourceServiceAdapter.getCardResourceInfo(cardResource),
              cardProfile.getProfileName());
        }
      }
    }
  }

  /**
   * (private)<br>
   * Checks if the provided reader is accepted using the filter on the name.
   *
   * @param reader The reader to check.
   * @return True if it is accepted.
   */
  private boolean isReaderAccepted(Reader reader) {
    return readerNameRegexPattern == null
        || readerNameRegexPattern.matcher(reader.getName()).matches();
  }

  /**
   * (package-private)<br>
   * Removes the provided card resource from the profile manager if it is present.
   *
   * @param cardResource The card resource to remove.
   * @since 2.0
   */
  void removeCardResource(CardResource cardResource) {
    boolean isRemoved = cardResources.remove(cardResource);
    if (logger.isDebugEnabled() && isRemoved) {
      logger.debug(
          "Remove {} from card resource profile '{}'",
          CardResourceServiceAdapter.getCardResourceInfo(cardResource),
          cardProfile.getProfileName());
    }
  }

  /**
   * (package-private)<br>
   * Invoked when a new reader is connected.<br>
   * If the associated plugin is referenced on the card profile, then tries to initialize a card
   * resource if the reader is accepted.
   *
   * @param readerManager The reader manager to use.
   * @since 2.0
   */
  void onReaderConnected(ReaderManagerAdapter readerManager) {
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

  /**
   * (package-private)<br>
   * Invoked when a new card is inserted.<br>
   * The behaviour is the same as if a reader was connected.
   *
   * @param readerManager The reader manager to use.
   * @since 2.0
   */
  void onCardInserted(ReaderManagerAdapter readerManager) {
    onReaderConnected(readerManager);
  }

  /**
   * (package-private)<br>
   * Tries to get a card resource and locks the associated reader.<br>
   * Applies the configured allocation strategy by looping, pausing, ordering resources.
   *
   * @return Null if there is no card resource available.
   * @since 2.0
   */
  CardResource getCardResource() {
    CardResource cardResource;
    long maxTime = System.currentTimeMillis() + globalConfiguration.getTimeoutMillis();
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
        && globalConfiguration.isBlockingAllocationMode()
        && System.currentTimeMillis() <= maxTime);
    return cardResource;
  }

  /**
   * (private)<br>
   * Make a pause if the provided card resource is null and a blocking allocation mode is requested.
   *
   * @param cardResource The founded card resource or null if not found.
   */
  private void pauseIfNeeded(CardResource cardResource) {
    if (cardResource == null && globalConfiguration.isBlockingAllocationMode()) {
      try {
        Thread.sleep(globalConfiguration.getCycleDurationMillis());
      } catch (InterruptedException e) {
        logger.error("Unexpected sleep interruption", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * (private)<br>
   * Tries to get a card resource searching in "regular" and "pool" plugins.
   *
   * @return Null if there is no card resource available.
   */
  private CardResource getRegularOrPoolCardResource() {
    CardResource cardResource;
    if (globalConfiguration.isUsePoolFirst()) {
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

  /**
   * (private)<br>
   * Tries to get a card resource searching in all "regular" plugins.
   *
   * <p>If a card resource is no more usable, then removes it from the service.
   *
   * @return Null if there is no card resource available.
   */
  private CardResource getRegularCardResource() {

    CardResource result = null;
    List<CardResource> unusableCardResources = new ArrayList<CardResource>(0);

    for (CardResource cardResource : cardResources) {
      Reader reader = cardResource.getReader();
      synchronized (reader) {
        ReaderManagerAdapter readerManager = service.getReaderManager(reader);
        if (readerManager != null) {
          try {
            if (readerManager.lock(
                cardResource, cardProfile.getCardResourceProfileExtensionSpi())) {
              int cardResourceIndex = cardResources.indexOf(cardResource);
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
      service.removeCardResource(cardResource);
    }

    return result;
  }

  /**
   * (private)<br>
   * Updates the order of the created card resources according to the configured strategy.
   *
   * @param cardResourceIndex The current card resource index of the available card resource
   *     founded.
   */
  private void updateCardResourcesOrder(int cardResourceIndex) {
    if (globalConfiguration.getAllocationStrategy() == AllocationStrategy.CYCLIC) {
      Collections.rotate(cardResources, -cardResourceIndex - 1);
    } else if (globalConfiguration.getAllocationStrategy() == AllocationStrategy.RANDOM) {
      Collections.shuffle(cardResources);
    }
  }

  /**
   * (private)<br>
   * Tries to get a card resource searching in all "pool" plugins.
   *
   * @return Null if there is no card resource available.
   */
  private CardResource getPoolCardResource() {
    for (PoolPlugin poolPlugin : poolPlugins) {
      try {
        Reader reader = poolPlugin.allocateReader(cardProfile.getReaderGroupReference());
        if (reader != null) {
          SmartCardSpi smartCardSpi =
              cardProfile.getCardResourceProfileExtensionSpi().matches((ProxyReader) reader);
          if (smartCardSpi != null) {
            CardResource cardResource = new CardResource(reader, (SmartCard) smartCardSpi);
            service.registerPoolCardResource(cardResource, poolPlugin);
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
