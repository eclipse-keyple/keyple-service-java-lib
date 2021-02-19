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

import org.eclipse.keyple.core.commons.KeypleDefaultSelectionsResponse;
import org.eclipse.keyple.core.commons.KeypleReaderEvent;

/**
 * This POJO is used to propagate a change of a card state in an {@link ObservableReader}.
 *
 * <p>The various events that can occur concern the insertion and removal of a card from a reader.
 *
 * <p>When an insertion is made there are two cases depending on whether a default selection has
 * been programmed or not.
 *
 * @since 2.0
 */
public final class ReaderEvent implements KeypleReaderEvent {

  private final String pluginName;
  private final String readerName;
  private final KeypleDefaultSelectionsResponse defaultResponses;

  /**
   * The different types of reader events, reflecting the status of the reader regarding the
   * presence of a card.
   *
   * @since 2.0
   */
  public enum EventType {

    /** A card has been inserted. */
    CARD_INSERTED,

    /** A card has been inserted and the default requests process has been successfully operated. */
    CARD_MATCHED,

    /** The card has been removed and is no longer able to communicate with the reader */
    CARD_REMOVED,

    /** The reader has been unregistered */
    UNREGISTERED
  }

  /** The type of event */
  private final EventType eventType;

  /**
   * ReaderEvent constructor for simple insertion notification mode
   *
   * @param pluginName the name of the current plugin (should be not null)
   * @param readerName the name of the current reader (should be not null)
   * @param eventType the type of event (should be not null)
   * @param defaultSelectionsResponse the response to the default AbstractDefaultSelectionsRequest
   *     (may be null)
   * @since 2.0
   */
  public ReaderEvent(
      String pluginName,
      String readerName,
      EventType eventType,
      KeypleDefaultSelectionsResponse defaultSelectionsResponse) {
    this.pluginName = pluginName;
    this.readerName = readerName;
    this.eventType = eventType;
    this.defaultResponses = defaultSelectionsResponse;
  }

  /**
   * Gets the name of the plugin from which the reader that generated the event comes from
   *
   * @return A not empty string.
   * @since 2.0
   */
  public String getPluginName() {
    return pluginName;
  }

  /**
   * Gets the name of the reader that generated the event comes from
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
   * Gets the default selection response that may be present when the event is {@link
   * EventType#CARD_INSERTED}, always present when the event is {@link EventType#CARD_MATCHED} and
   * null in the others cases.
   *
   * @return A nullable value.
   * @since 2.0
   */
  public KeypleDefaultSelectionsResponse getDefaultSelectionsResponse() {
    return defaultResponses;
  }

  /**
   * Gets the {@link Plugin} from which the reader that generated the event comes from.
   *
   * @return A not null reference to a Plugin object.
   * @since 2.0
   */
  public Plugin getPlugin() {
    // TODO check this
    return SmartCardService.getInstance().getPlugin(pluginName);
  }

  /**
   * Gets the {@link Reader} from which generated event comes from
   *
   * @return A not null reference to a Reader object.
   * @since 2.0
   */
  public Reader getReader() {
    // TODO check this
    return getPlugin().getReader(readerName);
  }
}
