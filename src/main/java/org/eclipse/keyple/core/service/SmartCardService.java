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

import java.util.Set;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.eclipse.keyple.core.common.KeypleCardExtension;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtensionFactory;
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory;

/**
 * Keyple main service.
 *
 * @since 2.0.0
 */
public interface SmartCardService {

  /**
   * Registers a new plugin to the service.
   *
   * @param pluginFactory The plugin factory.
   * @return A not null reference to the registered {@link Plugin}.
   * @throws KeyplePluginException If instantiation failed.
   * @throws IllegalStateException If the plugin has already been registered.
   * @since 2.0.0
   */
  Plugin registerPlugin(KeyplePluginExtensionFactory pluginFactory);

  /**
   * Attempts to unregister the plugin having the provided name from the service.
   *
   * @param pluginName The plugin name.
   * @since 2.0.0
   */
  void unregisterPlugin(String pluginName);

  /**
   * Returns the names of all registered plugins.
   *
   * @return A not null Set String.
   * @since 2.0.0
   */
  Set<String> getPluginNames();

  /**
   * Returns all registered plugins.
   *
   * @return A not null Set of {@link Plugin}.
   * @since 2.0.0
   */
  Set<Plugin> getPlugins();

  /**
   * Returns the plugin whose name is provided as an argument.
   *
   * @param pluginName The plugin name.
   * @return Null if the plugin is not found or no longer registered.
   * @since 2.0.0
   */
  Plugin getPlugin(String pluginName);

  /**
   * Returns the plugin associated to the provided {@link CardReader}.
   *
   * @param reader The card reader.
   * @return Null if the plugin is not found or no longer registered.
   * @since 2.1.0
   */
  Plugin getPlugin(CardReader reader);

  /**
   * Returns the reader associated to the provided unique name.
   *
   * @param readerName The name of the card reader.
   * @return Null if the reader is not found or no longer registered.
   * @since 2.1.0
   */
  CardReader getReader(String readerName);

  /**
   * Verifies the compatibility with the service of the provided card extension.
   *
   * <p>The verification is based on the comparison of the respective API versions.
   *
   * @param cardExtension A not null {@link KeypleCardExtension} reference object
   * @since 2.0.0
   */
  void checkCardExtension(KeypleCardExtension cardExtension);

  /**
   * Registers a new distributed local service to the service.
   *
   * @param distributedLocalServiceExtensionFactory Factory to use to instantiate a Distributed
   *     Local Service extension
   * @return A not null reference to the registered {@link DistributedLocalService}.
   * @throws IllegalStateException If the distributed local service has already been registered.
   * @since 2.0.0
   */
  DistributedLocalService registerDistributedLocalService(
      KeypleDistributedLocalServiceExtensionFactory distributedLocalServiceExtensionFactory);

  /**
   * Attempts to unregister the distributed local service having the provided name from the service.
   *
   * @param distributedLocalServiceName The distributed local service name.
   * @since 2.0.0
   */
  void unregisterDistributedLocalService(String distributedLocalServiceName);

  /**
   * Checks whether a distributed local service is already registered to the service or not.
   *
   * @param distributedLocalServiceName The name of the distributed local service to be checked.
   * @return True if the distributed local service is registered.
   * @since 2.0.0
   */
  boolean isDistributedLocalServiceRegistered(String distributedLocalServiceName);

  /**
   * Returns the distributed local service having the provided name.
   *
   * @param distributedLocalServiceName The name of the distributed local service.
   * @return Null if the distributed local service is not found or no longer registered.
   * @since 2.0.0
   */
  DistributedLocalService getDistributedLocalService(String distributedLocalServiceName);

  /**
   * Create a new instance of a {@link CardSelectionManager} in order to perform a card selection.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  CardSelectionManager createCardSelectionManager();
}
