# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- The possibility to import/export a card selection scenario.
### Fixed
- Logging format for distributed service.
### Upgraded
- "Calypsonet Terminal Reader API" to version `1.1.0`.
- "Keyple Util Library" to version `2.2.0`.

## [2.1.0] - 2022-07-25
### Added
- `SmartCardService.getPlugin` from a `CardReader` reference. 
- `SmartCardService.getReader` from a `CardReader` name.
- `Plugin.getReaderExtension` to access the reader's extension class.
### Deprecated
- `Reader` in favor of `CardReader` from the "Calypsonet Terminal Reader API".
- `ObservableReader` in favor of `ObservableCardReader` from the "Calypsonet Terminal Reader API".
- `ConfigurableReader` in favor of `ConfigurableCardReader` from the "Calypsonet Terminal Reader API".
- `ReaderEvent` in favor of `CardReaderEvent` from the "Calypsonet Terminal Reader API".
### Fixed
- Auto management of 61XX and 6CXX status words and case 4 commands (Calypsonet Terminal Requirements: `RL-SW-61XX.1`, `RL-SW-6CXX.1`, `RL-SW-ANALYSIS.1` and `RL-SW-CASE4.1`) (issue [#37]).
- Returned value of `getActiveSmartCard` method when there is no active smart card (issue [#40]).
- JSON serialization for interfaces in objects trees (issue [#43]).
- No longer clear the selection requests after processing the card selection.
- Closing the physical channel when unregistering a reader.
### Upgraded
- "Keyple Util Library" to version `2.1.0` by removing the use of deprecated methods.

## [2.0.1] - 2021-12-08
### Added
- `CHANGELOG.md` file (issue [eclipse/keyple#6]).
- CI: Forbid the publication of a version already released (issue [#34]).
### Fixed
- Logical channel management for multiple selections (issue [#38]).

## [2.0.0] - 2021-10-06
This is the initial release.
It follows the extraction of Keyple 1.0 components contained in the `eclipse/keyple-java` repository to dedicated repositories.
It also brings many major API changes.

[unreleased]: https://github.com/eclipse/keyple-service-java-lib/compare/2.1.0...HEAD
[2.1.0]: https://github.com/eclipse/keyple-service-java-lib/compare/2.0.1...2.1.0
[2.0.1]: https://github.com/eclipse/keyple-service-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/eclipse/keyple-service-java-lib/releases/tag/2.0.0

[#43]: https://github.com/eclipse/keyple-service-java-lib/issues/43
[#40]: https://github.com/eclipse/keyple-service-java-lib/issues/40
[#38]: https://github.com/eclipse/keyple-service-java-lib/issues/38
[#37]: https://github.com/eclipse/keyple-service-java-lib/issues/37
[#34]: https://github.com/eclipse/keyple-service-java-lib/issues/34

[eclipse/keyple#6]: https://github.com/eclipse/keyple/issues/6