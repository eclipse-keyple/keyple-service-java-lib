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
package org.eclipse.keyple.core.service.spi;

import org.eclipse.keyple.core.service.ReaderEvent;

/**
 * This interface has to be implemented by reader observers.
 *
 * @since 2.0
 */
public interface ReaderObserverSpi {

  /**
   * Called when a reader event occurs.
   *
   * <p>Note that this method is called <b>sequentially</b> on all observers.
   *
   * @param readerEvent The not null {@link ReaderEvent} containing all event information.
   * @since 2.0
   */
  void onReaderEvent(final ReaderEvent readerEvent);
}
