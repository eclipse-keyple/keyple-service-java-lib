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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import org.eclipse.keypop.card.AbstractApduException;
import org.eclipse.keypop.card.CardResponseApi;

/**
 * Contains all JSON adapters used for serialization and deserialization processes.<br>
 * These adapters are required for interfaces and abstract classes.
 *
 * @since 2.1.1
 */
final class JsonAdapter {

  private JsonAdapter() {}

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
            String.format(
                "Exception [%s] not founded in runtime environment. Original message: %s",
                type, message));
      }

      try {
        Constructor<? extends AbstractApduException> constructor =
            exceptionClass.getConstructor(CardResponseApi.class, boolean.class, String.class);

        return constructor.newInstance(cardResponseApi, isCardResponseComplete, message);

      } catch (NoSuchMethodException e) {
        throw new JsonParseException(
            String.format(
                "No valid constructor found for exception [%s] with message [%s]", type, message));
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        throw new JsonParseException(
            String.format(
                "Error while trying to build exception [%s] with message [%s]", type, message));
      }
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
}
