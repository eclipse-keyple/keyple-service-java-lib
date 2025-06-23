/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ReaderApiFactoryAdapterTest {

  private ReaderApiFactoryAdapter readerApiFactory;

  @Before
  public void setUp() {
    readerApiFactory = new ReaderApiFactoryAdapter();
  }

  @Test
  public void createCardSelectionManager_shouldReturnCardSelectionManagerInstance() {
    assertThat(readerApiFactory.createCardSelectionManager())
        .isInstanceOf(CardSelectionManagerAdapter.class);
  }

  @Test
  public void createBasicCardSelector_shouldReturnBasicCardSelectorAdapterInstance() {
    assertThat(readerApiFactory.createBasicCardSelector())
        .isInstanceOf(BasicCardSelectorAdapter.class);
  }

  @Test
  public void createIsoCardSelector_shouldReturnIsoCardSelectorAdapterInstance() {
    assertThat(readerApiFactory.createIsoCardSelector()).isInstanceOf(IsoCardSelectorAdapter.class);
  }
}
