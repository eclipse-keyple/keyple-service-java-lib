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

import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse;

/**
 * Implementation of {@link CardReaderEvent}.
 *
 * @since 2.0.0
 */
final class ReaderEventAdapter implements CardReaderEvent {

  private final String pluginName;
  private final String readerName;
  private final ScheduledCardSelectionsResponse scheduledCardSelectionsResponse;

  private final Type type;

  /**
   * CardReaderEvent constructor for simple insertion notification mode
   *
   * @param pluginName The name of the current plugin (should be not null).
   * @param readerName The name of the current reader (should be not null).
   * @param type The type of event (should be not null).
   * @param scheduledCardSelectionsResponse The responses received during the execution of the card
   *     selection scenario (can be null).
   * @since 2.0.0
   */
  ReaderEventAdapter(
      String pluginName,
      String readerName,
      Type type,
      ScheduledCardSelectionsResponse scheduledCardSelectionsResponse) {
    this.pluginName = pluginName;
    this.readerName = readerName;
    this.type = type;
    this.scheduledCardSelectionsResponse = scheduledCardSelectionsResponse;
  }

  /**
   * Returns the plugin name.
   *
   * @return A not empty String.
   * @since 2.0.0
   */
  String getPluginName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public String getReaderName() {
    return readerName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public Type getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public ScheduledCardSelectionsResponse getScheduledCardSelectionsResponse() {
    return scheduledCardSelectionsResponse;
  }
}
