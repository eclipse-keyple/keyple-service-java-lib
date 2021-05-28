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

import org.calypsonet.terminal.reader.CardReader;
import org.eclipse.keyple.core.common.KeypleReaderExtension;

/**
 * Drives the underlying hardware to configure the search and check for the presence of cards.
 *
 * @since 2.0
 */
public interface Reader extends CardReader {

  /**
   * Returns the {@link KeypleReaderExtension} that is reader-specific.
   *
   * <p>Note: the provided argument is used at compile time to check the type consistency.
   *
   * @param readerExtensionType The specific class of the reader.
   * @param <T> The type of the reader extension.
   * @return A {@link KeypleReaderExtension}.
   * @throws IllegalStateException If reader is no longer registered.
   * @since 2.0
   */
  <T extends KeypleReaderExtension> T getExtension(Class<T> readerExtensionType);
}
