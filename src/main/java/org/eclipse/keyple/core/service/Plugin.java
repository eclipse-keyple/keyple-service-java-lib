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
import java.util.Set;
import org.eclipse.keyple.core.common.KeyplePluginExtension;

/**
 * Manager for one or more {@link Reader} from the same family.
 *
 * <p>Provides the means to get the plugin name, enumerate and retrieve the readers.
 *
 * @since 2.0
 */
public interface Plugin {

  /**
   * Gets the name of the plugin.
   *
   * @return A not empty string.
   * @since 2.0
   */
  String getName();

  /**
   * Returns the {@link KeyplePluginExtension} that is plugin-specific.
   *
   * <p>Note: the provided argument is used at compile time to check the type consistency.
   *
   * @param pluginExtensionType The specific class of the plugin.
   * @param <T> The type of the plugin extension.
   * @return A not null reference.
   * @since 2.0
   */
  <T extends KeyplePluginExtension> T getExtension(Class<T> pluginExtensionType);

  /**
   * Gets the list of names of all available readers.
   *
   * @return An empty set if there's no reader connected.
   * @throws IllegalStateException if plugin is no longer registered.
   * @since 2.0
   */
  Set<String> getReadersNames();

  /**
   * Gets a map whose elements have a reader name as a key and a {@link Reader} as a value.
   *
   * @return An empty Map if there's no reader connected.
   * @throws IllegalStateException if the plugin is no longer registered.
   * @since 2.0
   */
  Map<String, Reader> getReaders();

  /**
   * Gets the {@link Reader} whose name is provided.
   *
   * @param readerName The name of the reader.
   * @return A not null reference.
   * @throws KeypleReaderNotFoundException if the wanted reader is not found
   * @throws IllegalStateException if the plugin is no longer registered.
   * @since 2.0
   */
  Reader getReader(String readerName);
}
