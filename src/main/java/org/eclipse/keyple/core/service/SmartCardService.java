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

import java.util.Map;
import org.eclipse.keyple.core.common.KeypleCardExtension;
import org.eclipse.keyple.core.common.KeypleDistributedLocalServiceExtensionFactory;
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory;

/**
 * Keyple main service.
 *
 * @since 2.0
 */
public interface SmartCardService {

  /**
   * Register a new plugin to be available in the platform if not registered yet
   *
   * @param pluginFactory plugin factory to instantiate plugin to be added
   * @throws KeyplePluginInstantiationException if instantiation failed
   * @return Plugin : registered reader plugin
   * @throws IllegalStateException if the plugin has already been registered.
   * @since 2.0
   */
  Plugin registerPlugin(KeyplePluginExtensionFactory pluginFactory);

  /**
   * Unregister plugin from platform
   *
   * @param pluginName plugin name
   * @throws IllegalStateException if the plugin or his reader(s) are already unregistered
   * @since 2.0
   */
  void unregisterPlugin(String pluginName);

  /**
   * Check whether a plugin is already registered to the platform or not
   *
   * @param pluginName name of the plugin to be checked
   * @return true if a plugin with matching name has been registered
   * @since 2.0
   */
  boolean isPluginRegistered(String pluginName);

  /**
   * Gets the plugins.
   *
   * @return the plugin names and plugin instances map of interfaced reader’s plugins.
   * @since 2.0
   */
  Map<String, Plugin> getPlugins();

  /**
   * Gets the plugin whose name is provided as an argument.
   *
   * @param pluginName the plugin name
   * @return the plugin
   * @throws KeyplePluginNotFoundException if the wanted plugin is not found
   * @since 2.0
   */
  Plugin getPlugin(String pluginName);

  /**
   * Verifies the compatibility with the service of the provided card extension.
   *
   * <p>The verification is based on the comparison of the respective API versions.
   *
   * @param cardExtension A not null {@link KeypleCardExtension} reference object
   * @since 2.0
   */
  void checkCardExtension(KeypleCardExtension cardExtension);

  /**
   * Registers a new Distributed Local Service to be available in the platform if not registered yet
   *
   * @param distributedLocalServiceExtensionFactory Factory to use to instantiate a Distributed
   *     Local Service extension
   * @return A {@link DistributedLocalService} reference
   * @since 2.0
   */
  DistributedLocalService registerDistributedLocalService(
      KeypleDistributedLocalServiceExtensionFactory distributedLocalServiceExtensionFactory);

  /**
   * TODO complete
   *
   * @param distributedLocalServiceName TODO
   * @since 2.0
   */
  void unregisterDistributedLocalService(String distributedLocalServiceName);

  /**
   * TODO complete
   *
   * @param distributedLocalServiceName TODO
   * @since 2.0
   */
  void getDistributedLocalService(String distributedLocalServiceName);
}
