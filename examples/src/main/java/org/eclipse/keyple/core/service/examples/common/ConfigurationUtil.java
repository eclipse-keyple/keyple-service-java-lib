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
package org.eclipse.keyple.core.service.examples.common;

import org.eclipse.keyple.card.generic.CardResourceProfileExtension;
import org.eclipse.keyple.card.generic.GenericExtensionServiceProvider;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.util.protocol.ContactlessCardCommonProtocol;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keyple.plugin.pcsc.PcscSupportedContactlessProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing methods for configuring readers and the card resource service used across
 * several examples.
 *
 * @since 2.0
 */
public class ConfigurationUtil {
  public static final String AID_EMV_PPSE = "325041592E5359532E4444463031";
  public static final String AID_KEYPLE_PREFIX = "315449432E";
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtil.class);

  // Common reader identifiers
  // These two regular expressions can be modified to fit the names of the readers used to run these
  // examples.
  public static final String CONTACTLESS_READER_NAME_REGEX = ".*ASK LoGO.*|.*Contactless.*";
  public static final String CONTACT_READER_NAME_REGEX = ".*Identive.*|.*HID.*";

  /**
   * (private)<br>
   * Constructor.
   */
  private ConfigurationUtil() {}

  /**
   * Retrieves the first available reader in the provided plugin whose name matches the provided
   * regular expression.
   *
   * @param plugin The plugin to which the reader belongs.
   * @param readerNameRegex A regular expression matching the targeted reader.
   * @return A not null reference.
   * @throws IllegalStateException If the reader is not found.
   * @since 2.0
   */
  public static Reader getCardReader(Plugin plugin, String readerNameRegex) {
    for (String readerName : plugin.getReaderNames()) {
      if (readerName.matches(readerNameRegex)) {
        Reader reader = plugin.getReader(readerName);
        // Configure the reader with parameters suitable for contactless operations.
        reader
            .getExtension(PcscReader.class)
            .setContactless(true)
            .setIsoProtocol(PcscReader.IsoProtocol.T1)
            .setSharingMode(PcscReader.SharingMode.SHARED);
        reader.activateProtocol(
            PcscSupportedContactlessProtocol.ISO_14443_4.name(),
            ContactlessCardCommonProtocol.ISO_14443_4.name());
        logger.info("Card reader, plugin; {}, name: {}", plugin.getName(), reader.getName());
        return reader;
      }
    }
    throw new IllegalStateException(
        String.format("Reader '%s' not found in plugin '%s'", readerNameRegex, plugin.getName()));
  }

  /**
   * Setup the {@link CardResourceService} to provide a card resource when requested.
   *
   * <p>Since the card resource service is used without reader observation, the card resource must
   * be available when this method is invoked and during the entire program execution.
   *
   * @param plugin The plugin to which the card resource reader belongs.
   * @param readerNameRegex A regular expression matching the expected card resource reader name.
   * @param cardResourceProfileName A string defining the card resource profile.
   * @throws IllegalStateException If the expected card resource is not found.
   * @since 2.0
   */
  public static void setupCardResourceService(
      Plugin plugin, String readerNameRegex, String cardResourceProfileName) {

    // Create a card resource extension.
    CardResourceProfileExtension cardResourceExtension =
        GenericExtensionServiceProvider.getService().createCardResourceProfileExtension();

    // Get the service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    // Create a minimalist configuration (no plugin/reader observation)
    cardResourceService
        .getConfigurator()
        .withPlugins(
            PluginsConfigurator.builder().addPlugin(plugin, new ReaderConfigurator()).build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(cardResourceProfileName, cardResourceExtension)
                .withReaderNameRegex(readerNameRegex)
                .build())
        .configure();
    cardResourceService.start();

    // verify the resource availability
    CardResource cardResource = cardResourceService.getCardResource(cardResourceProfileName);

    if (cardResource == null) {
      throw new IllegalStateException(
          String.format(
              "Unable to retrieve a card resource for profile '%s' from reader '%s' in plugin '%s'",
              cardResourceProfileName, readerNameRegex, plugin.getName()));
    }

    // release the verified resource
    cardResourceService.releaseCardResource(cardResource);
  }

  /**
   * Reader configurator used by the card resource service to setup the card resource reader with
   * the required settings.
   */
  private static class ReaderConfigurator implements ReaderConfiguratorSpi {
    private static final Logger logger = LoggerFactory.getLogger(ReaderConfigurator.class);

    /**
     * (private)<br>
     * Constructor.
     */
    private ReaderConfigurator() {}

    /** {@inheritDoc} */
    @Override
    public void setupReader(Reader reader) {
      // Configure the reader with parameters suitable for contactless operations.
      try {
        reader
            .getExtension(PcscReader.class)
            .setContactless(false)
            .setIsoProtocol(PcscReader.IsoProtocol.T0)
            .setSharingMode(PcscReader.SharingMode.SHARED);
      } catch (Exception e) {
        logger.error("Exception raised while setting up the reader {}", reader.getName(), e);
      }
    }
  }
}
