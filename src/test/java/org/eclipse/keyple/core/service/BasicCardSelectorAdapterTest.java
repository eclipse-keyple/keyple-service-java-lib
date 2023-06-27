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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class BasicCardSelectorAdapterTest {

  private BasicCardSelectorAdapter selectorAdapter;

  @Before
  public void setUp() {
    selectorAdapter = new BasicCardSelectorAdapter();
  }

  @Test
  public void testDefaultValues() {
    assertThat(selectorAdapter.getLogicalProtocolName()).isNull();
    assertThat(selectorAdapter.getPowerOnDataRegex()).isNull();
  }

  @Test
  public void testFilterByCardProtocol() {
    String protocol = "TestProtocol";
    selectorAdapter.filterByCardProtocol(protocol);
    assertThat(selectorAdapter.getLogicalProtocolName()).isEqualTo(protocol);
  }

  @Test
  public void testFilterByPowerOnData() {
    String regex = "TestRegex";
    selectorAdapter.filterByPowerOnData(regex);
    assertThat(selectorAdapter.getPowerOnDataRegex()).isEqualTo(regex);
  }
}
