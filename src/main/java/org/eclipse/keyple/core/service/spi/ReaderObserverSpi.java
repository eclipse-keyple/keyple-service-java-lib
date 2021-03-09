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
 * Reader observer recipient of the {@link ReaderEvent} from a {@link
 * org.eclipse.keyple.core.service.ObservableReader}.
 *
 * @since 2.0
 */
public interface ReaderObserverSpi {

  /**
   * Invoked when a reader event occurs.
   *
   * @param readerEvent The not null {@link ReaderEvent} containing all event information.
   * @since 2.0
   */
  void onReaderEvent(final ReaderEvent readerEvent);
}
