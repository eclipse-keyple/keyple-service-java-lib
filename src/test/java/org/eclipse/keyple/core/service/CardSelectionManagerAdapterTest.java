/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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

import org.junit.Before;
import org.junit.Test;

public class CardSelectionManagerAdapterTest {

  private CardSelectionManagerAdapter manager;

  @Before
  public void setUp() {
    manager =
        (CardSelectionManagerAdapter)
            SmartCardServiceProvider.getService().createCardSelectionManager();
  }

  @Test(expected = IllegalStateException.class)
  public void exportProcessedCardSelectionScenario_whenScenarioIsNotProcessed_shouldThrowISE() {
    manager.exportProcessedCardSelectionScenario();
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsNull_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsNull2_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario("null");
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsEmpty_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void importProcessedCardSelectionScenario_whenArgIsMalformed_shouldThrowIAE() {
    manager.importProcessedCardSelectionScenario("test");
  }
}
