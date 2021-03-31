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
import java.util.regex.PatternSyntaxException;
import org.eclipse.keyple.core.common.KeypleCardSelector;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * This POJO holds the information needed to select a particular card.
 *
 * <p>In addition to the card protocol provides two optional structure {@link AidSelector} and
 * {@link AtrFilter} to specify the expected card profile.
 *
 * @since 2.0
 */
public class CardSelector implements KeypleCardSelector {

  private String cardProtocol;
  private AidSelector aidSelector;
  private AtrFilter atrFilter;

  /**
   * The {@link AidSelector} defines the elements that will be used to construct the SELECT
   * APPLICATION command as defined in ISO7816-4 and sent to the card during the selection process.
   *
   * <ul>
   *   <li>The AID is mandatory and is the identifier of the targeted application.
   *   <li>{@link FileOccurrence} and {@link FileControlInformation} are optional and specify
   *       selections modes according to ISO7816-4.
   *   <li>An optional successful status codes may be added to create a list of accepted SW1SW2
   *       codes (in addition to 9000). Allows, for example, to manage the selection of the
   *       invalidated cards.
   * </ul>
   *
   * Note: The {@link AidSelector} is an optional field of the {@link CardSelector}.
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

    private final byte[] aid;
    private FileOccurrence fileOccurrence;
    private FileControlInformation fileControlInformation;

    /*
     * List of status codes in response to the select application command that should be considered
     * successful although they are different from 9000
     */
    private Set<Integer> successfulSelectionStatusCodes;

    /**
     * Creates an instance of {@link AidSelector} with its only mandatory parameter, the AID as an
     * array of bytes.
     *
     * @param aid A byte array containing {@value AID_MIN_LENGTH} to {@value AID_MAX_LENGTH} bytes.
     * @since 2.0
     * @throws IllegalArgumentException If the provided array is null or out of range.
     */
    public AidSelector(byte[] aid) {
      Assert.getInstance()
          .notNull(aid, "aid")
          .isInRange(aid.length, AID_MIN_LENGTH, AID_MAX_LENGTH, "aid");
      this.aid = aid;
      this.fileOccurrence = FileOccurrence.FIRST;
      this.fileControlInformation = FileControlInformation.FCI;
      this.successfulSelectionStatusCodes = null;
    }

    /**
     * Creates an instance of {@link AidSelector} with its only mandatory parameter, the AID as an
     * array of bytes.
     *
     * @param aid A byte array containing {@value AID_MIN_LENGTH} to {@value AID_MAX_LENGTH} bytes.
     * @since 2.0
     * @throws IllegalArgumentException If the provided AID is null, invalid or out of range.
     */
    public AidSelector(String aid) {
      this(ByteArrayUtil.fromHex(aid));
    }

    /**
     * Sets the file occurrence mode (see ISO7816-4).
     *
     * <p>The default value is {@link FileOccurrence#FIRST}.
     *
     * @param fileOccurrence The {@link FileOccurrence}.
     * @return The object instance.
     * @throws IllegalArgumentException If fileOccurrence is null.
     * @since 2.0
     */
    public AidSelector setFileOccurrence(FileOccurrence fileOccurrence) {
      Assert.getInstance().notNull(fileOccurrence, "fileOccurrence");
      this.fileOccurrence = fileOccurrence;
      return this;
    }

    /**
     * Sets the file control mode (see ISO7816-4).
     *
     * <p>The default value is {@link FileControlInformation#FCI}.
     *
     * @param fileControlInformation The {@link FileControlInformation}.
     * @return The object instance.
     * @throws IllegalArgumentException If fileControlInformation is null.
     * @since 2.0
     */
    public AidSelector setFileControlInformation(FileControlInformation fileControlInformation) {
      Assert.getInstance().notNull(fileControlInformation, "fileControlInformation");
      this.fileControlInformation = fileControlInformation;
      return this;
    }

    /**
     * Add as status code to be accepted to the list of successful selection status codes.
     *
     * @param statusCode The status code to be accepted.
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
     * Gets the AID.
     *
     * @return A not empty byte array.
     * @since 2.0
     */
    public byte[] getAid() {
      return aid;
    }

    /**
     * @return The file occurrence parameter.
     * @since 2.0
     */
    public FileOccurrence getFileOccurrence() {
      return fileOccurrence;
    }

    /**
     * @return The file control information parameter.
     * @since 2.0
     */
    public FileControlInformation getFileControlInformation() {
      return fileControlInformation;
    }

    /**
     * Gets the list of successful selection status codes.
     *
     * @return A not null reference.
     * @since 2.0
     */
    public Set<Integer> getSuccessfulSelectionStatusCodes() {
      return successfulSelectionStatusCodes;
    }

    /**
     * Print out the AID in JSon format.
     *
     * @return A string.
     * @since 2.0
     */
    @Override
    public String toString() {
      return "AID_SELECTOR = " + JsonUtil.toJson(this);
    }
  }

  /**
   * The {@link AtrFilter} allows the identification of a card using a regular expression applied to
   * the ATR (transformed into a hexadecimal string).
   *
   * <p>It is mainly used when the card does not support the SELECT APPLICATION command.
   *
   * <p>Note: The {@link AtrFilter} is an optional field of the {@link CardSelector}.
   *
   * @since 2.0
   */
  public static final class AtrFilter {

    private final String atrRegex;

    /**
     * Creates an instance of {@link AtrFilter}.
     *
     * @param atrRegex A string containing a valid regular expression.
     * @throws IllegalArgumentException If atrRegex is null, empty or invalid.
     * @since 2.0
     */
    public AtrFilter(String atrRegex) {
      Assert.getInstance().notEmpty(atrRegex, "atrRegex");
      try {
        Pattern.compile(atrRegex);
      } catch (PatternSyntaxException exception) {
        throw new IllegalArgumentException(
            String.format("Invalid regular expression: '%s'.", atrRegex));
      }
      this.atrRegex = atrRegex;
    }

    /**
     * Gets the regular expression.
     *
     * @return A not empty string.
     * @since 2.0
     */
    public String getAtrRegex() {
      return atrRegex;
    }

    /**
     * Tells if the provided ATR matches the registered regular expression
     *
     * @param atr a buffer containing the ATR to be checked
     * @return a boolean true the ATR matches the current regex
     * @since 2.0
     */
    public boolean atrMatches(byte[] atr) {
      return ByteArrayUtil.toHex(atr).matches(atrRegex);
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
  public CardSelector() {}

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
