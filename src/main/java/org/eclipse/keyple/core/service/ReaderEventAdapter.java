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

import org.calypsonet.terminal.reader.selection.ScheduledCardSelectionsResponse;

/**
 * (package-private)<br>
 * Implementation of {@link ReaderEvent}.
 *
 * @since 2.0
 */
final class ReaderEventAdapter implements ReaderEvent {

  private final String pluginName;
  private final String readerName;
  private final ScheduledCardSelectionsResponse scheduledCardSelectionsResponse;

  private final Type type;

  /**
   * ReaderEvent constructor for simple insertion notification mode
   *
   * @param pluginName The name of the current plugin (should be not null).
   * @param readerName The name of the current reader (should be not null).
   * @param type The type of event (should be not null).
   * @param scheduledCardSelectionsResponse The responses received during the execution of the card
   *     selection scenario (can be null).
   * @since 2.0
   */
  public ReaderEventAdapter(
      String pluginName,
      String readerName,
      Type type,
      ScheduledCardSelectionsResponse scheduledCardSelectionsResponse) {
    this.pluginName = pluginName;
    this.readerName = readerName;
    this.type = type;
    this.scheduledCardSelectionsResponse = scheduledCardSelectionsResponse;
  }

  @Override
  public String getPluginName() {
    return pluginName;
  }

  @Override
  public String getReaderName() {
    return readerName;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public ScheduledCardSelectionsResponse getScheduledCardSelectionsResponse() {
    return scheduledCardSelectionsResponse;
  }
}
