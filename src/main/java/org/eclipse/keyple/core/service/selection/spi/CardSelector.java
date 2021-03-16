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
package org.eclipse.keyple.core.service.selection.spi;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.eclipse.keyple.core.common.KeypleCardSelector;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This POJO holds the information needed to select a particular card.
 *
 * <p>In addition to the card protocol provides two optional structure {@link AidSelector} and
 * {@link AtrFilter} to specify the expected card profile.
 *
 * @since 2.0
 */
public class CardSelector implements KeypleCardSelector {

  private static final Logger logger = LoggerFactory.getLogger(CardSelector.class);

  private String cardProtocol;
  private AidSelector aidSelector;
  private AtrFilter atrFilter;

  /**
   * The AID selection data.
   *
   * <p>- AID’s bytes of the card application to select. In case the card application is currently
   * not selected, a logical channel is established and the corresponding card application is
   * selected by the card reader, otherwise keep the current channel. <br>
   *
   * <ul>
   *   <li>optional {@link FileOccurrence} and {@link FileControlInformation} defines selections
   *       modes according to ISO7816-4.
   *   <li>optional successfulSelectionStatusCodes define a list of accepted SW1SW2 codes (in
   *       addition to 9000). Allows, for example, to manage the selection of the invalidated cards.
   *   <li>AidSelector could be missing in CardSelector when operating a card which don’t support
   *       the Select Application command (as it is the case for SAM).
   * </ul>
   *
   * @since 2.0
   */
  public static final class AidSelector {
    public static final int AID_MIN_LENGTH = 5;
    public static final int AID_MAX_LENGTH = 16;

    /**
     * FileOccurrence indicates how to carry out the file occurrence in accordance with ISO7816-4
     *
     * <p>The getIsoBitMask method provides the bit mask to be used to set P2 in the select command
     * (ISO/IEC 7816-4.2)
     *
     * @since 2.0
     */
    public enum FileOccurrence {
      /**
       * First occurrence.
       *
       * @since 2.0
       */
      FIRST((byte) 0x00),
      /**
       * Last occurrence.
       *
       * @since 2.0
       */
      LAST((byte) 0x01),
      /**
       * Next occurrence.
       *
       * @since 2.0
       */
      NEXT((byte) 0x02),
      /**
       * Previous occurrence.
       *
       * @since 2.0
       */
      PREVIOUS((byte) 0x03);

      private final byte isoBitMask;

      FileOccurrence(byte isoBitMask) {
        this.isoBitMask = isoBitMask;
      }

      /**
       * Gets the bit mask to apply to the corresponding byte in the ISO selection application
       * command.
       *
       * @return A byte.
       * @since 2.0
       */
      public byte getIsoBitMask() {
        return isoBitMask;
      }
    }

    /**
     * FileControlInformation indicates how to which template is expected in accordance with
     * ISO7816-4
     *
     * <p>The getIsoBitMask method provides the bit mask to be used to set P2 in the select command
     * (ISO/IEC 7816-4.2)
     *
     * @since 2.0
     */
    public enum FileControlInformation {
      /**
       * File control information.
       *
       * @since 2.0
       */
      FCI(((byte) 0x00)),
      /**
       * File control parameters.
       *
       * @since 2.0
       */
      FCP(((byte) 0x04)),
      /**
       * File management data.
       *
       * @since 2.0
       */
      FMD(((byte) 0x08)),
      /**
       * No response expected.
       *
       * @since 2.0
       */
      NO_RESPONSE(((byte) 0x0C));

      private final byte isoBitMask;

      FileControlInformation(byte isoBitMask) {
        this.isoBitMask = isoBitMask;
      }

      /**
       * Gets the bit mask to apply to the corresponding byte in the ISO selection application
       * command.
       *
       * @return A byte.
       * @since 2.0
       */
      public byte getIsoBitMask() {
        return isoBitMask;
      }
    }

    private byte[] aidToSelect;
    private FileOccurrence fileOccurrence;
    private FileControlInformation fileControlInformation;

    /*
     * List of status codes in response to the select application command that should be considered
     * successful although they are different from 9000
     */
    private Set<Integer> successfulSelectionStatusCodes;

    /**
     * Constructor
     *
     * @since 2.0
     */
    public AidSelector() {
      this.fileOccurrence = FileOccurrence.FIRST;
      this.fileControlInformation = FileControlInformation.FCI;
      this.successfulSelectionStatusCodes = null;
    }

    /**
     * Sets the AID provided as an array of bytes.
     *
     * @param aid The AID as byte array.
     * @return The object instance.
     * @since 2.0
     */
    public AidSelector setAidToSelect(byte[] aid) {
      if (aid.length < AID_MIN_LENGTH || aid.length > AID_MAX_LENGTH) {
        aidToSelect = null;
        throw new IllegalArgumentException(
            "Bad AID length: " + aid.length + ". The AID length should be " + "between 5 and 15.");
      } else {
        aidToSelect = aid;
      }
      return this;
    }

    /**
     * Sets the AID provided as an hex string.
     *
     * @param aid The AID as a String.
     * @return The object instance..
     * @since 2.0
     */
    public AidSelector setAidToSelect(String aid) {
      return this.setAidToSelect(ByteArrayUtil.fromHex(aid));
    }

    /**
     * Sets the file occurrence mode (see ISO7816-4)
     *
     * @param fileOccurrence the {@link FileOccurrence}
     * @return The object instance.
     * @since 2.0
     */
    public AidSelector setFileOccurrence(FileOccurrence fileOccurrence) {
      this.fileOccurrence = fileOccurrence;
      return this;
    }

