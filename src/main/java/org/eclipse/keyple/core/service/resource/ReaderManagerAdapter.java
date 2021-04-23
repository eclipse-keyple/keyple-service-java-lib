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
package org.eclipse.keyple.core.service.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.card.ProxyReader;
import org.eclipse.keyple.core.card.spi.CardResourceProfileExtensionSpi;
import org.eclipse.keyple.core.card.spi.SmartCardSpi;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;

/**
 * (package-private)<br>
 * Manager of a reader associated to a "regular" plugin.
 *
 * <p>It contains all associated created card resources and manages concurrent access to the
 * reader's card resources so that only one card resource can be used at a time.
 *
 * @since 2.0
 */
class ReaderManagerAdapter {

  /** The associated reader */
  private final Reader reader;

  /** The associated plugin */
  private final Plugin plugin;

  /** Collection of all created card resources. */
  private final Set<CardResource> cardResources;

  /** Current selected card resource. */
  private CardResource selectedCardResource;

  /** Indicates if a card resource is actually in use. */
  private volatile boolean isBusy;

  /** Indicates if the associated reader is accepted by at least one card profile manager. */
  private volatile boolean isActive;

  /**
   * (package-private)<br>
   * Creates a new reader manager not active by default.
   *
   * @param reader The associated reader.
   * @param plugin The associated plugin.
   * @since 2.0
   */
  ReaderManagerAdapter(Reader reader, Plugin plugin) {
    this.reader = reader;
    this.plugin = plugin;
    this.cardResources = Collections.newSetFromMap(new ConcurrentHashMap<CardResource, Boolean>());
    this.selectedCardResource = null;
    this.isBusy = false;
    this.isActive = false;
  }

  /**
   * (package-private)<br>
   * Gets the associated reader.
   *
   * @return A not null reference.
   * @since 2.0
   */
  Reader getReader() {
    return reader;
  }

  /**
   * (package-private)<br>
   * Gets the associated plugin.
   *
   * @return A not null reference.
   * @since 2.0
   */
  Plugin getPlugin() {
    return plugin;
  }

  /**
   * (package-private)<br>
   * Gets a view of the current created card resources.
   *
   * @return An empty collection if there's no card resources.
   * @since 2.0
   */
  Set<CardResource> getCardResources() {
    return cardResources;
  }

  /**
   * (package-private)<br>
   * Indicates if the associated reader is accepted by at least one card profile manager.
   *
   * @return True if the reader manager is active.
   * @since 2.0
   */
  boolean isActive() {
    return isActive;
  }

  /**
   * (package-private)<br>
   * Activates the reader manager.
   *
   * @since 2.0
   */
  void activate() {
    this.isActive = true;
  }

  /**
   * (package-private)<br>
   * Gets a new or an existing card resource if the current inserted card matches with the provided
   * card resource profile extension.
   *
   * <p>If the card matches, then updates the current selected card resource.
   *
   * <p>In any case, invoking this method unlocks the reader due to the use of the selection service
   * by the extension during the match process.
   *
   * @param extension The card resource profile extension to use for matching.
   * @return Null if the inserted card does not match with the provided profile extension.
   * @since 2.0
   */
  CardResource matches(CardResourceProfileExtensionSpi extension) {
    CardResource cardResource = null;
    SmartCardSpi smartCard = extension.matches((ProxyReader) reader);
    if (smartCard != null) {
      cardResource = getOrCreateCardResource((SmartCard) smartCard);
      selectedCardResource = cardResource;
    }
    unlock();
    return cardResource;
  }

  /**
   * (package-private)<br>
   * Tries to lock the provided card resource if the reader is not busy.
   *
   * <p>If the provided card resource is not the current selected one, then tries to select it using
   * the provided card resource profile extension.
   *
   * @param cardResource The card resource to lock.
   * @param extension The card resource profile extension to use in case if a new selection is
   *     needed.
   * @return True if the card resource is locked.
   * @throws IllegalStateException If a new selection has been made and the current card does not
   *     match the provided profile extension or is not the same smart card than the provided one.
   * @since 2.0
   */
  boolean lock(CardResource cardResource, CardResourceProfileExtensionSpi extension) {
    if (isBusy) {
      return false;
    }
    if (selectedCardResource != cardResource) {
      SmartCardSpi smartCard = extension.matches((ProxyReader) reader);
      if (smartCard == null
          || !Arrays.equals(
              cardResource.getSmartCard().getAtrBytes(), ((SmartCard) smartCard).getAtrBytes())
          || !Arrays.equals(
              cardResource.getSmartCard().getFciBytes(), ((SmartCard) smartCard).getFciBytes())) {
        selectedCardResource = null;
        throw new IllegalStateException(
            "No card is inserted or its profile does not match the associated data.");
      }
      selectedCardResource = cardResource;
    }
    isBusy = true;
    return true;
  }

  /**
   * (package-private)<br>
   * Free the reader.
   *
   * @since 2.0
   */
  void unlock() {
    isBusy = false;
  }

  /**
   * (package-private)<br>
   * Removes the provided card resource.
   *
   * @param cardResource The card resource to remove.
   * @since 2.0
   */
  void removeCardResource(CardResource cardResource) {
    cardResources.remove(cardResource);
    if (selectedCardResource == cardResource) {
      selectedCardResource = null;
    }
  }

  /**
   * (private)<br>
   * Gets an existing card resource having the same smart card than the provided one, or creates a
   * new one if not.
   *
   * @param smartCard The associated smart card.
   * @return A not null reference.
   */
  private CardResource getOrCreateCardResource(SmartCard smartCard) {
    // Check if an identical card resource is already created.
    for (CardResource cardResource : cardResources) {
      if (Arrays.equals(cardResource.getSmartCard().getAtrBytes(), smartCard.getAtrBytes())
          && Arrays.equals(cardResource.getSmartCard().getFciBytes(), smartCard.getFciBytes())) {
        return cardResource;
      }
    }
    // If none, then create a new one.
    CardResource cardResource = new CardResource(reader, smartCard);
    cardResources.add(cardResource);
    return cardResource;
  }
}
