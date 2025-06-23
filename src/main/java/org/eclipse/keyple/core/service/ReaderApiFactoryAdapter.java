/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.BasicCardSelector;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.IsoCardSelector;

/**
 * Implementation of {@link ReaderApiFactory}.
 *
 * @since 3.0.0
 */
class ReaderApiFactoryAdapter implements ReaderApiFactory {

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public CardSelectionManager createCardSelectionManager() {
    return new CardSelectionManagerAdapter();
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public BasicCardSelector createBasicCardSelector() {
    return new BasicCardSelectorAdapter();
  }

  /**
   * {@inheritDoc}
   *
   * @since 3.0.0
   */
  @Override
  public IsoCardSelector createIsoCardSelector() {
    return new IsoCardSelectorAdapter();
  }
}
