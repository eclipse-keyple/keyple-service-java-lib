/* **************************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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

import static org.eclipse.keyple.core.service.DistributedUtilAdapter.JsonProperty.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.card.AbstractApduException;
import org.eclipse.keypop.card.CardResponseApi;
import org.eclipse.keypop.card.ChannelControl;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse;

/**
 * Contains all JSON adapters used for serialization and deserialization processes.<br>
 * These adapters are required for interfaces and abstract classes.
 *
 * @since 2.1.1
 */
final class JsonAdapter {

  private JsonAdapter() {}

  /**
   * Loads the class having the provided name, without initializing it, and checks that it is a
   * subtype of the expected type.
   *
   * <p>Used when the class name is read from JSON data.
   *
   * @param className The fully qualified name of the class.
   * @param expectedType The expected super type.
   * @param <T> The expected super type.
   * @return A not null reference.
   * @throws ClassNotFoundException If the class is not found.
   * @throws IllegalArgumentException If the class is not a subtype of the expected type.
   * @since 3.5.0
   */
  static <T> Class<? extends T> loadSubclassOf(String className, Class<T> expectedType)
      throws ClassNotFoundException {
    Class<?> clazz = Class.forName(className, false, JsonAdapter.class.getClassLoader());
    if (!expectedType.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(
          "Class '" + className + "' is not a subtype of '" + expectedType.getName() + "'");
    }
    return clazz.asSubclass(expectedType);
  }

  /**
   * Gets the card selector class having the provided name.
   *
   * <p>Only the card selectors provided by the service are supported.
   *
   * @param className The fully qualified name of the class.
   * @return A not null reference.
   * @throws IllegalArgumentException If the class is not a card selector provided by the service.
   * @since 3.5.0
   */
  @SuppressWarnings("rawtypes")
  static Class<? extends CardSelector> getCardSelectorClass(String className) {
    if (BasicCardSelectorAdapter.class.getName().equals(className)) {
      return BasicCardSelectorAdapter.class;
    }
    if (IsoCardSelectorAdapter.class.getName().equals(className)) {
      return IsoCardSelectorAdapter.class;
    }
    throw new IllegalArgumentException("CardSelector type '" + className + "' is not supported");
  }

  /**
   * Loads the class having the provided name, without initializing it, and checks that it belongs
   * to a card extension supported by the service and is a subtype of the expected types.
   *
   * <p>Used when the class name is read from JSON data.
   *
   * @param className The fully qualified name of the class.
   * @param expectedType The expected super type.
   * @param expectedSpiType The expected SPI super type.
   * @param <T> The expected super type.
   * @return A not null reference.
   * @throws ClassNotFoundException If the class is not found or does not belong to a supported card
   *     extension.
   * @throws IllegalArgumentException If the class is not a subtype of the expected types.
   * @since 3.5.0
   */
  static <T> Class<? extends T> loadCardExtensionClass(
      String className, Class<T> expectedType, Class<?> expectedSpiType)
      throws ClassNotFoundException {
    if (!SmartCardServiceAdapter.getInstance().isCardExtensionClass(className)) {
      throw new ClassNotFoundException(
          "Class '" + className + "' does not belong to a supported card extension");
    }
    Class<? extends T> clazz = loadSubclassOf(className, expectedType);
    if (!expectedSpiType.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(
          "Class '" + className + "' is not a subtype of '" + expectedSpiType.getName() + "'");
    }
    return clazz;
  }

  /**
   * Serializer/De-serializer of a {@link AbstractApduException}.
   *
   * @since 2.0.0
   */
  static final class ApduExceptionJsonAdapter
      implements JsonSerializer<AbstractApduException>, JsonDeserializer<AbstractApduException> {

    /**
     * {@inheritDoc}
     *
     * @since 2.0.0
     */
    @Override
    public JsonElement serialize(
        AbstractApduException exception,
        Type type,
        JsonSerializationContext jsonSerializationContext) {

      JsonObject json = new JsonObject();
      json.addProperty("detailMessage", exception.getMessage());
      json.addProperty("isCardResponseComplete", exception.isCardResponseComplete());
      json.add("cardResponseApi", jsonSerializationContext.serialize(exception.getCardResponse()));
      return json;
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.2.1
     */
    @Override
    public AbstractApduException deserialize(
        JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
        throws JsonParseException {

      JsonObject jsonObject = (JsonObject) jsonElement;

      String message = jsonObject.get("detailMessage").getAsString();
      boolean isCardResponseComplete = jsonObject.get("isCardResponseComplete").getAsBoolean();
      CardResponseApi cardResponseApi =
          jsonDeserializationContext.deserialize(
              jsonObject.get("cardResponseApi").getAsJsonObject(), CardResponseApi.class);

      Class<? extends AbstractApduException> exceptionClass;
      try {
        exceptionClass = (Class<? extends AbstractApduException>) Class.forName(type.getTypeName());
      } catch (ClassNotFoundException e) {
        throw new JsonParseException(
            "Exception '"
                + type
                + "' is not found in runtime environment. Original message: "
                + message);
      }

      try {
        Constructor<? extends AbstractApduException> constructor =
            exceptionClass.getConstructor(CardResponseApi.class, boolean.class, String.class);

        return constructor.newInstance(cardResponseApi, isCardResponseComplete, message);

      } catch (NoSuchMethodException e) {
        throw new JsonParseException(
            "No valid constructor found for exception '" + type + "' with message: " + message, e);
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        throw new JsonParseException(
            "Failed to build exception '" + type + "' with message: " + message, e);
      }
    }
  }

  /**
   * JSON deserializer of a {@link ScheduledCardSelectionsResponse}.
   *
   * @since 3.2.3
   */
  static final class ScheduledCardSelectionsResponseJsonDeserializerAdapter
      implements JsonDeserializer<ScheduledCardSelectionsResponseAdapter> {

    /**
     * {@inheritDoc}
     *
     * @since 3.2.3
     */
    @Override
    public ScheduledCardSelectionsResponseAdapter deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return context.deserialize(json, ScheduledCardSelectionsResponseAdapter.class);
    }
  }

  /**
   * JSON deserializer of a {@link org.eclipse.keypop.card.CardSelectionResponseApi}.
   *
   * @since 2.3.0
   */
  static final class CardSelectionResponseApiJsonDeserializerAdapter
      implements JsonDeserializer<CardSelectionResponseAdapter> {

    /**
     * {@inheritDoc}
     *
     * @since 2.3.0
     */
    @Override
    public CardSelectionResponseAdapter deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return context.deserialize(json, CardSelectionResponseAdapter.class);
    }
  }

