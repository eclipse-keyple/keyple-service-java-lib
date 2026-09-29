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
package org.eclipse.keyple.core.service;

import static org.assertj.core.api.Assertions.*;

import com.google.gson.JsonParser;
import org.eclipse.keyple.core.service.keypleextension.KeypleTestSmartCard;
import org.eclipse.keypleother.registeredextension.TestCardExtension;
import org.eclipse.keypleother.registeredextension.TestNotSpiSmartCard;
import org.eclipse.keypleother.registeredextension.TestSmartCard;
import org.eclipse.keypop.card.spi.CardSelectionExtensionSpi;
import org.eclipse.keypop.card.spi.SmartCardSpi;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.eclipse.keypop.reader.selection.spi.CardSelectionExtension;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.junit.BeforeClass;
import org.junit.Test;

public class JsonAdapterTest {

  private static final String UNREGISTERED_SMART_CARD_CLASS_NAME =
      "org.eclipse.keypleother.unregisteredextension.UnregisteredSmartCard";

  // Flags are held outside the test types so that reading them does not load these types
  public static boolean unregisteredTypeLoaded;
  private static boolean unexpectedTypeLoaded;
  private static boolean unexpectedTypeCreated;

  @BeforeClass
  public static void beforeClass() {
    SmartCardServiceProvider.getService().checkCardExtension(new TestCardExtension());
  }

  @Test
  public void getCardSelectorClass_whenClassIsABasicCardSelector_shouldReturnClass() {
    assertThat(JsonAdapter.getCardSelectorClass(BasicCardSelectorAdapter.class.getName()))
        .isEqualTo(BasicCardSelectorAdapter.class);
  }

  @Test
  public void getCardSelectorClass_whenClassIsAnIsoCardSelector_shouldReturnClass() {
    assertThat(JsonAdapter.getCardSelectorClass(IsoCardSelectorAdapter.class.getName()))
        .isEqualTo(IsoCardSelectorAdapter.class);
  }

  @Test
  public void getCardSelectorClass_whenClassIsNotACardSelectorOfTheService_shouldThrowIAE() {
    assertThatThrownBy(() -> JsonAdapter.getCardSelectorClass(UnexpectedType.class.getName()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not supported");
    assertThat(unexpectedTypeLoaded).isFalse();
  }

  @Test
  public void
      loadCardExtensionClass_whenClassBelongsToKeyplePackage_shouldReturnClassWithoutRegistration()
          throws Exception {
    assertThat(
            JsonAdapter.loadCardExtensionClass(
                KeypleTestSmartCard.class.getName(), SmartCard.class, SmartCardSpi.class))
        .isEqualTo(KeypleTestSmartCard.class);
  }

  @Test
  public void loadCardExtensionClass_whenClassBelongsToRegisteredExtension_shouldReturnClass()
      throws Exception {
    assertThat(
            JsonAdapter.loadCardExtensionClass(
                TestSmartCard.class.getName(), SmartCard.class, SmartCardSpi.class))
        .isEqualTo(TestSmartCard.class);
  }

  @Test
  public void loadCardExtensionClass_whenClassDoesNotBelongToRegisteredExtension_shouldThrowCNFE() {
    assertThatThrownBy(
            () ->
                JsonAdapter.loadCardExtensionClass(
                    UNREGISTERED_SMART_CARD_CLASS_NAME, SmartCard.class, SmartCardSpi.class))
        .isInstanceOf(ClassNotFoundException.class)
        .hasMessageContaining("does not belong to a supported card extension");
    assertThat(unregisteredTypeLoaded).isFalse();
  }

  @Test
  public void
      loadCardExtensionClass_whenPackageOnlyStartsLikeRegisteredExtensionPackage_shouldThrowCNFE() {
    assertThatThrownBy(
            () ->
                JsonAdapter.loadCardExtensionClass(
                    "org.eclipse.keypleother.registeredextensionfake.FakeSmartCard",
                    SmartCard.class,
                    SmartCardSpi.class))
        .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  public void loadCardExtensionClass_whenClassDoesNotImplementSpi_shouldThrowIAE() {
    assertThatThrownBy(
            () ->
                JsonAdapter.loadCardExtensionClass(
                    TestNotSpiSmartCard.class.getName(), SmartCard.class, SmartCardSpi.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(SmartCardSpi.class.getName());
  }

  @Test
  public void loadCardExtensionClass_whenClassIsNotOfExpectedType_shouldThrowIAE() {
    assertThatThrownBy(
            () ->
                JsonAdapter.loadCardExtensionClass(
                    TestSmartCard.class.getName(),
                    CardSelectionExtension.class,
                    CardSelectionExtensionSpi.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not a subtype of");
  }

  @Test
  public void loadSubclassOf_whenClassIsASubtype_shouldReturnClass() throws Exception {
    assertThat(
            JsonAdapter.loadSubclassOf(
                BasicCardSelectorAdapter.class.getName(), CardSelector.class))
        .isEqualTo(BasicCardSelectorAdapter.class);
  }

  @Test
  public void loadSubclassOf_whenClassIsUnknown_shouldThrowCNFE() {
    assertThatThrownBy(
            () -> JsonAdapter.loadSubclassOf("com.unknown.DoesNotExist", CardSelector.class))
        .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  public void loadSubclassOf_whenClassIsNotASubtype_shouldThrowIAE() {
    assertThatThrownBy(
            () -> JsonAdapter.loadSubclassOf(UnexpectedType.class.getName(), CardSelector.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not a subtype of");
    assertThat(unexpectedTypeLoaded).isFalse();
  }

  @Test
  public void
      cardSelectionScenarioAdapterJsonAdapter_deserialize_whenCardSelectorTypeIsNotACardSelector_shouldThrowIAE() {
    String json =
        "{\"multiSelectionProcessing\":\"FIRST_MATCH\",\"channelControl\":\"KEEP_OPEN\","
            + "\"cardSelectorsTypes\":[\""
            + UnexpectedType.class.getName()
            + "\"],\"cardSelectors\":[{\"payload\":\"value\"}],"
            + "\"cardSelectionRequests\":[]}";
    assertThatThrownBy(
            () ->
                new JsonAdapter.CardSelectionScenarioAdapterJsonAdapter()
                    .deserialize(
                        JsonParser.parseString(json), CardSelectionScenarioAdapter.class, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not supported");
    assertThat(unexpectedTypeLoaded).isFalse();
    assertThat(unexpectedTypeCreated).isFalse();
  }

  public static class UnexpectedType {
    static {
      unexpectedTypeLoaded = true;
    }

    public String payload;

    public UnexpectedType() {
      unexpectedTypeCreated = true;
    }
  }
}
