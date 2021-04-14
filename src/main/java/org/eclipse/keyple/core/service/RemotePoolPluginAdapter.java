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
import static org.eclipse.keyple.core.service.DistributedLocalServiceAdapter.PluginService;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.AbstractMap;
import java.util.SortedSet;
import org.eclipse.keyple.core.distributed.remote.spi.RemotePluginSpi;
import org.eclipse.keyple.core.distributed.remote.spi.RemoteReaderSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of a remote {@link PoolPlugin}.
 *
 * @since 2.0
 */
final class RemotePoolPluginAdapter extends AbstractPluginAdapter implements PoolPlugin {

  private static final Logger logger = LoggerFactory.getLogger(RemotePoolPluginAdapter.class);

  private final RemotePluginSpi remotePluginSpi;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * @param remotePluginSpi The associated SPI.
   * @since 2.0
   */
  RemotePoolPluginAdapter(RemotePluginSpi remotePluginSpi) {
    super(remotePluginSpi.getName(), remotePluginSpi);
    this.remotePluginSpi = remotePluginSpi;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public SortedSet<String> getReaderGroupReferences() {

    checkStatus();

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(
        JsonProperty.SERVICE.name(), PluginService.GET_READER_GROUP_REFERENCES.name());

    // Execute the remote service.
    try {
      JsonObject output =
          DistributedUtilAdapter.executePluginServiceRemotely(
              input, remotePluginSpi, getName(), logger);

      return JsonUtil.getParser()
          .fromJson(
              output.get(JsonProperty.RESULT.name()).getAsString(),
              new TypeToken<SortedSet<String>>() {}.getType());

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public Reader allocateReader(String readerGroupReference) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "The pool plugin '{}' is allocating a reader of the group reference '{}'.",
          getName(),
          readerGroupReference);
    }
    Assert.getInstance().notEmpty(readerGroupReference, "readerGroupReference");

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), PluginService.ALLOCATE_READER.name());
    input.addProperty(JsonProperty.READER_GROUP_REFERENCE.name(), readerGroupReference);

    // Execute the remote service.
    AbstractMap.SimpleEntry<String, Boolean> readerInfo;
    try {
      JsonObject output =
          DistributedUtilAdapter.executePluginServiceRemotely(
              input, remotePluginSpi, getName(), logger);

      readerInfo =
          JsonUtil.getParser()
              .fromJson(
                  output.get(JsonProperty.RESULT.name()).getAsString(),
                  new TypeToken<AbstractMap.SimpleEntry<String, Boolean>>() {}.getType());

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
      return null;
    }

    // Build a remote reader and register it.
    String readerName = readerInfo.getKey();
    Boolean isObservable = readerInfo.getValue();

    RemoteReaderSpi remoteReaderSpi = remotePluginSpi.createRemoteReader(readerName, isObservable);

    RemoteReaderAdapter remoteReaderAdapter;
    if (remoteReaderSpi.isObservable()) {
      remoteReaderAdapter = new ObservableRemoteReaderAdapter(remoteReaderSpi, null, getName());
    } else {
      remoteReaderAdapter = new RemoteReaderAdapter(remoteReaderSpi, getName());
    }
    getReaders().put(remoteReaderSpi.getName(), remoteReaderAdapter);
    remoteReaderAdapter.register();
    return remoteReaderAdapter;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void releaseReader(Reader reader) {

    checkStatus();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "The pool plugin '{}' is releasing the reader '{}'.",
          getName(),
          reader != null ? reader.getName() : null);
    }
    Assert.getInstance().notNull(reader, "reader");

    // Build the input JSON data.
    JsonObject input = new JsonObject();
    input.addProperty(JsonProperty.SERVICE.name(), PluginService.RELEASE_READER.name());
    input.addProperty(JsonProperty.READER_NAME.name(), reader.getName());

    // Execute the remote service.
    try {
      DistributedUtilAdapter.executePluginServiceRemotely(
          input, remotePluginSpi, getName(), logger);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      DistributedUtilAdapter.throwRuntimeException(e);
    } finally {
      getReaders().remove(reader.getName());
      ((LocalReaderAdapter) reader).unregister();
    }
  }
}
