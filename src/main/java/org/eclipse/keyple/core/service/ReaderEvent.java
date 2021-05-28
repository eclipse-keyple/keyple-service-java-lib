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

import org.calypsonet.terminal.reader.CardReaderEvent;
import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse;

/**
 * This POJO contains all information about a change of card state within an {@link
 * ObservableReader}.
 *
 * <p>In the case of a card insertion, the responses received by the reader are included in the
 * event.
 *
 * @since 2.0
 */
public final class ReaderEvent implements CardReaderEvent {

  private final String pluginName;
  private final String readerName;
  private final ScheduledCardSelectionsResponse scheduledCardSelectionsResponse;

  private final EventType eventType;

  /**
   * ReaderEvent constructor for simple insertion notification mode
   *
   * @param pluginName The name of the current plugin (should be not null).
   * @param readerName The name of the current reader (should be not null).
   * @param eventType The type of event (should be not null).
   * @param scheduledCardSelectionsResponse The responses received during the execution of the card
   *     selection scenario (can be null).
   * @since 2.0
   */
  public ReaderEvent(
      String pluginName,
      String readerName,
      EventType eventType,
      ScheduledCardSelectionsResponse scheduledCardSelectionsResponse) {
    this.pluginName = pluginName;
    this.readerName = readerName;
    this.eventType = eventType;
    this.scheduledCardSelectionsResponse = scheduledCardSelectionsResponse;
  }

  /**
   * Gets the name of the plugin from which the reader that generated the event comes from.
   *
   * @return A not empty string.
   * @since 2.0
   */
  public String getPluginName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public String getReaderName() {
    return readerName;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public EventType getEventType() {
    return eventType;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ScheduledCardSelectionsResponse getScheduledCardSelectionsResponse() {
    return scheduledCardSelectionsResponse;
  }
}
