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
package org.eclipse.keyple.core.service.selection;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.keyple.core.common.KeypleCardSelector;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.core.util.json.JsonUtil;

/**
 * Contains information needed to select a particular card.
 *
 * <p>Provides a builder to define 3 filtering levels based:
 *
 * <ul>
 *   <li>The card protocol.
 *   <li>A regular expression to be applied to the power on sequence data (ATR).
 *   <li>An Application Identifier (AID) used to create a standard SELECT APPLICATION Apdu with
 *       various options.
 * </ul>
 *
 * <p>All three filter levels are optional.
 *
 * <p>Also provides a method to check the match between the ATR of a card and the defined filter.
 *
 * @since 2.0
 */
public final class CardSelector implements KeypleCardSelector {

  private final String cardProtocol;
  private final String atrRegex;
  private final byte[] aid;
  private final FileOccurrence fileOccurrence;
  private final FileControlInformation fileControlInformation;
  private final LinkedHashSet<Integer> successfulSelectionStatusCodes;

  /**
   * Builder of {@link CardSelector}.
   *
   * @since 2.0
   */
  public static final class Builder {
    private static final int AID_MIN_LENGTH = 5;
    private static final int AID_MAX_LENGTH = 16;
    private static final int DEFAULT_SUCCESSFUL_CODE = 0x9000;

    private String cardProtocol;
    private String atrRegex;
    private byte[] aid;
    private FileOccurrence fileOccurrence;
    private FileControlInformation fileControlInformation;
    private final LinkedHashSet<Integer> successfulSelectionStatusCodes;

    /** (private) */
    private Builder() {
      this.successfulSelectionStatusCodes = new LinkedHashSet<Integer>();
      this.successfulSelectionStatusCodes.add(DEFAULT_SUCCESSFUL_CODE);
    }

    /**
     * Requests an protocol-based filtering by defining an expected card.
     *
     * <p>If the card protocol is set, only cards using that protocol will match the card selector.
     *
     * @param cardProtocol A not empty String.
     * @return The object instance.
     * @throws IllegalArgumentException If the argument is null or empty.
     * @throws IllegalStateException If this parameter has already been set.
     * @since 2.0
     */
    public Builder filterByCardProtocol(String cardProtocol) {

      Assert.getInstance().notEmpty(cardProtocol, "cardProtocol");

      if (this.cardProtocol != null) {
        throw new IllegalStateException(
            String.format("cardProtocol has already been set to '%s'", this.cardProtocol));
      }

      this.cardProtocol = cardProtocol;
      return this;
    }

    /**
     * Requests an ATR-based filtering by defining a regular expression that will be applied to the
     * card's ATR (power on sequence data).
     *
     * <p>If it is set, only the cards whose ATR is recognized by the provided regular expression
     * will match the card selector.
     *
     * @param atrRegex A valid regular expression
     * @return The object instance.
     * @throws IllegalArgumentException If the provided regular expression is null, empty or
     *     invalid.
     * @throws IllegalStateException If this parameter has already been set.
     * @since 2.0
     */
    public Builder filterByAtr(String atrRegex) {

      Assert.getInstance().notEmpty(atrRegex, "atrRegex");

      if (this.atrRegex != null) {
        throw new IllegalStateException(
            String.format("atrRegex has already been set to '%s'", this.atrRegex));
      }

      try {
        Pattern.compile(atrRegex);
      } catch (PatternSyntaxException exception) {
        throw new IllegalArgumentException(
            String.format("Invalid regular expression: '%s'.", atrRegex));
      }

      this.atrRegex = atrRegex;
      return this;
    }

    /**
     * Requests a DF Name-based filtering by defining in a byte array the AID that will be included
     * in the standard SELECT APPLICATION command sent to the card during the selection process.
     *
     * <p>The provided AID can be a right truncated image of the target DF Name (see ISO 7816-4
     * 4.2).
     *
     * @param aid A byte array containing {@value AID_MIN_LENGTH} to {@value AID_MAX_LENGTH} bytes.
     * @return The object instance.
     * @throws IllegalArgumentException If the provided array is null or out of range.
     * @throws IllegalStateException If this parameter has already been set.
     * @since 2.0
     */
    public Builder filterByDfName(byte[] aid) {

      Assert.getInstance()
          .notNull(aid, "aid")
          .isInRange(aid.length, AID_MIN_LENGTH, AID_MAX_LENGTH, "aid");

      if (this.aid != null) {
        throw new IllegalStateException(
            String.format("aid has already been set to '%s'", ByteArrayUtil.toHex(this.aid)));
      }

      this.aid = aid;
      return this;
    }

    /**
     * Requests a DF Name-based filtering by defining in a hexadecimal string the AID that will be
     * included in the standard SELECT APPLICATION command sent to the card during the selection
     * process.
     *
     * <p>The provided AID can be a right truncated image of the target DF Name (see ISO 7816-4
     * 4.2).
     *
     * @param aid A hexadecimal string representation of {@value AID_MIN_LENGTH} to {@value
     *     AID_MAX_LENGTH} bytes.
     * @return The object instance.
     * @throws IllegalArgumentException If the provided AID is null, invalid or out of range.
     * @throws IllegalStateException If this parameter has already been set.
     * @since 2.0
     */
    public Builder filterByDfName(String aid) {
      return filterByDfName(ByteArrayUtil.fromHex(aid));
    }