  /**
   * JSON deserializer of a {@link org.eclipse.keypop.card.CardResponseApi}.
   *
   * @since 2.3.0
   */
  static final class CardResponseApiJsonDeserializerAdapter
      implements JsonDeserializer<CardResponseAdapter> {

    /**
     * {@inheritDoc}
     *
     * @since 2.3.0
     */
    @Override
    public CardResponseAdapter deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return context.deserialize(json, CardResponseAdapter.class);
    }
  }

  /**
   * JSON serializer/deserializer of a {@link org.eclipse.keypop.card.ApduResponseApi}.
   *
   * @since 2.0.0
   */
  static final class ApduResponseApiJsonAdapter
      implements JsonSerializer<ApduResponseAdapter>, JsonDeserializer<ApduResponseAdapter> {

    /**
     * {@inheritDoc}
     *
     * @since 2.0.2
     */
    @Override
    public JsonElement serialize(
        ApduResponseAdapter src, Type typeOfSrc, JsonSerializationContext context) {
      return context.serialize(src);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0.0
     */
    @Override
    public ApduResponseAdapter deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return context.deserialize(json, ApduResponseAdapter.class);
    }
  }

  /**
   * JSON serializer/deserializer of a {@link CardSelectionScenarioAdapter}.
   *
   * @since 3.3.5
   */
  static final class CardSelectionScenarioAdapterJsonAdapter
      implements JsonSerializer<CardSelectionScenarioAdapter>,
          JsonDeserializer<CardSelectionScenarioAdapter> {

    /**
     * {@inheritDoc}
     *
     * @since 3.3.5
     */
    @Override
    public JsonElement serialize(
        CardSelectionScenarioAdapter src, Type typeOfSrc, JsonSerializationContext context) {

      JsonObject jsonObject = new JsonObject();

      // Basic fields
      jsonObject.addProperty(
          MULTI_SELECTION_PROCESSING.getKey(), src.getMultiSelectionProcessing().name());
      jsonObject.addProperty(CHANNEL_CONTROL.getKey(), src.getChannelControl().name());

      // Original card selectors
      List<String> cardSelectorsTypes = new ArrayList<>(src.getCardSelectors().size());
      for (CardSelector<?> cardSelector : src.getCardSelectors()) {
        cardSelectorsTypes.add(cardSelector.getClass().getName());
      }
      jsonObject.add(CARD_SELECTORS_TYPES.getKey(), context.serialize(cardSelectorsTypes));
      jsonObject.add(CARD_SELECTORS.getKey(), context.serialize(src.getCardSelectors()));

      // Card selection requests
      jsonObject.add(
          CARD_SELECTION_REQUESTS.getKey(), context.serialize(src.getCardSelectionRequests()));

      return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.3.5
     */
    @Override
    public CardSelectionScenarioAdapter deserialize(
        JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {

      JsonObject jsonObject = (JsonObject) jsonElement;

      // Basic fields
      MultiSelectionProcessing multiSelectionProcessing =
          MultiSelectionProcessing.valueOf(
              jsonObject.get(MULTI_SELECTION_PROCESSING.getKey()).getAsString());

      ChannelControl channelControl =
          ChannelControl.valueOf(jsonObject.get(CHANNEL_CONTROL.getKey()).getAsString());

      // Card selectors
      List<String> cardSelectorsTypes =
          JsonUtil.getParser()
              .fromJson(
                  jsonObject.get(CARD_SELECTORS_TYPES.getKey()).getAsJsonArray(),
                  new TypeToken<ArrayList<String>>() {}.getType());

      JsonArray cardSelectorsJsonArray = jsonObject.get(CARD_SELECTORS.getKey()).getAsJsonArray();

      List<CardSelector<?>> cardSelectors = new ArrayList<>(cardSelectorsTypes.size());
      for (int i = 0; i < cardSelectorsTypes.size(); i++) {
        cardSelectors.add(
            JsonUtil.getParser()
                .fromJson(
                    cardSelectorsJsonArray.get(i),
                    getCardSelectorClass(cardSelectorsTypes.get(i))));
      }

      // Card selection requests
      List<CardSelectionRequestSpi> cardSelectionRequests =
          JsonUtil.getParser()
              .fromJson(
                  jsonObject.get(CARD_SELECTION_REQUESTS.getKey()).getAsJsonArray(),
                  new TypeToken<ArrayList<InternalDto.CardSelectionRequest>>() {}.getType());

      return new CardSelectionScenarioAdapter(
          cardSelectors, cardSelectionRequests, multiSelectionProcessing, channelControl);
    }
  }
}
