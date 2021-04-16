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
import org.eclipse.keyple.core.card.ApduRequest;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * (package-private)<br>
 * Serializer/Deserializer of a {@link ApduRequest}.
 *
 * @since 2.0
 */
class ApduRequestJsonAdapter implements JsonSerializer<ApduRequest>, JsonDeserializer<ApduRequest> {

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
    Set<String> successfulStatusCodes = new HashSet<String>();
    for (int code : apduRequest.getSuccessfulStatusCodes()) {
      successfulStatusCodes.add(Integer.toHexString(code).toUpperCase());
    }
    output.add("successfulStatusCodes", jsonSerializationContext.serialize(successfulStatusCodes));
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

    byte[] bytes = ByteArrayUtil.fromHex(jsonElement.getAsJsonObject().get("bytes").getAsString());
    boolean isCase4 = jsonElement.getAsJsonObject().get("isCase4").getAsBoolean();
    String name = jsonElement.getAsJsonObject().get("name").getAsString();

    ApduRequest apduRequest = new ApduRequest(bytes, isCase4).setName(name);

    Set<String> successfulStatusCodes =
        JsonUtil.getParser()
            .fromJson(
                jsonElement.getAsJsonObject().get("successfulStatusCodes").getAsJsonArray(),
                new TypeToken<Set<String>>() {}.getType());

    for (String successfulStatusCode : successfulStatusCodes) {
      apduRequest.addSuccessfulStatusCode(Integer.parseInt(successfulStatusCode, 16));
    }

    return apduRequest;
  }
}