    /**
     * Sets the file occurrence mode (see ISO7816-4).
     *
     * <p>The default value is {@link FileOccurrence#FIRST}.
     *
     * @param fileOccurrence The {@link FileOccurrence}.
     * @return The object instance.
     * @throws IllegalArgumentException If fileOccurrence is null.
     * @throws IllegalStateException If this parameter has already been set.
     * @since 2.0
     */
    public Builder setFileOccurrence(FileOccurrence fileOccurrence) {
      Assert.getInstance().notNull(fileOccurrence, "fileOccurrence");
      if (this.fileOccurrence != null) {
        throw new IllegalStateException(
            String.format("fileOccurrence has already been set to '%s'", this.fileOccurrence));
      }
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
     * @throws IllegalStateException If this parameter has already been set.
     * @since 2.0
     */
    public Builder setFileControlInformation(FileControlInformation fileControlInformation) {
      Assert.getInstance().notNull(fileControlInformation, "fileControlInformation");
      if (this.fileControlInformation != null) {
        throw new IllegalStateException(
            String.format(
                "fileControlInformation has already been set to '%s'",
                this.fileControlInformation));
      }
      this.fileControlInformation = fileControlInformation;
      return this;
    }

    /**
     * Add a status code to be accepted to the list of successful select application status codes.
     *
     * @param statusCode The status code to be accepted.
     * @return The object instance.
     * @since 2.0
     */
    public Builder addSuccessfulStatusCode(int statusCode) {
      this.successfulSelectionStatusCodes.add(statusCode);
      return this;
    }

    /**
     * Creates an instance of {@link CardSelector}.
     *
     * @return A not null reference.
     * @since 2.0
     */
    public CardSelector build() {
      return new CardSelector(this);
    }
  }

  /**
   * Defines the navigation options through the different applications contained in the card
   * according to the ISO7816-4 standard.
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
   * Defines the type of templates available in return for the select command, according to the
   * ISO7816-4 standard.
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

  /**
   * (private)<br>
   * Created an instance of {@link CardSelector}.
   *
   * @param builder The {@link Builder}.
   */
  private CardSelector(Builder builder) {
    this.cardProtocol = builder.cardProtocol;
    this.atrRegex = builder.atrRegex;
    this.aid = builder.aid;
    this.fileOccurrence =
        builder.fileOccurrence == null ? FileOccurrence.FIRST : builder.fileOccurrence;
    this.fileControlInformation =
        builder.fileControlInformation == null
            ? FileControlInformation.FCI
            : builder.fileControlInformation;
    this.successfulSelectionStatusCodes = builder.successfulSelectionStatusCodes;
  }

  /**
   * Creates builder to build a {@link CardSelector}.
   *
   * @return Created builder.
   * @since 2.0
   */
  public static Builder builder() {
    return new Builder();
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
   * Gets the regular expression to be applied to the card's ATR.
   *
   * @return null if no ATR regex has been set.
   * @since 2.0
   */
  public String getAtrRegex() {
    return atrRegex;
  }

  /**
   * Gets the AID.
   *
   * @return null if no AID has been set.
   * @since 2.0
   */
  public byte[] getAid() {
    return aid;
  }

  /**
   * Gets the {@link FileOccurrence} parameter defining the navigation within the card applications
   * list.
   *
   * <p>The default value is {@link FileOccurrence#FIRST}.
   *
   * @return A not null reference.
   * @since 2.0
   */
  public FileOccurrence getFileOccurrence() {
    return fileOccurrence;
  }

  /**
   * Gets the {@link FileControlInformation} parameter defining the output type of the select
   * command.
   *
   * <p>The default value is {@link FileControlInformation#FCI}.
   *
   * @return A not null reference.
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
   * Add a status code to be accepted to the list of successful select application status codes.
   *
   * <p>The same as the one proposed by the {@link Builder}, this method can be used after the
   * creation of the {@link CardSelector}.
   *
   * @param statusCode The status code to be accepted.
   * @since 2.0
   */
  public void addSuccessfulStatusCode(int statusCode) {
    this.successfulSelectionStatusCodes.add(statusCode);
  }

  /**
   * Tells if the provided ATR matches the registered regular expression
   *
   * @param atr a buffer containing the ATR to be checked
   * @return true if the atrRegex is not set or if the ATR matches the current regex
   * @since 2.0
   */
  public boolean atrMatches(byte[] atr) {
    if (atrRegex != null) {
      return ByteArrayUtil.toHex(atr).matches(atrRegex);
    } else {
      return true;
    }
  }

  @Override
  public String toString() {
    return "CARD_SELECTOR = " + JsonUtil.toJson(this);
  }
}
