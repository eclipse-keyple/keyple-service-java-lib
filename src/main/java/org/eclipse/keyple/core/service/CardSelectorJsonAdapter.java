/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.keyple.core.service.selection.CardSelector;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * (package-private)<br>
 * Serializer/Deserializer of a {@link CardSelector}.
 *
 * @since 2.0
 */
public class CardSelectorJsonAdapter
    implements JsonSerializer<CardSelector>, JsonDeserializer<CardSelector> {

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public JsonElement serialize(
      CardSelector cardSelector, Type type, JsonSerializationContext jsonSerializationContext) {

    JsonObject output = new JsonObject();

    output.addProperty("cardProtocol", cardSelector.getCardProtocol());
    output.addProperty("atrRegex", cardSelector.getAtrRegex());
    output.addProperty("aid", ByteArrayUtil.toHex(cardSelector.getAid()));
    output.addProperty("fileOccurrence", cardSelector.getFileOccurrence().name());
    output.addProperty("fileControlInformation", cardSelector.getFileControlInformation().name());
    Set<String> successfulStatusCodes = new HashSet<String>();
    for (int code : cardSelector.getSuccessfulSelectionStatusCodes()) {
      successfulStatusCodes.add(Integer.toHexString(code).toUpperCase());
    }
    output.add(
        "successfulSelectionStatusCodes",
        jsonSerializationContext.serialize(successfulStatusCodes));

    return output;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public CardSelector deserialize(
      JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {

    String cardProtocol = jsonElement.getAsJsonObject().get("cardProtocol").getAsString();
    String atrRegex = jsonElement.getAsJsonObject().get("atrRegex").getAsString();
    String aid = jsonElement.getAsJsonObject().get("aid").getAsString();
    CardSelector.FileOccurrence fileOccurrence =
        CardSelector.FileOccurrence.valueOf(
            jsonElement.getAsJsonObject().get("fileOccurrence").getAsString());
    CardSelector.FileControlInformation fileControlInformation =
        CardSelector.FileControlInformation.valueOf(
            jsonElement.getAsJsonObject().get("fileControlInformation").getAsString());

    CardSelector cardSelector =
        CardSelector.builder()
            .filterByCardProtocol(cardProtocol)
            .filterByAtr(atrRegex)
            .filterByDfName(aid)
            .setFileOccurrence(fileOccurrence)
            .setFileControlInformation(fileControlInformation)
            .build();

    Set<String> successfulStatusCodes =
        JsonUtil.getParser()
            .fromJson(
                jsonElement
                    .getAsJsonObject()
                    .get("successfulSelectionStatusCodes")
                    .getAsJsonArray(),
                new TypeToken<Set<String>>() {}.getType());

    for (String successfulStatusCode : successfulStatusCodes) {
      cardSelector.addSuccessfulStatusCode(Integer.parseInt(successfulStatusCode, 16));
    }

    return cardSelector;
  }
}