    /**
     * Sets the file control mode (see ISO7816-4)
     *
     * @param fileControlInformation the {@link FileControlInformation}
     * @return The object instance.
     * @since 2.0
     */
    public AidSelector setFileControlInformation(FileControlInformation fileControlInformation) {
      this.fileControlInformation = fileControlInformation;
      return this;
    }

    /**
     * Getter for the AID provided at construction time
     *
     * @return byte array containing the AID
     * @since 2.0
     */
    public byte[] getAidToSelect() {
      return aidToSelect;
    }

    /**
     * @return the file occurrence parameter
     * @since 2.0
     */
    public FileOccurrence getFileOccurrence() {
      return fileOccurrence;
    }

    /**
     * @return the file control information parameter
     * @since 2.0
     */
    public FileControlInformation getFileControlInformation() {
      return fileControlInformation;
    }

    /**
     * Gets the list of successful selection status codes
     *
     * @return the list of status codes
     * @since 2.0
     */
    public Set<Integer> getSuccessfulSelectionStatusCodes() {
      return successfulSelectionStatusCodes;
    }

    /**
     * Add as status code to be accepted to the list of successful selection status codes
     *
     * @param statusCode the status code to be accepted
     * @since 2.0
     */
    public void addSuccessfulStatusCode(int statusCode) {
      // the list is kept null until a code is added
      if (this.successfulSelectionStatusCodes == null) {
        this.successfulSelectionStatusCodes = new LinkedHashSet<Integer>();
      }
      this.successfulSelectionStatusCodes.add(statusCode);
    }

    /**
     * Print out the AID in hex
     *
     * @return a string
     * @since 2.0
     */
    @Override
    public String toString() {
      return "AID_SELECTOR = " + JsonUtil.toJson(this);
    }
  }

  /**
   * Static nested class to hold the data elements used to perform an ATR based filtering
   *
   * @since 2.0
   */
  public static final class AtrFilter {
    /**
     * Regular expression dedicated to handle the card logical channel opening based on ATR pattern
     */
    private String atrRegex;

    /**
     * Regular expression based filter
     *
     * @param atrRegex String hex regular expression
     * @since 2.0
     */
    public AtrFilter(String atrRegex) {
      this.atrRegex = atrRegex;
    }

    /**
     * Setter for the regular expression provided at construction time
     *
     * @param atrRegex expression string
     * @since 2.0
     */
    public void setAtrRegex(String atrRegex) {
      this.atrRegex = atrRegex;
    }

    /**
     * Getter for the regular expression provided at construction time
     *
     * @return Regular expression string
     * @since 2.0
     */
    public String getAtrRegex() {
      return atrRegex;
    }

    /**
     * Tells if the provided ATR matches the registered regular expression
     *
     * <p>If the registered regular expression is empty, the ATR is always matching.
     *
     * @param atr a buffer containing the ATR to be checked
     * @return a boolean true the ATR matches the current regex
     * @since 2.0
     */
    public boolean atrMatches(byte[] atr) {
      boolean m;
      if (atrRegex.length() != 0) {
        Pattern p = Pattern.compile(atrRegex);
        String atrString = ByteArrayUtil.toHex(atr);
        m = p.matcher(atrString).matches();
      } else {
        m = true;
      }
      return m;
    }

    /**
     * Print out the ATR regex
     *
     * @return a string
     * @since 2.0
     */
    @Override
    public String toString() {
      return "ATR_FILTER = " + JsonUtil.toJson(this);
    }
  }

  /**
   * Constructor
   *
   * @since 2.0
   */
  public CardSelector() {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "Selection data: AID = {}, ATRREGEX = {}",
          (this.aidSelector == null || this.aidSelector.getAidToSelect() == null)
              ? "null"
              : ByteArrayUtil.toHex(this.aidSelector.getAidToSelect()),
          this.atrFilter == null ? "null" : this.atrFilter.getAtrRegex());
    }
  }

  /**
   * Sets the card protocol.
   *
   * @param cardProtocol A not empty String.
   * @return The object instance.
   * @since 2.0
   */
  public final CardSelector setCardProtocol(String cardProtocol) {
    this.cardProtocol = cardProtocol;
    return this;
  }

  /**
   * Sets the card ATR Filter
   *
   * @param atrFilter the {@link AtrFilter} of the targeted card
   * @return The object instance.
   * @since 2.0
   */
  public final CardSelector setAtrFilter(AtrFilter atrFilter) {
    this.atrFilter = atrFilter;
    return this;
  }

  /**
   * Sets the card AID Selector
   *
   * @param aidSelector the {@link AidSelector} of the targeted card
   * @return The object instance.
   * @since 2.0
   */
  public final CardSelector setAidSelector(AidSelector aidSelector) {
    this.aidSelector = aidSelector;
    return this;
  }

  /**
   * Gets the card protocol name.
   *
   * @return null if no card protocol has been set.
   * @since 2.0
   */
  public final String getCardProtocol() {
    return cardProtocol;
  }

  /**
   * Gets the ATR filter
   *
   * @return null if no ATR filter has been set.
   * @since 2.0
   */
  public final AtrFilter getAtrFilter() {
    return atrFilter;
  }

  /**
   * Gets the AID selector
   *
   * @return null if no AID selector has been set.
   * @since 2.0
   */
  public final AidSelector getAidSelector() {
    return aidSelector;
  }

  @Override
  public String toString() {
    return "CARD_SELECTOR = " + JsonUtil.toJson(this);
  }
}
