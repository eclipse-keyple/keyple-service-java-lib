/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.examples.UseCase8_CardResourceService;

import org.calypsonet.terminal.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.card.generic.CardResourceProfileExtension;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.card.generic.GenericExtensionServiceProvider;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case ‘generic 8’ – Card resource service (PC/SC)</h1>
 *
 * <p>We demonstrate here the usage of the card resource service with a local pool of PC/SC readers.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>The card resource service is configured and started to observe the connection/disconnection
 *       of readers and the insertion/removal of cards.
 *   <li>A command line menu allows you to take and release the two defined types of card resources.
 *   <li>The log and console printouts show the operation of the card resource service.
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0
 */
public class Main_CardResourceService_Pcsc {
  private static final Logger logger = LoggerFactory.getLogger(Main_CardResourceService_Pcsc.class);
  public static final String ATR_REGEX_A = "^3B3F9600805A4880C1205017AEC0[0-9A-F]{4}829000$";
  public static final String ATR_REGEX_B = "^3B3F9600805A4880C1205017AEC1[0-9A-F]{4}829000$";
  public static final String RESOURCE_A = "RESOURCE_A";
  public static final String RESOURCE_B = "RESOURCE_B";
  public static final String READER_NAME_REGEX_A = ".*Identive.*";
  public static final String READER_NAME_REGEX_B = ".*HID.*";

  public static void main(String[] args) throws InterruptedException {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    // Get the generic card extension service
    GenericExtensionService cardExtension = GenericExtensionServiceProvider.getService();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    logger.info("=============== UseCase Generic #8: card resource service ==================");

    // Create a card resource extension A expecting a card having power-on data matching the regex
    // A.
    CardResourceProfileExtension cardResourceExtensionA =
        GenericExtensionServiceProvider.getService()
            .createCardResourceProfileExtension()
            .setPowerOnDataRegex(ATR_REGEX_A);

    // Create a card resource extension B expecting a card having power-on data matching the regex
    // B.
    CardResourceProfileExtension cardResourceExtensionB =
        GenericExtensionServiceProvider.getService()
            .createCardResourceProfileExtension()
            .setPowerOnDataRegex(ATR_REGEX_B);

    // Get the service
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();

    PluginAndReaderExceptionHandler pluginAndReaderExceptionHandler =
        new PluginAndReaderExceptionHandler();

    // Configure the card resource service:
    // - allocation mode is blocking with a 100 milliseconds cycle and a 10 seconds timeout.
    // - the readers are searched in the PC/SC plugin, the observation of the plugin (for the
    // connection/disconnection of readers) and of the readers (for the insertion/removal of cards)
    // is activated.
    // - two card resource profiles A and B are defined, each expecting a specific card
    // characterized by its power-on data and placed in a specific reader.
    // - the timeout for using the card's resources is set at 5 seconds.
    cardResourceService
        .getConfigurator()
        .withBlockingAllocationMode(100, 10000)
        .withPlugins(
            PluginsConfigurator.builder()
                .addPluginWithMonitoring(
                    plugin,
                    new ReaderConfigurator(),
                    pluginAndReaderExceptionHandler,
                    pluginAndReaderExceptionHandler)
                .withUsageTimeout(5000)
                .build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(RESOURCE_A, cardResourceExtensionA)
                .withReaderNameRegex(READER_NAME_REGEX_A)
                .build(),
            CardResourceProfileConfigurator.builder(RESOURCE_B, cardResourceExtensionB)
                .withReaderNameRegex(READER_NAME_REGEX_B)
                .build())
        .configure();

    cardResourceService.start();

    // sleep for a moment for a better readability of the console display
    Thread.sleep(2000);

    logger.info("= #### Connect/disconnect readers, insert/remove cards, watch the log.");

    boolean loop = true;
    CardResource cardResourceA = null;
    CardResource cardResourceB = null;
    while (loop) {
      char c = getInput();
      switch (c) {
        case 'a':
          cardResourceA = cardResourceService.getCardResource(RESOURCE_A);
          if (cardResourceA != null) {
            logger.info(
                "Card resource A is available: reader {}, smart card {}",
                cardResourceA.getReader().getName(),
                cardResourceA.getSmartCard());
          } else {
            logger.info("Card resource A is not available");
          }
          break;
        case 'A':
          if (cardResourceA != null) {
            logger.info("Release card resource A.");
            cardResourceService.releaseCardResource(cardResourceA);
          } else {
            logger.error("Card resource A is not available");
          }
          break;
        case 'b':
          cardResourceB = cardResourceService.getCardResource(RESOURCE_B);
          if (cardResourceB != null) {
            logger.info(
                "Card resource B is available: reader {}, smart card {}",
                cardResourceB.getReader().getName(),
                cardResourceB.getSmartCard());
          } else {
            logger.info("Card resource B is not available");
          }
          break;
        case 'B':
          if (cardResourceB != null) {
            logger.info("Release card resource B.");
            cardResourceService.releaseCardResource(cardResourceB);
          } else {
            logger.error("Card resource B is not available");
          }
          break;
        case 'q':
          loop = false;
          break;
        default:
          break;
      }
    }

    // unregister plugin
    smartCardService.unregisterPlugin(plugin.getName());

    logger.info("Exit program.");
  }

  /**
   * Reader configurator used by the card resource service to setup the SAM reader with the required
   * settings.
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

  /** Class implementing the exception handler SPIs for plugin and reader monitoring. */
  private static class PluginAndReaderExceptionHandler
      implements PluginObservationExceptionHandlerSpi, CardReaderObservationExceptionHandlerSpi {

    @Override
    public void onPluginObservationError(String pluginName, Throwable e) {
      logger.error("An exception occurred while monitoring the plugin '{}'.", pluginName, e);
    }

    @Override
    public void onReaderObservationError(String pluginName, String readerName, Throwable e) {
      logger.error(
          "An exception occurred while monitoring the reader '{}/{}'.", pluginName, readerName, e);
    }
  }

  public static char getInput() {

    int key = 0;

    System.out.println("Options:");
    System.out.println("    'a': Get resource A");
    System.out.println("    'A': Release resource A");
    System.out.println("    'b': Get resource B");
    System.out.println("    'B': Release resource B");
    System.out.print("Select an option: ");

    try {
      key = System.in.read();
    } catch (java.io.IOException e) {
      logger.error("Input error");
    }

    try {
      while ((System.in.available()) != 0) System.in.read();
    } catch (java.io.IOException e) {
      logger.error("Input error");
    }

    return (char) key;
  }

  /**
   * This object is used to freeze the main thread while card operations are handle through the
   * observers callbacks. A call to the notify() method would end the program (not demonstrated
   * here).
   */
  private static final Object waitForEnd = new Object();
}
