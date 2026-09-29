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
package org.eclipse.keypleother.unregisteredextension;

import org.eclipse.keyple.core.service.JsonAdapterTest;
import org.eclipse.keypop.card.spi.SmartCardSpi;
import org.eclipse.keypop.reader.selection.spi.SmartCard;

/** Smart card of a card extension which is not registered with the service. */
public class UnregisteredSmartCard implements SmartCard, SmartCardSpi {

  static {
    JsonAdapterTest.unregisteredTypeLoaded = true;
  }

  public UnregisteredSmartCard() {
    JsonAdapterTest.unregisteredTypeLoaded = true;
  }

  @Override
  public String getPowerOnData() {
    return null;
  }
}
