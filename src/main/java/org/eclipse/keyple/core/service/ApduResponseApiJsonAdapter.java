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

import com.google.gson.*;
import java.lang.reflect.Type;

/**
 * (package-private)<br>
 * JSON serializer/deserializer of a {@link org.calypsonet.terminal.card.ApduResponseApi}.
 *
 * <p>It is necessary to define a serializer because the type of the associated object registered is
 * an interface, and therefore has no fields. Gson will then use its default reflexivity mechanism
 * to serialize the object only if explicitly requested in the "serialize" method. This is only
 * necessary for serializations of object trees.
 *
 * @since 2.0.0
 */
final class ApduResponseApiJsonAdapter
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
