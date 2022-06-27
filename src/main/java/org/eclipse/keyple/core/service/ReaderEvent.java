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

import org.calypsonet.terminal.reader.CardReaderEvent;

/**
 * All information about a change of card state within an {@link ObservableReader}.
 *
 * <p>In the case of a card insertion, the responses received by the reader are included in the
 * event.
 *
 * @since 2.0.0
 * @deprecated Use {@link CardReaderEvent} instead.
 */
@Deprecated
public interface ReaderEvent extends CardReaderEvent {
  /**
   * Gets the name of the plugin from which the reader that generated the event comes from.
   *
   * @return A not empty string.
   * @since 2.0.0
   * @deprecated No longer supported.
   */
  @Deprecated
  String getPluginName();
}
