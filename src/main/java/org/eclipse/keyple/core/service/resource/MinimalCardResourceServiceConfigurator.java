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

import org.eclipse.keyple.core.common.KeypleCardResourceProfileExtension;
import org.eclipse.keyple.core.service.ObservablePlugin;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;

/**
 * Minimal configurator of the card resource service.
 *
 * <p>The minimal configuration uses all default settings and allocation strategies :
 *
 * <ul>
 *   <li>Use first allocation strategy for "regular" plugin.
 *   <li>Use non blocking allocation mode.
 *   <li>No plugin or reader monitoring.
 * </ul>
 *
 * @since 2.0
 */
public interface MinimalCardResourceServiceConfigurator {

  /**
   * Configures the card resource service with one {@link Plugin} or {@link ObservablePlugin} with
   * all default settings.
   *
   * @param plugin The plugin to use.
   * @param readerConfiguratorSpi The configurator to use to setup the reader before to use it.
   * @param cardResourceProfileExtension The card resource profile extension to use for matching
   *     card.
   * @param readerNameRegex The reader name regex value to use to filter the used readers of the
   *     provided plugin (optional).
   * @return Next configuration step.
   * @throws IllegalArgumentException If the plugin is null or an instance of a {@link PoolPlugin}
   *     or if the profile extension or the reader configurator is null.
   * @since 2.0
   */
  CardResourceServiceConfigurator.ConfigurationStep withPlugin(
      Plugin plugin,
      ReaderConfiguratorSpi readerConfiguratorSpi,
      KeypleCardResourceProfileExtension cardResourceProfileExtension,
      String readerNameRegex);

  /**
   * Configures the card resource service with one {@link PoolPlugin} with all default settings.
   *
   * @param poolPlugin The pool plugin to use.
   * @param cardResourceProfileExtension The card resource profile extension to use for matching
   *     card.
   * @param readerGroupReference The reader group reference to use.
   * @return Next configuration step.
   * @throws IllegalArgumentException If the pool plugin is null or the profile extension or the
   *     reader group reference is null or empty.
   * @since 2.0
   */
  CardResourceServiceConfigurator.ConfigurationStep withPoolPlugin(
      PoolPlugin poolPlugin,
      KeypleCardResourceProfileExtension cardResourceProfileExtension,
      String readerGroupReference);
}
