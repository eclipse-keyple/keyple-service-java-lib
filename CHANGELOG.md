# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Upgraded
- Keyple Plugin API `2.2.0` -> `2.3.0` (remains compliant with plugins using the version `2.2.0` and `2.1.0`)

## [2.3.2] - 2023-11-13
### Fixed
- CI: code coverage report when releasing.
### Added
- Added project status badges on `README.md` file.
### Changed
- Reduced monitoring cycle for observable readers implementing non-blocking insertion/removal states 
(100 ms instead of 200 ms).
### Upgraded
- Keyple Plugin API `2.1.0` -> `2.2.0` (remains compliant with plugins using the version `2.1.0`)
- Keyple Util Library `2.3.0` -> `2.3.1` (code source not impacted)

## [2.3.1] - 2023-05-30
### Fixed
- Fixes an issue with exception handling in the `WAIT_FOR_CARD_REMOVAL` state of the observable reader state machine 
that blocked the state machine in the same state.
- Fixes a performance issue introduced in version `2.1.3` related to the closing of the physical channel when processing 
a new card selection scenario with the same card (issue [#58]).

## [2.3.0] - 2023-05-22
### Upgraded
- Calypsonet Terminal Reader API `1.2.0` -> `1.3.0`.
  Introduced a new capability to export a locally processed card selection scenario to be imported and analyzed remotely
  by another card selection manager.
  For this purpose, the following two methods have been added to the `CardSelectionManager` interface:
  - `exportProcessedCardSelectionScenario`
  - `importProcessedCardSelectionScenario`

## [2.2.1] - 2023-05-05
### Fixed
- Fixes the communication issue between client and server components of the "Distributed" solution that appeared with 
  version `2.1.4`.

## [2.2.0] - 2023-04-25
:warning: **CAUTION**: When using the "Distributed" solution with pool plugins, it is necessary to use at least this 
version on the client and server side.
### Added
- The `PoolPlugin.getSelectedSmartCard` method to retrieve the smart card if it has been automatically selected by the 
  reader allocation process.
### Upgraded
- "Keyple Plugin API" to version `2.1.0`

## [2.1.4] - 2023-04-04
### Changed
- Objects transmitted through the network for "Distributed" solution are now serialized/de-serialized
  as JSON objects, and no more as strings containing JSON objects.
- All JSON property names are now "lowerCamelCase" formatted.

## [2.1.3] - 2023-02-17
### Fixed
- Management of the physical channel when chaining multiple selection scenarios.
### Upgraded
- "Keyple Distributed Remote API" to version `2.1.0`
- "Google Gson Library" (com.google.code.gson) to version `2.10.1`.

## [2.1.2] - 2023-01-10
### Upgraded
- "Calypsonet Terminal Reader API" to version `1.2.0`.
- "Keyple Util Library" to version `2.3.0`.

## [2.1.1] - 2022-10-26
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
- Auto management of 61XX and 6CXX status words and case 4 commands (Calypsonet Terminal Requirements: `RL-SW-61XX.1`, 
  `RL-SW-6CXX.1`, `RL-SW-ANALYSIS.1` and `RL-SW-CASE4.1`) (issue [#37]).
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
It follows the extraction of Keyple 1.0 components contained in the `eclipse/keyple-java` repository to dedicated 
repositories.
It also brings many major API changes.

[unreleased]: https://github.com/eclipse/keyple-service-java-lib/compare/2.3.2...HEAD
[2.3.2]: https://github.com/eclipse/keyple-service-java-lib/compare/2.3.1...2.3.2
[2.3.1]: https://github.com/eclipse/keyple-service-java-lib/compare/2.3.0...2.3.1
[2.3.0]: https://github.com/eclipse/keyple-service-java-lib/compare/2.2.1...2.3.0
[2.2.1]: https://github.com/eclipse/keyple-service-java-lib/compare/2.2.0...2.2.1
[2.2.0]: https://github.com/eclipse/keyple-service-java-lib/compare/2.1.4...2.2.0
[2.1.4]: https://github.com/eclipse/keyple-service-java-lib/compare/2.1.3...2.1.4
[2.1.3]: https://github.com/eclipse/keyple-service-java-lib/compare/2.1.2...2.1.3
[2.1.2]: https://github.com/eclipse/keyple-service-java-lib/compare/2.1.1...2.1.2
[2.1.1]: https://github.com/eclipse/keyple-service-java-lib/compare/2.1.0...2.1.1
[2.1.0]: https://github.com/eclipse/keyple-service-java-lib/compare/2.0.1...2.1.0
[2.0.1]: https://github.com/eclipse/keyple-service-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/eclipse/keyple-service-java-lib/releases/tag/2.0.0

[#58]: https://github.com/eclipse/keyple-service-java-lib/issues/58
[#43]: https://github.com/eclipse/keyple-service-java-lib/issues/43
[#40]: https://github.com/eclipse/keyple-service-java-lib/issues/40
[#38]: https://github.com/eclipse/keyple-service-java-lib/issues/38
[#37]: https://github.com/eclipse/keyple-service-java-lib/issues/37
[#34]: https://github.com/eclipse/keyple-service-java-lib/issues/34

[eclipse/keyple#6]: https://github.com/eclipse/keyple/issues/6