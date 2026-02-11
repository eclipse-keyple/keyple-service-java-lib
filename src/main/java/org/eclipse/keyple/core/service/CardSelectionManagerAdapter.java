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

import static org.eclipse.keyple.core.service.InternalDto.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.*;
import org.eclipse.keypop.card.spi.CardSelectionExtensionSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.CardCommunicationException;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.ReaderCommunicationException;
import org.eclipse.keypop.reader.selection.*;
import org.eclipse.keypop.reader.selection.spi.CardSelectionExtension;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link CardSelectionManager}.
 *
 * @since 2.0.0
 */
final class CardSelectionManagerAdapter implements CardSelectionManager {

  private static final Logger logger = LoggerFactory.getLogger(CardSelectionManagerAdapter.class);
  private static final String MULTI_SELECTION_PROCESSING = "multiSelectionProcessing";
  private static final String CHANNEL_CONTROL = "channelControl";
  private static final String CARD_SELECTORS_TYPES = "cardSelectorsTypes";
  private static final String CARD_SELECTORS = "cardSelectors";
  private static final String CARD_SELECTIONS_TYPES = "cardSelectionsTypes";
  private static final String CARD_SELECTIONS = "cardSelections";
  private static final String DEFAULT_CARD_SELECTIONS = "defaultCardSelections";

  private final List<CardSelector<?>> cardSelectors;
  private final List<CardSelectionExtensionSpi> cardSelections;
  private final List<CardSelectionRequestSpi> cardSelectionRequests;
  private List<CardSelectionResponseApi> cardSelectionResponses;
  private MultiSelectionProcessing multiSelectionProcessing;
  private ChannelControl channelControl = ChannelControl.KEEP_OPEN;

