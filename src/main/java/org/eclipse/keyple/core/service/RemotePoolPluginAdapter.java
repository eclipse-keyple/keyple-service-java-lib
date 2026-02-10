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

import static org.eclipse.keyple.core.service.DistributedUtilAdapter.*;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.SortedSet;
import java.util.TreeSet;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePoolPluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a remote {@link PoolPlugin}.
 *
 * @since 2.0.0
 */
final class RemotePoolPluginAdapter extends AbstractPluginAdapter implements PoolPlugin {

  private static final Logger logger = LoggerFactory.getLogger(RemotePoolPluginAdapter.class);

  private final RemotePoolPluginSpi remotePoolPluginSpi;

  /**
   * Constructor.
   *
   * @param remotePoolPluginSpi The associated SPI.
   * @since 2.0.0
   */
  RemotePoolPluginAdapter(RemotePoolPluginSpi remotePoolPluginSpi) {
    super(remotePoolPluginSpi.getName(), (KeyplePluginExtension) remotePoolPluginSpi);
    this.remotePoolPluginSpi = remotePoolPluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  void register() throws PluginIOException {
    super.register();
    int distributedApiLevel = remotePoolPluginSpi.exchangeApiLevel(CORE_API_LEVEL);
    logger.info(
        "[plugin={}] Registering distributed remote pool plugin [coreApiLevel={}, remotePluginApiLevel={}]",
        getName(),
        CORE_API_LEVEL,
        distributedApiLevel);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  void unregister() {
    try {
      remotePoolPluginSpi.onUnregister();
    } catch (Exception e) {
      logger.warn(
          "[plugin={}] Failed to unregister plugin extension [reason={}]",
          getName(),
          e.getMessage());
    }
    super.unregister();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public SortedSet<String> getReaderGroupReferences() {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
    input.addProperty(
        JsonProperty.SERVICE.getKey(), PluginService.GET_READER_GROUP_REFERENCES.name());

    // Execute the remote service.
    try {
      JsonObject output =
          executePluginServiceRemotely(input, remotePoolPluginSpi, getName(), logger);

      return JsonUtil.getParser()
          .fromJson(
              output.getAsJsonArray(JsonProperty.RESULT.getKey()).toString(),
              new TypeToken<SortedSet<String>>() {}.getType());

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return new TreeSet<>();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardReader allocateReader(String readerGroupReference) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "[plugin={}] Allocating reader [readerGroupReference={}]",
          getName(),
          readerGroupReference);
    }
    Assert.getInstance().notEmpty(readerGroupReference, "readerGroupReference");

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
    input.addProperty(JsonProperty.SERVICE.getKey(), PluginService.ALLOCATE_READER.name());

    JsonObject params = new JsonObject();
    params.addProperty(JsonProperty.READER_GROUP_REFERENCE.getKey(), readerGroupReference);

    input.add(JsonProperty.PARAMETERS.getKey(), params);

    // Execute the remote service.
    String localReaderName;
    String remoteReaderName;
    SmartCard selectedSmartCard = null;
    try {
      JsonObject output =
          executePluginServiceRemotely(input, remotePoolPluginSpi, getName(), logger);

      output = output.get(JsonProperty.RESULT.getKey()).getAsJsonObject();

      localReaderName = output.get(JsonProperty.READER_NAME.getKey()).getAsString();
      remoteReaderName = localReaderName + REMOTE_READER_NAME_SUFFIX;

      if (output.has(JsonProperty.SELECTED_SMART_CARD.getKey())) {
        String selectedSmartCardJson =
            output.getAsJsonObject(JsonProperty.SELECTED_SMART_CARD.getKey()).toString();
        String selectedSmartCardClassName =
            output.get(JsonProperty.SELECTED_SMART_CARD_CLASS_NAME.getKey()).getAsString();
        try {
          Class<?> classOfSelectedSmartCard = Class.forName(selectedSmartCardClassName);
          selectedSmartCard =
              (SmartCard)
                  JsonUtil.getParser().fromJson(selectedSmartCardJson, classOfSelectedSmartCard);
        } catch (ClassNotFoundException e) {
          logger.error(
              "[plugin={}] Class not found [className={}]",
              getName(),
              selectedSmartCardClassName,
              e);
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
      return null;
    }

    // Build a remote reader and register it.
    RemoteReaderSpi remoteReaderSpi =
        remotePoolPluginSpi.createRemoteReader(remoteReaderName, localReaderName);
    RemoteReaderAdapter remoteReaderAdapter =
        new RemoteReaderAdapter(remoteReaderSpi, getName(), selectedSmartCard, CORE_API_LEVEL);

    getReadersMap().put(remoteReaderSpi.getName(), remoteReaderAdapter);
    remoteReaderAdapter.register();
    return remoteReaderAdapter;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.2.0
   */
  @Override
  public SmartCard getSelectedSmartCard(CardReader reader) {
    Assert.getInstance().notNull(reader, "reader");
    RemoteReaderAdapter remoteReader = (RemoteReaderAdapter) getReader(reader.getName());
    return remoteReader != null ? remoteReader.getSelectedSmartCard() : null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void releaseReader(CardReader reader) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "[plugin={}] Releasing reader [name={}]",
          getName(),
          reader != null ? reader.getName() : null);
    }
    Assert.getInstance().notNull(reader, "reader");

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.CORE_API_LEVEL.getKey(), CORE_API_LEVEL);
    input.addProperty(JsonProperty.SERVICE.getKey(), PluginService.RELEASE_READER.name());

    JsonObject params = new JsonObject();
    params.addProperty(
        JsonProperty.READER_NAME.getKey(),
        reader.getName().replace(REMOTE_READER_NAME_SUFFIX, "")); // NOSONAR

    input.add(JsonProperty.PARAMETERS.getKey(), params);

    // Execute the remote service.
    try {
      executePluginServiceRemotely(input, remotePoolPluginSpi, getName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throwRuntimeException(e);
    } finally {
      getReadersMap().remove(reader.getName());
      ((RemoteReaderAdapter) reader).unregister();
    }
  }
}
