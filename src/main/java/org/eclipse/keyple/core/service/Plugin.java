/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import java.util.Set;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keypop.reader.CardReader;

/**
 * Manager for one or more {@link CardReader} from the same family.
 *
 * <p>Provides the means to get the plugin name, enumerate and retrieve the readers.
 *
 * @since 2.0.0
 */
public interface Plugin {

  /**
   * Gets the name of the plugin.
   *
   * @return A not empty string.
   * @since 2.0.0
   */
  String getName();

  /**
   * Returns the {@link KeyplePluginExtension} that is plugin-specific.
   *
   * <p>Note: the provided argument is used at compile time to check the type consistency.
   *
   * @param pluginExtensionClass The specific class of the plugin.
   * @param <T> The type of the plugin extension.
   * @return A not null reference.
   * @since 2.0.0
   */
  <T extends KeyplePluginExtension> T getExtension(Class<T> pluginExtensionClass);

  /**
   * Returns the {@link KeypleReaderExtension} that is reader-specific.
   *
   * <p>Note: the provided argument is used at compile time to check the type consistency.
   *
   * @param readerExtensionClass The specific class of the reader.
   * @param readerName The name of the reader.
   * @param <T> The type of the reader extension.
   * @return A {@link KeypleReaderExtension}.
   * @throws IllegalStateException If plugin or reader is no longer registered.
   * @throws IllegalArgumentException If the reader name is unknown.
   * @since 2.1.0
   */
  <T extends KeypleReaderExtension> T getReaderExtension(
      Class<T> readerExtensionClass, String readerName);

  /**
   * Gets the names of all connected readers.
   *
   * @return An empty set if there's no reader connected.
   * @throws IllegalStateException if plugin is no longer registered.
   * @since 2.0.0
   */
  Set<String> getReaderNames();

  /**
   * Gets all connected readers.
   *
   * @return An empty Set if there's no reader connected.
   * @throws IllegalStateException if the plugin is no longer registered.
   * @since 2.0.0
   */
  Set<CardReader> getReaders();

  /**
   * Gets the {@link CardReader} whose name is provided.
   *
   * @param name The name of the reader.
   * @return Null if the reader has not been found.
   * @throws IllegalStateException if the plugin is no longer registered.
   * @since 2.0.0
   */
  CardReader getReader(String name);

  /**
   * Returns the first reader whose name matches the provided regular expression.
   *
   * @param readerNameRegex The name of the card reader as a regular expression string.
   * @return Null if the reader is not found or no longer registered.
   * @throws IllegalArgumentException If the provided regex is invalid.
   * @since 3.1.0
   */
  CardReader findReader(String readerNameRegex);
}
