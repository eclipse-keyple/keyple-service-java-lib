/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://calypsonet.org/
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

import java.util.List;
import org.calypsonet.terminal.card.ChannelControl;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * (package-private)<br>
 * This POJO contains a selection scenario composed of one or more card selection requests and
 * indicators specifying the expected behavior.
 *
 * <p>It comprises:
 *
 * <ul>
 *   <li>A list of {@link CardSelectionRequestSpi} corresponding to the selection targets.
 *   <li>A {@link MultiSelectionProcessing} indicator specifying whether all scheduled card
 *       selections are to be executed or whether to stop at the first one that is successful.
 *   <li>A {@link ChannelControl} indicator controlling the physical channel the end of the
 *       selection process.
 * </ul>
 *
 * @since 2.0.0
 */
final class CardSelectionScenarioAdapter {

  private final List<CardSelectionRequestSpi> cardSelectionRequests;
  private final MultiSelectionProcessing multiSelectionProcessing;
  private final ChannelControl channelControl;

  /**
   * (package-private)<br>
   * Builds a card selection scenario from a list of selection cases and two enum constants guiding
   * the expected behaviour of the selection process.
   *
   * <p>Note: the {@link CardSelectionRequestSpi} list should be carefully ordered in accordance
   * with the cards expected in the application to optimize the processing time of the selection
   * process. The first selection case in the list will be processed first.
   *
   * @param cardSelectionRequests A list of card selection requests.
   * @param multiSelectionProcessing The multi selection processing policy.
   * @param channelControl The channel control policy.
   * @throws IllegalArgumentException if the card selection request list is null or empty, if one of
   *     the indicators is null.
   * @since 2.0.0
   */
  CardSelectionScenarioAdapter(
      List<CardSelectionRequestSpi> cardSelectionRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl) {

    Assert.getInstance()
        .notEmpty(cardSelectionRequests, "cardSelectionRequests")
        .notNull(multiSelectionProcessing, "multiSelectionProcessing")
        .notNull(channelControl, "channelControl");

    this.cardSelectionRequests = cardSelectionRequests;
    this.multiSelectionProcessing = multiSelectionProcessing;
    this.channelControl = channelControl;
  }

  /**
   * (package-private)<br>
   * Gets the card selection requests list.
   *
   * @return A not null reference
   * @since 2.0.0
   */
  List<CardSelectionRequestSpi> getCardSelectionRequests() {
    return cardSelectionRequests;
  }

  /**
   * (package-private)<br>
   * Gets the multi selection processing policy.
   *
   * @return A not null reference
   * @since 2.0.0
   */
  MultiSelectionProcessing getMultiSelectionProcessing() {
    return multiSelectionProcessing;
  }

  /**
   * (package-private)<br>
   * Gets the channel control policy.
   *
   * @return A not null reference
   * @since 2.0.0
   */
  ChannelControl getChannelControl() {
    return channelControl;
  }

  /**
   * Converts the card selection scenario into a string where the data is encoded in a json format.
   *
   * @return A not empty String
   * @since 2.0.0
   */
  @Override
  public String toString() {
    return "CARD_SELECTION_SCENARIO = " + JsonUtil.toJson(this);
  }
}
