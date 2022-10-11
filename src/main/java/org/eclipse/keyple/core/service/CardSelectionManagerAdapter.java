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
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import org.calypsonet.terminal.card.*;
import org.calypsonet.terminal.card.spi.CardSelectionRequestSpi;
import org.calypsonet.terminal.card.spi.CardSelectionSpi;
import org.calypsonet.terminal.card.spi.ParseException;
import org.calypsonet.terminal.reader.CardCommunicationException;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.ReaderCommunicationException;
import org.calypsonet.terminal.reader.selection.CardSelectionManager;
import org.calypsonet.terminal.reader.selection.CardSelectionResult;
import org.calypsonet.terminal.reader.selection.InvalidCardResponseException;
import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse;
import org.calypsonet.terminal.reader.selection.spi.CardSelection;
import org.calypsonet.terminal.reader.selection.spi.SmartCard;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private) <br>
 * Implementation of the {@link CardSelectionManager}.
 *
 * @since 2.0.0
 */
final class CardSelectionManagerAdapter implements CardSelectionManager {

  private static final Logger logger = LoggerFactory.getLogger(CardSelectionManagerAdapter.class);
  private static final String DETECTION_MODE = "detectionMode";
  private static final String NOTIFICATION_MODE = "notificationMode";
  private static final String MULTI_SELECTION_PROCESSING = "multiSelectionProcessing";
  private static final String CHANNEL_CONTROL = "channelControl";
  private static final String CARD_SELECTIONS_TYPES = "cardSelectionsTypes";
  private static final String CARD_SELECTIONS = "cardSelections";
  private static final String DEFAULT_CARD_SELECTIONS = "defaultCardSelections";

  private final List<CardSelectionSpi> cardSelections;
  private final List<CardSelectionRequestSpi> cardSelectionRequests;
  private MultiSelectionProcessing multiSelectionProcessing;
  private ChannelControl channelControl = ChannelControl.KEEP_OPEN;
  private ObservableCardReader.DetectionMode detectionMode =
      ObservableCardReader.DetectionMode.REPEATING;
  private ObservableCardReader.NotificationMode notificationMode =
      ObservableCardReader.NotificationMode.ALWAYS;

