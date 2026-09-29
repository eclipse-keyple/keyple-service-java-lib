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

import org.eclipse.keyple.core.common.CommonApiProperties;
import org.eclipse.keyple.core.common.KeypleCardExtension;
import org.eclipse.keypop.card.CardApiProperties;
import org.eclipse.keypop.reader.ReaderApiProperties;

/** Card extension not provided by the Eclipse Keyple project, registered by the tests. */
public class TestCardExtension implements KeypleCardExtension {

  @Override
  public String getReaderApiVersion() {
    return ReaderApiProperties.VERSION;
  }

  @Override
  public String getCardApiVersion() {
    return CardApiProperties.VERSION;
  }

  @Override
  public String getCommonApiVersion() {
    return CommonApiProperties.VERSION;
  }
}
