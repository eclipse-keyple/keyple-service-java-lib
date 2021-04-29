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

import org.eclipse.keyple.core.common.KeypleReaderEvent;
import org.eclipse.keyple.core.service.selection.ScheduledCardSelectionsResponse;

/**
 * This POJO contains all information about a change of card state within an {@link
 * ObservableReader}.
 *
 * <p>In the case of a card insertion, the responses received by the reader are included in the
 * event.
 *
 * @since 2.0
 */
public final class ReaderEvent implements KeypleReaderEvent {

  private final String pluginName;
  private final String readerName;
  private final ScheduledCardSelectionsResponse scheduledCardSelectionsResponse;

  /**
   * The different types of reader events, reflecting the status of the reader regarding the
   * presence of a card.
   *
   * @since 2.0
   */
  public enum EventType {

    /**
     * A card has been inserted with or without specific selection.
     *
     * @since 2.0
     */
    CARD_INSERTED,

    /**
     * A card has been inserted and matched the selection.
     *
     * @since 2.0
     */
    CARD_MATCHED,

    /**
     * The card has been removed from the reader.
     *
     * @since 2.0
     */
    CARD_REMOVED,

    /**
     * The reader has been unregistered.
     *
     * @since 2.0
     */
    UNREGISTERED
  }

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
   * Gets the name of the reader that generated the event comes from.
   *
   * @return A not empty string.
   * @since 2.0
   */
  public String getReaderName() {
    return readerName;
  }

  /**
   * Gets the reader event type.
   *
   * @return A not null value.
   * @since 2.0
   */
  public EventType getEventType() {
    return eventType;
  }

  /**
   * Gets the card selection responses that may be present when the event is {@link
   * EventType#CARD_INSERTED}, always present when the event is {@link EventType#CARD_MATCHED} and
   * null in the others cases.
   *
   * @return null if the event is not carrying a {@link ScheduledCardSelectionsResponse}.
   * @since 2.0
   */
  public ScheduledCardSelectionsResponse getScheduledCardSelectionsResponse() {
    return scheduledCardSelectionsResponse;
  }
}