  /**
   * (package-private) <br>
   * Creates an instance of the service with which the selection stops as soon as a card matches a
   * selection case.
   *
   * @since 2.0.0
   */
  CardSelectionManagerAdapter() {
    multiSelectionProcessing = MultiSelectionProcessing.FIRST_MATCH;
    cardSelections = new ArrayList<CardSelectionSpi>();
    cardSelectionRequests = new ArrayList<CardSelectionRequestSpi>();
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
  public int prepareSelection(CardSelection cardSelection) {

    Assert.getInstance().notNull(cardSelection, "cardSelection");

    /* keep the selection request */
    cardSelections.add((CardSelectionSpi) cardSelection);
    cardSelectionRequests.add(((CardSelectionSpi) cardSelection).getCardSelectionRequest());
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
  public String exportCardSelectionScenario(
      ObservableCardReader.DetectionMode detectionMode,
      ObservableCardReader.NotificationMode notificationMode) {

    Assert.getInstance()
        .notNull(detectionMode, DETECTION_MODE)
        .notNull(notificationMode, NOTIFICATION_MODE);

    JsonObject jsonObject = new JsonObject();

    // Basic fields
    jsonObject.addProperty(MULTI_SELECTION_PROCESSING, multiSelectionProcessing.name());
    jsonObject.addProperty(CHANNEL_CONTROL, channelControl.name());
    jsonObject.addProperty(DETECTION_MODE, detectionMode.name());
    jsonObject.addProperty(NOTIFICATION_MODE, notificationMode.name());

    // Original card selections
    List<String> cardSelectionsTypes = new ArrayList<String>(cardSelections.size());
    for (CardSelectionSpi cardSelection : cardSelections) {
      cardSelectionsTypes.add(cardSelection.getClass().getName());
    }
    jsonObject.add(CARD_SELECTIONS_TYPES, JsonUtil.getParser().toJsonTree(cardSelectionsTypes));
    jsonObject.add(CARD_SELECTIONS, JsonUtil.getParser().toJsonTree(cardSelections));

    // Default card selections
    List<CardSelectionAdapter> defaultCardSelections =
        new ArrayList<CardSelectionAdapter>(cardSelections.size());
    for (CardSelectionSpi cardSelection : cardSelections) {
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
    detectionMode =
        ObservableCardReader.DetectionMode.valueOf(jsonObject.get(DETECTION_MODE).getAsString());
    notificationMode =
        ObservableCardReader.NotificationMode.valueOf(
            jsonObject.get(NOTIFICATION_MODE).getAsString());

    // Card selections
    List<String> cardSelectionsTypes =
        JsonUtil.getParser()
            .fromJson(
                jsonObject.get(CARD_SELECTIONS_TYPES).getAsJsonArray(),
                new TypeToken<ArrayList<String>>() {}.getType());
    JsonArray cardSelectionsJsonArray = jsonObject.get(CARD_SELECTIONS).getAsJsonArray();
    JsonArray defaultCardSelectionsJsonArray =
        jsonObject.get(DEFAULT_CARD_SELECTIONS).getAsJsonArray();

    // Clear the current list of card selections
    cardSelections.clear();
    cardSelectionRequests.clear();

    int index = 0;
    for (int i = 0; i < cardSelectionsTypes.size(); i++) {
      CardSelection cardSelection;
      try {
        Class<?> classOfCardSelection = Class.forName(cardSelectionsTypes.get(i));
        // Original card selection
        cardSelection =
            (CardSelection)
                JsonUtil.getParser().fromJson(cardSelectionsJsonArray.get(i), classOfCardSelection);
      } catch (ClassNotFoundException e) {
        // Default card selection
        logger.warn(
            "Original CardSelection type '{}' not found. Use default type '{}' for deserialization.",
            cardSelectionsTypes.get(i),
            CardSelectionAdapter.class.getName());
        cardSelection =
            JsonUtil.getParser()
                .fromJson(defaultCardSelectionsJsonArray.get(i), CardSelectionAdapter.class);
      }
      // Prepare selection
      index = prepareSelection(cardSelection);
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
                  cardSelectionRequests, multiSelectionProcessing, channelControl);
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
      ObservableCardReader.DetectionMode detectionMode,
      ObservableCardReader.NotificationMode notificationMode) {

    Assert.getInstance()
        .notNull(observableCardReader, "observableCardReader")
        .notNull(detectionMode, DETECTION_MODE)
        .notNull(notificationMode, NOTIFICATION_MODE);

    CardSelectionScenarioAdapter cardSelectionScenario =
        new CardSelectionScenarioAdapter(
            cardSelectionRequests, multiSelectionProcessing, channelControl);
    if (observableCardReader instanceof ObservableLocalReaderAdapter) {
      ((ObservableLocalReaderAdapter) observableCardReader)
          .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode, detectionMode);
    } else if (observableCardReader instanceof ObservableRemoteReaderAdapter) {
      ((ObservableRemoteReaderAdapter) observableCardReader)
          .scheduleCardSelectionScenario(cardSelectionScenario, notificationMode, detectionMode);
    } else {
      throw new IllegalArgumentException("Not a Keyple reader implementation.");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.1
   */
  @Override
  public void scheduleCardSelectionScenario(ObservableCardReader observableCardReader) {
    scheduleCardSelectionScenario(observableCardReader, detectionMode, notificationMode);
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
   * (private)<br>
   * Analyzes the responses received in return of the execution of a card selection scenario and
   * returns the CardSelectionResult.
   *
   * @param cardSelectionResponses The card selection responses.
   * @return A not null reference.
   * @throws IllegalArgumentException If the list is null or empty.
   */
  private CardSelectionResult processCardSelectionResponses(
      List<CardSelectionResponseApi> cardSelectionResponses) {

    Assert.getInstance().notEmpty(cardSelectionResponses, "cardSelectionResponses");

    CardSelectionResultAdapter cardSelectionsResult = new CardSelectionResultAdapter();

    int index = 0;

    /* Check card responses */
    for (CardSelectionResponseApi cardSelectionResponse : cardSelectionResponses) {
      if (cardSelectionResponse.hasMatched()) {
        // invoke the parse method defined by the card extension to retrieve the smart card
        SmartCard smartCard;
        try {
          smartCard = (SmartCard) cardSelections.get(index).parse(cardSelectionResponse);
        } catch (ParseException e) {
          throw new InvalidCardResponseException(
              "Error occurred while parsing the card response: " + e.getMessage(), e);
        }
        cardSelectionsResult.addSmartCard(index, smartCard);
      }
      index++;
    }
    return cardSelectionsResult;
  }
}
