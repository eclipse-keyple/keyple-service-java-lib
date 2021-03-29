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

import org.eclipse.keyple.core.common.KeypleReaderExtension;

/**
 * Drives the underlying hardware to configure the search and check for the presence of cards.
 *
 * @since 2.0
 */
public interface Reader {

  /**
   * Gets the name of the reader.
   *
   * @return A not empty string.
   * @since 2.0
   */
  String getName();

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

  /**
   * Tells if the current card communication is contactless.
   *
   * @return true if the communication is contactless, false if not.
   * @throws IllegalStateException If reader is no longer registered.
   * @since 2.0
   */
  boolean isContactless();

  /**
   * Checks if is the card present.
   *
   * @return true if a card is present in the reader.
   * @throws KeypleReaderCommunicationException If the communication with the reader has failed.
   * @throws IllegalStateException If reader is no longer registered.
   * @since 2.0
   */
  boolean isCardPresent();

  /**
   * Activates the provided card protocol and assigns it a name.
   *
   * <ul>
   *   <li>Activates the detection of cards using this protocol (if the plugin allows it).
   *   <li>Asks the plugin to take this protocol into account if a card using this protocol is
   *       identified during the selection phase.
   *   <li>Internally associates the two strings provided as arguments.
   *   <li>The #readerProtocolName argument is the name of the protocol among those supported by the
   *       reader.
   *   <li>The #cardProtocol is the protocol name shared with the application when a card
   *       communicating with the provided reader protocol is detected.
   * </ul>
   *
   * Note: in the case where multiple protocols are activated, they will be checked in the selection
   * process in the order in which they were activated. The most likely cases should therefore be
   * activated first.
   *
   * @param readerProtocolName The name of the protocol as known by the reader.
   * @param cardProtocol The name of the protocol as known by the application.
   * @throws IllegalArgumentException If one of the provided protocol is null.
   * @throws IllegalStateException If reader is no longer registered.
   * @throws KeypleReaderProtocolNotSupportedException If the protocol is not supported.
   * @since 2.0
   */
  void activateProtocol(String readerProtocolName, String cardProtocol);

  /**
   * Deactivates the provided card protocol.
   *
   * <ul>
   *   <li>Inhibits the detection of cards using this protocol.
   *   <li>Ask the plugin to ignore this protocol if a card using this protocol is identified during
   *       the selection phase.
   * </ul>
   *
   * @param readerProtocolName A not empty String.
   * @throws IllegalStateException If reader is no longer registered.
   * @throws IllegalArgumentException If the provided protocol is null.
   * @throws KeypleReaderProtocolNotSupportedException If the protocol is not supported.
   * @since 2.0
   */
  void deactivateProtocol(String readerProtocolName);
}
