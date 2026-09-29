/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keypleother.registeredextension;

import org.eclipse.keypop.card.CardSelectionResponseApi;
import org.eclipse.keypop.card.spi.CardSelectionExtensionSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.card.spi.SmartCardSpi;
import org.eclipse.keypop.reader.selection.spi.CardSelectionExtension;

/** Card selection extension of the test card extension. */
public class TestCardSelectionExtension
    implements CardSelectionExtension, CardSelectionExtensionSpi {

  @Override
  public CardSelectionRequestSpi getCardSelectionRequest() {
    return null;
  }

  @Override
  public SmartCardSpi parse(CardSelectionResponseApi cardSelectionResponseApi) {
    return new TestSmartCard();
  }
}
