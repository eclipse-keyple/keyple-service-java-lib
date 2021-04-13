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

import static org.eclipse.keyple.core.service.DistributedLocalServiceAdapter.JsonProperty;

import com.google.gson.JsonObject;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;

/**
 * (package-private)<br>
 * Utility class of distributed components.
 *
 * @since 2.0
 */
final class DistributedUtilAdapter {

  /**
   * (private)<br>
   * Constructor.
   */
  private DistributedUtilAdapter() {}

  /**
   * (package-private)<br>
   * Executes remotely the provided JSON input data of a specific plugin service, parses the provide
   * JSON output data, checks if the JSON contains an error and throws the embedded exception if
   * exists..
   *
   * @param input The JSON input data to process.
   * @param remotePluginSpi The SPI in charge of carrying out the treatment.
   * @param pluginName The name of the remote plugin.
   * @param logger The logger to use for logging.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   * @since 2.0
   */
  static JsonObject executePluginServiceRemotely(
      JsonObject input, RemotePluginSpi remotePluginSpi, String pluginName, Logger logger)
      throws Exception { // NOSONAR

    if (logger.isDebugEnabled()) {
      logger.debug("The plugin '{}' is sending the following JSON data : {}", pluginName, input);
    }

    String outputJson = remotePluginSpi.executeRemotely(input.toString());

    if (logger.isDebugEnabled()) {
      logger.debug("The plugin '{}' received the following JSON data : {}", pluginName, outputJson);
    }

    return getJsonObject(outputJson);
  }

  /**
   * (package-private)<br>
   * Executes remotely the provided JSON input data of a specific reader service, parses the provide
   * JSON output data, checks if the JSON contains an error and throws the embedded exception if
   * exists.
   *
   * @param input The JSON input data to process.
   * @param remoteReaderSpi The SPI in charge of carrying out the treatment.
   * @param readerName The name of the remote reader.
   * @param pluginName The name of the remote plugin.
   * @param logger The logger to use for logging.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   * @since 2.0
   */
  static JsonObject executeReaderServiceRemotely(
      JsonObject input,
      RemoteReaderSpi remoteReaderSpi,
      String readerName,
      String pluginName,
      Logger logger)
      throws Exception { // NOSONAR

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The reader '{}' of plugin '{}' is sending the following JSON data : {}",
          readerName,
          pluginName,
          input);
    }

    String outputJson = remoteReaderSpi.executeRemotely(input.toString());

    if (logger.isDebugEnabled()) {
      logger.debug(
          "The reader '{}' of plugin '{}' received the following JSON data : {}",
          readerName,
          pluginName,
          outputJson);
    }

    return getJsonObject(outputJson);
  }

  /**
   * Parses the provided JSON output data, checks if the JSON contains an error and throws the
   * embedded exception if exists.
   *
   * @param outputJson The JSON to parse.
   * @return The JSON output data, or null if returned data are null or empty.
   * @throws Exception The embedded exception if exists.
   * @since 2.0
   */
  private static JsonObject getJsonObject(String outputJson) throws Exception { // NOSONAR

    if (outputJson == null || outputJson.isEmpty()) {
      return null;
    }

    JsonObject output = JsonUtil.getParser().fromJson(outputJson, JsonObject.class);
    if (output.has(JsonProperty.ERROR.name())) {
      BodyError body =
          JsonUtil.getParser()
              .fromJson(output.get(JsonProperty.ERROR.name()).getAsString(), BodyError.class);
      throw body.getException();
    }
    return output;
  }

  /**
   * (package-private)<br>
   * Throws a runtime exception containing the provided exception.
   *
   * @param e The cause.
   * @throws RuntimeException The thrown runtime exception.
   * @since 2.0
   */
  static void throwRuntimeException(Exception e) {
    throw new RuntimeException( // NOSONAR
        String.format(
            "The distributed message sender received an unexpected error : %s", e.getMessage()),
        e);
  }
}
