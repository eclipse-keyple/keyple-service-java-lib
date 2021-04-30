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
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.util.Assert;

/**
 * (package-private)<br>
 * Implementation of {@link CardResourceServiceConfigurator}.
 *
 * @since 2.0
 */
final class CardResourceServiceConfiguratorAdapter implements CardResourceServiceConfigurator {

  /* Regular plugins */
  private List<Plugin> plugins;
  private List<ConfiguredPlugin> configuredPlugins;
  private AllocationStrategy allocationStrategy;
  private int usageTimeoutMillis;

  /* Pool plugins */
  private List<PoolPlugin> poolPlugins;
  private boolean usePoolFirst;

  /* Card resource profiles configurators */
  private final Set<CardResourceProfileConfigurator> cardResourceProfileConfigurators;

  /* Global */
  private boolean isBlockingAllocationMode;
  private int cycleDurationMillis;
  private int timeoutMillis;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @since 2.0
   */
  CardResourceServiceConfiguratorAdapter() {
    cardResourceProfileConfigurators = new HashSet<CardResourceProfileConfigurator>(1);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator withPlugins(PluginsConfigurator pluginsConfigurator) {
    Assert.getInstance().notNull(pluginsConfigurator, "pluginsConfigurator");
    if (plugins != null) {
      throw new IllegalStateException("Plugins already configured.");
    }
    plugins = pluginsConfigurator.getPlugins();
    configuredPlugins = pluginsConfigurator.getConfiguredPlugins();
    allocationStrategy = pluginsConfigurator.getAllocationStrategy();
    usageTimeoutMillis = pluginsConfigurator.getUsageTimeoutMillis();
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator withPoolPlugins(
      PoolPluginsConfigurator poolPluginsConfigurator) {
    Assert.getInstance().notNull(poolPluginsConfigurator, "poolPluginsConfigurator");
    if (poolPlugins != null) {
      throw new IllegalStateException("Pool plugins already configured.");
    }
    poolPlugins = poolPluginsConfigurator.getPoolPlugins();
    usePoolFirst = poolPluginsConfigurator.isUsePoolFirst();
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator withCardResourceProfiles(
      CardResourceProfileConfigurator... cardResourceProfileConfigurators) {
    Assert.getInstance()
        .notNull(cardResourceProfileConfigurators, "cardResourceProfileConfigurators");
    if (!this.cardResourceProfileConfigurators.isEmpty()) {
      throw new IllegalStateException("Card resource profiles already configured.");
    }
    for (CardResourceProfileConfigurator configurator : cardResourceProfileConfigurators) {
      Assert.getInstance().notNull(configurator, "cardResourceProfileConfigurator");
      this.cardResourceProfileConfigurators.add(configurator);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardResourceServiceConfigurator withBlockingAllocationMode(
      int cycleDurationMillis, int timeoutMillis) {
    Assert.getInstance()
        .greaterOrEqual(cycleDurationMillis, 1, "cycleDurationMillis")
        .greaterOrEqual(timeoutMillis, 1, "timeoutMillis");
    if (this.isBlockingAllocationMode) {
      throw new IllegalStateException("Allocation mode already configured.");
    }
    this.isBlockingAllocationMode = true;
    this.cycleDurationMillis = cycleDurationMillis;
    this.timeoutMillis = timeoutMillis;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void configure() {

    // Configure default values.
    if (plugins == null) {
      plugins = Collections.emptyList();
    }
    if (configuredPlugins == null) {
      configuredPlugins = Collections.emptyList();
    }
    if (poolPlugins == null) {
      poolPlugins = Collections.emptyList();
    }

    // Check global plugins (regular + pool).
    Set<Plugin> allPlugins = new HashSet<Plugin>(1);
    allPlugins.addAll(plugins);
    allPlugins.addAll(poolPlugins);
    if (allPlugins.isEmpty()) {
      throw new IllegalStateException("No plugin configured.");
    }

    // Check card resource profiles.
    if (cardResourceProfileConfigurators.isEmpty()) {
      throw new IllegalStateException("No card resource profile configured.");
    }
    // Check card resource profiles names and plugins.
    Set<String> profileNames = new HashSet<String>(1);
    for (CardResourceProfileConfigurator profile : cardResourceProfileConfigurators) {
      // Check name.
      if (!profileNames.add(profile.getProfileName())) {
        throw new IllegalStateException(
            "Some card resource profiles are configured with the same profile name.");
      }
      // Check plugins.
      if (!allPlugins.containsAll(profile.getPlugins())) {
        throw new IllegalStateException(
            "Some card resource profiles specify plugins which are not configured in the global list.");
      }
    }

    // Remove plugins not used by a least one card profile.
    Set<Plugin> usedPlugins = computeUsedPlugins(allPlugins);

    if (usedPlugins.size() != allPlugins.size()) {

      Set<Plugin> unusedPlugins = new HashSet<Plugin>(allPlugins);
      unusedPlugins.removeAll(usedPlugins);

      plugins.removeAll(unusedPlugins);
      configuredPlugins.removeAll(getConfiguredPlugins(unusedPlugins));
      poolPlugins.removeAll(extractPoolPlugins(unusedPlugins));
    }

    // Apply the configuration.
    CardResourceServiceAdapter.getInstance().configure(this);
  }

  /**
   * (private)<br>
   * Computes the collection of the plugins used by at least one card profile.
   *
   * @return A not null collection.
   */
  private Set<Plugin> computeUsedPlugins(Set<Plugin> configuredPlugins) {
    Set<Plugin> usedPlugins = new HashSet<Plugin>(1);
    for (CardResourceProfileConfigurator profile : cardResourceProfileConfigurators) {
      if (!profile.getPlugins().isEmpty()) {
        usedPlugins.addAll(profile.getPlugins());
      } else {
        return configuredPlugins;
      }
    }
    return usedPlugins;
  }

  /**
   * (private)<br>
   * Gets all {@link ConfiguredPlugin} associated to a plugin contained in the provided collection.
   *
   * @param plugins The reference collection.
   * @return A not null collection.
   */
  private List<ConfiguredPlugin> getConfiguredPlugins(Set<Plugin> plugins) {
    List<ConfiguredPlugin> results = new ArrayList<ConfiguredPlugin>(1);
    for (ConfiguredPlugin configuredRegularPlugin : configuredPlugins) {
      if (plugins.contains(configuredRegularPlugin.getPlugin())) {
        results.add(configuredRegularPlugin);
      }
    }
    return results;
  }

  /**
   * (private)<br>
   * Extracts all {@link PoolPlugin} from a collection of {@link Plugin}.
   *
   * @param plugins The origin collection.
   * @return A not null list
   */
  private List<PoolPlugin> extractPoolPlugins(Set<Plugin> plugins) {
    List<PoolPlugin> results = new ArrayList<PoolPlugin>(1);
    for (Plugin plugin : plugins) {
      if (plugin instanceof PoolPlugin) {
        results.add((PoolPlugin) plugin);
      }
    }
    return results;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null list.
   * @since 2.0
   */
  List<Plugin> getPlugins() {
    return plugins;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null list.
   * @since 2.0
   */
  List<ConfiguredPlugin> getConfiguredPlugins() {
    return configuredPlugins;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null reference.
   * @since 2.0
   */
  AllocationStrategy getAllocationStrategy() {
    return allocationStrategy;
  }

  /**
   * (package-private)<br>
   *
   * @return 0 if no usage timeout is set.
   * @since 2.0
   */
  int getUsageTimeoutMillis() {
    return usageTimeoutMillis;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null list.
   * @since 2.0
   */
  List<PoolPlugin> getPoolPlugins() {
    return poolPlugins;
  }

  /**
   * (package-private)<br>
   *
   * @return True if pool plugins are prior to regular plugins.
   * @since 2.0
   */
  boolean isUsePoolFirst() {
    return usePoolFirst;
  }

  /**
   * (package-private)<br>
   * Gets the configurations of all configured card resource profiles.
   *
   * @return A not null reference.
   * @since 2.0
   */
  Set<CardResourceProfileConfigurator> getCardResourceProfileConfigurators() {
    return cardResourceProfileConfigurators;
  }

  /**
   * (package-private)<br>
   *
   * @return A not null boolean.
   * @since 2.0
   */
  boolean isBlockingAllocationMode() {
    return isBlockingAllocationMode;
  }

  /**
   * (package-private)<br>
   *
   * @return A positive int.
   * @since 2.0
   */
  int getCycleDurationMillis() {
    return cycleDurationMillis;
  }

  /**
   * (package-private)<br>
   *
   * @return A positive int.
   * @since 2.0
   */
  int getTimeoutMillis() {
    return timeoutMillis;
  }
}
