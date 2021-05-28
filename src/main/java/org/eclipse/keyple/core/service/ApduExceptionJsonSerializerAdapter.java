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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.calypsonet.terminal.card.AbstractApduException;
import org.calypsonet.terminal.card.CardResponseApi;

/**
 * (package-private)<br>
 * Serializer of a {@link AbstractApduException}.
 *
 * <p>Only the field "message" is serialized during the process.
 *
 * @since 2.0
 */
class ApduExceptionJsonSerializerAdapter implements JsonSerializer<AbstractApduException> {

  /**
   * (package-private)<br>
   * Creates an instance.
   *
   * @since 2.0
   */
  ApduExceptionJsonSerializerAdapter() {}

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public JsonElement serialize(
      AbstractApduException exception,
      Type type,
      JsonSerializationContext jsonSerializationContext) {

    JsonObject json = new JsonObject();
    json.addProperty("message", exception.getMessage());
    json.add(
        "cardResponse",
        jsonSerializationContext.serialize(exception.getCardResponse(), CardResponseApi.class));
    return json;
  }
}