  /**
   * Creates an instance of the service with which the selection stops as soon as a card matches a
   * selection case.
   *
   * @since 2.0.0
   */
  CardSelectionManagerAdapter() {
    multiSelectionProcessing = MultiSelectionProcessing.FIRST_MATCH;
    cardSelectors = new ArrayList<>();
    cardSelections = new ArrayList<>();
    cardSelectionRequests = new ArrayList<>();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void setMultipleSelectionMode() {
    multiSelectionProcessing = MultiSelectionProcessing.PROCESS_ALL;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public int prepareSelection(
      CardSelector<?> cardSelector, CardSelectionExtension cardSelectionExtension) {

    Assert.getInstance()
        .notNull(cardSelector, "cardSelector")
        .notNull(cardSelectionExtension, "cardSelectionExtension");

    if (!(cardSelectionExtension instanceof CardSelectionExtensionSpi)) {
      throw new IllegalArgumentException(
          "Cannot cast 'cardSelectionExtension' to CardSelectionExtensionSpi. Actual type: "
              + cardSelectionExtension.getClass().getName());
    }

    /* keep the selection request */
    cardSelectors.add(cardSelector);
    cardSelections.add((CardSelectionExtensionSpi) cardSelectionExtension);
    cardSelectionRequests.add(
        ((CardSelectionExtensionSpi) cardSelectionExtension).getCardSelectionRequest());
    /* return the selection index (starting at 0) */
    return cardSelections.size() - 1;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void prepareReleaseChannel() {
    channelControl = ChannelControl.CLOSE_AFTER;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.1
   */
  @Override
  public String exportCardSelectionScenario() {

    JsonObject jsonObject = new JsonObject();

    // Basic fields
    jsonObject.addProperty(MULTI_SELECTION_PROCESSING, multiSelectionProcessing.name());
    jsonObject.addProperty(CHANNEL_CONTROL, channelControl.name());

    // Original card selectors
    List<String> cardSelectorsTypes = new ArrayList<>(cardSelectors.size());
    for (CardSelector<?> cardSelector : cardSelectors) {
      cardSelectorsTypes.add(cardSelector.getClass().getName());
    }
    jsonObject.add(CARD_SELECTORS_TYPES, JsonUtil.getParser().toJsonTree(cardSelectorsTypes));
    jsonObject.add(CARD_SELECTORS, JsonUtil.getParser().toJsonTree(cardSelectors));

    // Original card selections
    List<String> cardSelectionsTypes = new ArrayList<>(cardSelections.size());
    for (CardSelectionExtensionSpi cardSelection : cardSelections) {
      cardSelectionsTypes.add(cardSelection.getClass().getName());
    }
    jsonObject.add(CARD_SELECTIONS_TYPES, JsonUtil.getParser().toJsonTree(cardSelectionsTypes));
    jsonObject.add(CARD_SELECTIONS, JsonUtil.getParser().toJsonTree(cardSelections));

    // Default card selections
    List<CardSelectionAdapter> defaultCardSelections = new ArrayList<>(cardSelections.size());
    for (CardSelectionExtensionSpi cardSelection : cardSelections) {
      defaultCardSelections.add(new CardSelectionAdapter(cardSelection));
    }
    jsonObject.add(DEFAULT_CARD_SELECTIONS, JsonUtil.getParser().toJsonTree(defaultCardSelections));

    return jsonObject.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.1
   */
  @Override
  public int importCardSelectionScenario(String cardSelectionScenario) {

    JsonObject jsonObject = JsonUtil.getParser().fromJson(cardSelectionScenario, JsonObject.class);

    // Basic fields
    multiSelectionProcessing =
        MultiSelectionProcessing.valueOf(jsonObject.get(MULTI_SELECTION_PROCESSING).getAsString());
    channelControl = ChannelControl.valueOf(jsonObject.get(CHANNEL_CONTROL).getAsString());

    // Card selectors
    List<String> cardSelectorsTypes =
        JsonUtil.getParser()
            .fromJson(
                jsonObject.get(CARD_SELECTORS_TYPES).getAsJsonArray(),
                new TypeToken<ArrayList<String>>() {}.getType());
    JsonArray cardSelectorsJsonArray = jsonObject.get(CARD_SELECTORS).getAsJsonArray();

    // Card selections
    List<String> cardSelectionsTypes =
        JsonUtil.getParser()
            .fromJson(
                jsonObject.get(CARD_SELECTIONS_TYPES).getAsJsonArray(),
                new TypeToken<ArrayList<String>>() {}.getType());
    JsonArray cardSelectionsJsonArray = jsonObject.get(CARD_SELECTIONS).getAsJsonArray();
    JsonArray defaultCardSelectionsJsonArray =
        jsonObject.get(DEFAULT_CARD_SELECTIONS).getAsJsonArray();

    // Clear the current list of card selectors and selections
    cardSelectors.clear();
    cardSelections.clear();
    cardSelectionRequests.clear();

    int index = 0;
    for (int i = 0; i < cardSelectorsTypes.size(); i++) {
      CardSelector<?> cardSelector;
      try {
        Class<?> classOfCardSelector = Class.forName(cardSelectorsTypes.get(i));
        cardSelector =
            (CardSelector<?>)
                JsonUtil.getParser().fromJson(cardSelectorsJsonArray.get(i), classOfCardSelector);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "Original CardSelector type '" + cardSelectorsTypes.get(i) + "' is not found", e);
      }
      CardSelectionExtension cardSelection;
      try {
        Class<?> classOfCardSelection = Class.forName(cardSelectionsTypes.get(i));
        // Original card selection
        cardSelection =
            (CardSelectionExtension)
                JsonUtil.getParser().fromJson(cardSelectionsJsonArray.get(i), classOfCardSelection);
      } catch (ClassNotFoundException e) {
        // Default card selection
        logger.warn(
            "Original CardSelection type '{}' is not found. Replaced by default type '{}' for deserialization",
            cardSelectionsTypes.get(i),
            CardSelectionAdapter.class.getName());
        cardSelection =
            JsonUtil.getParser()
                .fromJson(defaultCardSelectionsJsonArray.get(i), CardSelectionAdapter.class);
      }
      // Prepare selection
      index = prepareSelection(cardSelector, cardSelection);
    }
    return index;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardSelectionResult processCardSelectionScenario(CardReader reader) {

    Assert.getInstance().notNull(reader, "reader");

    // Communicate with the card to make the actual selection
    List<CardSelectionResponseApi> cardSelectionResponses;

    try {
      cardSelectionResponses =
          ((AbstractReaderAdapter) reader)
              .transmitCardSelectionRequests(
                  cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl);
    } catch (ReaderBrokenCommunicationException e) {
      throw new ReaderCommunicationException(e.getMessage(), e);
    } catch (CardBrokenCommunicationException e) {
      throw new CardCommunicationException(e.getMessage(), e);
    }

    // Analyze the received responses
    return processCardSelectionResponses(cardSelectionResponses);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void scheduleCardSelectionScenario(
      ObservableCardReader observableCardReader,
      ObservableCardReader.NotificationMode notificationMode) {

    Assert.getInstance()
        .notNull(observableCardReader, "observableCardReader")
        .notNull(notificationMode, "notificationMode");

    CardSelectionScenarioAdapter cardSelectionScenario =
        new CardSelectionScenarioAdapter(
            cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl);
    if (observableCardReader instanceof ObservableLocalReaderAdapter) {
      ((ObservableLocalReaderAdapter) observableCardReader)
          .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode);
    } else if (observableCardReader instanceof ObservableRemoteReaderAdapter) {
      ((ObservableRemoteReaderAdapter) observableCardReader)
          .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode);
    } else {
      throw new IllegalArgumentException(
          "Cannot cast 'observableCardReader' to ObservableLocalReaderAdapter or ObservableRemoteReaderAdapter. "
              + "Actual type: "
              + observableCardReader.getClass().getName());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
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
   * {@inheritDoc}
   *
   * @since 2.3.0
   */
  @Override
  public String exportProcessedCardSelectionScenario() {
    if (cardSelectionResponses == null) {
      throw new IllegalStateException(
          "The card selection scenario has not yet been processed or has failed");
    }
    return JsonUtil.toJson(cardSelectionResponses);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.3.0
   */
  @Override
  public CardSelectionResult importProcessedCardSelectionScenario(
      String processedCardSelectionScenario) {
    List<CardSelectionResponseApi> cardSelectionResponses;
    try {
      cardSelectionResponses =
          JsonUtil.getParser()
              .fromJson(
                  processedCardSelectionScenario,
                  new TypeToken<ArrayList<CardSelectionResponseApi>>() {}.getType());
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException(
          "Parameter 'processedCardSelectionScenario' has an invalid JSON synthax: "
              + processedCardSelectionScenario,
          e);
    }
    if (cardSelectionResponses == null) {
      throw new IllegalArgumentException(
          "Parameter 'processedCardSelectionScenario' is null or empty: "
              + processedCardSelectionScenario);
    }
    return processCardSelectionResponses(cardSelectionResponses);
  }

  /**
   * Analyzes the responses received in return of the execution of a card selection scenario and
   * returns the CardSelectionResult.
   *
   * @param cardSelectionResponses The card selection responses.
   * @return A not null reference.
   * @throws IllegalArgumentException If the list is null or empty.
   */
  private CardSelectionResult processCardSelectionResponses(
      List<CardSelectionResponseApi> cardSelectionResponses) {

    Assert.getInstance()
        .isInRange(
            cardSelectionResponses.size(), 1, cardSelections.size(), "cardSelectionResponses");

    CardSelectionResultAdapter cardSelectionsResult = new CardSelectionResultAdapter();
    int index = 0;
    for (CardSelectionResponseApi cardSelectionResponse : cardSelectionResponses) {
      if (cardSelectionResponse.hasMatched()) {
        // invoke the parse method defined by the card extension to retrieve the smart card
        SmartCard smartCard;
        try {
          smartCard = (SmartCard) cardSelections.get(index).parse(cardSelectionResponse);
        } catch (ParseException e) {
          throw new InvalidCardResponseException(
              "Failed to parse the card response: " + cardSelectionResponse, e);
        } catch (UnsupportedOperationException e) {
          logger.warn(
              "Unable to parse card selection responses due to missing card extensions in runtime environment");
          cardSelectionsResult = new CardSelectionResultAdapter(); // Empty result
          break;
        }
        cardSelectionsResult.addSmartCard(index, smartCard);
      }
      index++;
    }
    this.cardSelectionResponses = cardSelectionResponses;
    return cardSelectionsResult;
  }
}
