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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.util.Assert;

/**
 * Configurator of all pool plugins to associate to the card resource service.
 *
 * @since 2.0
 */
public final class PoolPluginsConfigurator {

  private final boolean usePoolFirst;
  private final List<PoolPlugin> poolPlugins;

  private PoolPluginsConfigurator(Builder builder) {
    usePoolFirst = builder.usePoolFirst;
    poolPlugins = builder.poolPlugins;
  }

  /**
   * (package-private)<br>
   *
   * @return True if pool plugins must be used prior to "regular" plugins.
   * @since 2.0
   */
  boolean isUsePoolFirst() {
    return usePoolFirst;
  }

  /**
   * (package-private)<br>
   * Gets the list of all configured "pool" plugins.
   *
   * @return A not empty list.
   * @since 2.0
   */
  List<PoolPlugin> getPoolPlugins() {
    return poolPlugins;
  }

  /**
   * Gets the configurator's builder to use in order to create a new instance.
   *
   * @return A not null reference.
   * @since 2.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder of {@link PoolPluginsConfigurator}.
   *
   * @since 2.0
   */
  public static class Builder {

    private Boolean usePoolFirst;
    private final List<PoolPlugin> poolPlugins;

    private Builder() {
      poolPlugins = new ArrayList<PoolPlugin>(1);
    }

    /**
     * Configures the card resource service to search for available cards in pool plugins before
     * regular plugins.
     *
     * <p>Default value: pool last
     *
     * @return The current builder instance.
     * @throws IllegalStateException If the setting has already been configured.
     * @since 2.0
     */
    public Builder usePoolFirst() {
      if (usePoolFirst != null) {
        throw new IllegalStateException("Pool plugins priority already configured.");
      }
      usePoolFirst = true;
      return this;
    }

    /**
     * Adds a {@link PoolPlugin} to the default list of all card profiles.
     *
     * <p><u>Note:</u> The order of the plugins is important because it will be kept during the
     * allocation process unless redefined by card profiles.
     *
     * @param poolPlugin The pool plugin to add.
     * @return The current builder instance.
     * @throws IllegalArgumentException If the provided pool plugin is null.
     * @throws IllegalStateException If the pool plugin has already been configured.
     * @since 2.0
     */
    public Builder addPoolPlugin(PoolPlugin poolPlugin) {
      Assert.getInstance().notNull(poolPlugin, "poolPlugin");
      if (poolPlugins.contains(poolPlugin)) {
        throw new IllegalStateException("Pool plugin already configured.");
      }
      poolPlugins.add(poolPlugin);
      return this;
    }

    /**
     * Creates a new instance of {@link PoolPluginsConfigurator} using the current configuration.
     *
     * @return A new instance.
     * @throws IllegalStateException If no pool plugin has been configured.
     * @since 2.0
     */
    public PoolPluginsConfigurator build() {
      if (poolPlugins.isEmpty()) {
        throw new IllegalStateException("No pool plugin was configured.");
      }
      if (usePoolFirst == null) {
        usePoolFirst = false;
      }
      return new PoolPluginsConfigurator(this);
    }
  }
}
