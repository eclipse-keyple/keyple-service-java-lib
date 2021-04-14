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
import org.eclipse.keyple.core.card.ApduRequest;
import org.eclipse.keyple.core.util.ByteArrayUtil;

/**
 * (package-private)<br>
 * Serializer/Deserializer of a {@link ApduRequest}.
 *
 * @since 2.0
 */
class ApduRequestJsonAdapter
    implements JsonSerializer<ApduRequest>, JsonDeserializer<ApduRequest> {

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public JsonElement serialize(
      ApduRequest apduRequest, Type type, JsonSerializationContext jsonSerializationContext) {

    JsonObject output = new JsonObject();
    output.addProperty("bytes", ByteArrayUtil.toHex(apduRequest.getBytes()));
    output.addProperty("isCase4", apduRequest.isCase4());
    StringBuilder successfulStatusCodesSb = new StringBuilder();
    for (int code : apduRequest.getSuccessfulStatusCodes()) {
      if (successfulStatusCodesSb.length() == 0) {
        successfulStatusCodesSb.append(Integer.toHexString(code));
      } else {
        successfulStatusCodesSb.append(", ").append(Integer.toHexString(code));
      }
    }
    output.addProperty("successfulStatusCodes", successfulStatusCodesSb.toString());
    output.addProperty("name", apduRequest.getName());
    return output;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public ApduRequest deserialize(
      JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {

    String className = jsonElement.getAsJsonObject().get("class").getAsString();
    String name = jsonElement.getAsJsonObject().get("name").getAsString();

    //    try {
    //      return (ApduRequest) Enum.valueOf((Class<? extends Enum>) Class.forName(className),
    // name);
    //    } catch (ClassNotFoundException e) {
    //      throw new JsonParseException(
    //          "Can not parse jsonElement as a CardCommand " + jsonElement.toString());
    //    }
    return null;
  }
}
