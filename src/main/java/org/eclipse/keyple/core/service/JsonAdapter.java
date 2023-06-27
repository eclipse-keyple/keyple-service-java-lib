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
import java.lang.reflect.Type;
import org.eclipse.keypop.card.AbstractApduException;

/**
 * Contains all JSON adapters used for serialization and deserialization processes.<br>
 * These adapters are required for interfaces and abstract classes.
 *
 * @since 2.1.1
 */
final class JsonAdapter {

  /**
   * Serializer of a {@link AbstractApduException}.
   *
   * <p>Only the field "message" is serialized during the process.
   *
   * @since 2.0.0
   */
  static final class ApduExceptionJsonSerializerAdapter
      implements JsonSerializer<AbstractApduException> {

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
      json.add("cardResponse", jsonSerializationContext.serialize(exception.getCardResponse()));
      return json;
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
