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

import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.reader.selection.CommonIsoCardSelector;
import org.junit.Before;
import org.junit.Test;

public class IsoCardSelectorAdapterTest {

  private IsoCardSelectorAdapter selectorAdapter;

  @Before
  public void setUp() {
    selectorAdapter = new IsoCardSelectorAdapter();
  }

  @Test
  public void testDefaultValues() {
    assertThat(selectorAdapter.getAid()).isNull();
    assertThat(selectorAdapter.getLogicalProtocolName()).isNull();
    assertThat(selectorAdapter.getPowerOnDataRegex()).isNull();
    assertThat(selectorAdapter.getFileOccurrence())
        .isEqualTo(CommonIsoCardSelector.FileOccurrence.FIRST);
    assertThat(selectorAdapter.getFileControlInformation())
        .isEqualTo(CommonIsoCardSelector.FileControlInformation.FCI);
  }

  @Test
  public void testFilterByDfNameByte() {
    byte[] aid = new byte[] {1, 2, 3};
    selectorAdapter.filterByDfName(aid);
    assertThat(selectorAdapter.getAid()).isEqualTo(aid);
  }

  @Test
  public void testFilterByDfNameString() {
    String aid = "010203";
    selectorAdapter.filterByDfName(aid);
    assertThat(selectorAdapter.getAid()).isEqualTo(HexUtil.toByteArray(aid));
  }

  @Test
  public void testSetFileOccurrence() {
    CommonIsoCardSelector.FileOccurrence fo = CommonIsoCardSelector.FileOccurrence.LAST;
    selectorAdapter.setFileOccurrence(fo);
    assertThat(selectorAdapter.getFileOccurrence()).isEqualTo(fo);
  }

  @Test
  public void testSetFileControlInformation() {
    CommonIsoCardSelector.FileControlInformation fci =
        CommonIsoCardSelector.FileControlInformation.FCP;
    selectorAdapter.setFileControlInformation(fci);
    assertThat(selectorAdapter.getFileControlInformation()).isEqualTo(fci);
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
