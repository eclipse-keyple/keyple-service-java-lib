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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.core.card.*;
import org.eclipse.keyple.core.card.spi.CardSelectionSpi;
import org.eclipse.keyple.core.service.selection.CardSelectionResult;
import org.eclipse.keyple.core.service.selection.CardSelectionService;
import org.eclipse.keyple.core.service.selection.MultiSelectionProcessing;
import org.eclipse.keyple.core.service.selection.ScheduledCardSelectionsResponse;
import org.eclipse.keyple.core.service.selection.spi.CardSelection;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;
import org.eclipse.keyple.core.util.Assert;

/**
 * (package-private) <br>
 * Implementation of the {@link CardSelectionService}.
 *
 * @since 2.0
 */
final class CardSelectionServiceAdapter implements CardSelectionService {

  private final List<CardSelectionSpi> cardSelections;
  private final List<CardSelectionRequest> cardSelectionRequests;
  private final MultiSelectionProcessing multiSelectionProcessing;
  private ChannelControl channelControl = ChannelControl.KEEP_OPEN;

  /**
   * (package-private) <br>
   * Creates an instance of the service with which the selection stops as soon as a card matches a
   * selection case.
   *
   * @since 2.0
   */
  CardSelectionServiceAdapter(MultiSelectionProcessing multiSelectionProcessing) {
    this.multiSelectionProcessing = multiSelectionProcessing;
    cardSelections = new ArrayList<CardSelectionSpi>();
    cardSelectionRequests = new ArrayList<CardSelectionRequest>();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public int prepareSelection(CardSelection cardSelection) {
    /* keep the selection request */
    cardSelections.add((CardSelectionSpi) cardSelection);
    cardSelectionRequests.add(((CardSelectionSpi) cardSelection).getCardSelectionRequest());
    /* return the selection index (starting at 0) */
    return cardSelections.size() - 1;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void prepareReleaseChannel() {
    channelControl = ChannelControl.CLOSE_AFTER;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardSelectionResult processCardSelectionScenario(Reader reader) {

    // Communicate with the card to make the actual selection
    List<CardSelectionResponse> cardSelectionResponses;

    try {
      cardSelectionResponses =
          ((AbstractReaderAdapter) reader)
              .transmitCardSelectionRequests(
                  cardSelectionRequests, multiSelectionProcessing, channelControl);
    } catch (ReaderCommunicationException e) {
      throw new KeypleReaderCommunicationException(e.getMessage(), e);
    } catch (CardCommunicationException e) {
      throw new KeypleCardCommunicationException(e.getMessage(), e);
    }

    // clear the selection requests list
    cardSelectionRequests.clear();

    // Analyze the received responses
    return processCardSelectionResponses(cardSelectionResponses);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void scheduleCardSelectionScenario(
      ObservableReader observableReader, ObservableReader.NotificationMode notificationMode) {
    scheduleCardSelectionScenario(
        observableReader, notificationMode, ObservableReader.PollingMode.REPEATING);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void scheduleCardSelectionScenario(
      ObservableReader observableReader,
      ObservableReader.NotificationMode notificationMode,
      ObservableReader.PollingMode pollingMode) {
    CardSelectionScenarioAdapter cardSelectionScenario =
        new CardSelectionScenarioAdapter(
            cardSelectionRequests, multiSelectionProcessing, channelControl);
    ((ObservableLocalReaderAdapter) observableReader)
        .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode, pollingMode);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardSelectionResult parseScheduledCardSelectionsResponse(
      ScheduledCardSelectionsResponse scheduledCardSelectionsResponse) {

    Assert.getInstance()
        .notNull(scheduledCardSelectionsResponse, "scheduledCardSelectionsResponse");

    return processCardSelectionResponses(
        ((ScheduledCardSelectionsResponseAdapter) scheduledCardSelectionsResponse)
            .getCardSelectionResponses());
  }

  /**
   * (private)<br>
   * Analyzes the responses received in return of the execution of a card selection scenario and
   * returns the CardSelectionResult.
   *
   * @param cardSelectionResponses The card selection responses.
   * @return A not null reference.
   * @throws IllegalArgumentException If the list is null or empty.
   */
  private CardSelectionResult processCardSelectionResponses(
      List<CardSelectionResponse> cardSelectionResponses) {

    Assert.getInstance().notEmpty(cardSelectionResponses, "cardSelectionResponses");

    CardSelectionResultAdapter cardSelectionsResult = new CardSelectionResultAdapter();

    int index = 0;

    /* Check card responses */
    for (CardSelectionResponse cardSelectionResponse : cardSelectionResponses) {
      /* test if the selection is successful: we should have either a FCI or an ATR */
      if (cardSelectionResponse != null
          && cardSelectionResponse.getSelectionStatus() != null
          && cardSelectionResponse.getSelectionStatus().hasMatched()) {
        /*
         * create a AbstractSmartCard with the class deduced from the selection request
         * during the selection preparation
         */
        SmartCard smartCard = (SmartCard) cardSelections.get(index).parse(cardSelectionResponse);

        // determine if the current matching card is selected
        SelectionStatus selectionStatus = cardSelectionResponse.getSelectionStatus();

        boolean isSelected = selectionStatus.hasMatched();

        cardSelectionsResult.addSmartCard(index, smartCard, isSelected);
      }
      index++;
    }
    return cardSelectionsResult;
  }
}
