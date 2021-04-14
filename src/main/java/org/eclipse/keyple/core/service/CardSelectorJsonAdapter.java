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
import java.lang.reflect.Type;
import org.eclipse.keyple.core.service.selection.CardSelector;
import org.eclipse.keyple.core.util.ByteArrayUtil;

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
    output.addProperty("aid", ByteArrayUtil.toHex(cardSelector.getAid()));
    output.addProperty("fileOccurrence", cardSelector.getFileOccurrence().name());
    output.addProperty("fileControlInformation", cardSelector.getFileControlInformation().name());
    StringBuilder successfulStatusCodesSb = new StringBuilder();
    for (int code : cardSelector.getSuccessfulSelectionStatusCodes()) {
      if (successfulStatusCodesSb.length() == 0) {
        successfulStatusCodesSb.append(Integer.toHexString(code));
      } else {
        successfulStatusCodesSb.append(", ").append(Integer.toHexString(code));
      }
    }
    output.addProperty("successfulSelectionStatusCodes", successfulStatusCodesSb.toString());
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
    // TODO implement the deserialization
    return null;
  }
}
