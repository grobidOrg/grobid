# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.9.0] - 2026-04-07

### Added
- Conflict of interest and author contributions statement extraction in header and segmentation models #1319
- Extract figures, tables and equations from back/annex sections #1215
- Extract URLs from PDF annotations in fulltext #1315
- Mark consolidated bibliographical references and header explicitly in TEI output #1313
- Include middle name and format initials in BibTeX output #1356
- Fetch ORCID from Crossref when not extracted by Grobid #1406
- Timeout configuration for consolidation requests (separate glutton and Crossref timeout) #1340
- Lingua as an alternative for language recognition #1239
- Blingfire as an alternative sentence segmentation engine #1378
- Native support for Linux ARM 64 architecture
- Multi-architecture Docker builds with ARM64 support (pdfalto and wapiti binaries for Linux ARM 64)
- Support for Python environment managers (virtualenv, conda) for DeLFT integration #1010
- Added version and revision information in the web UI #1390
- Added health status indicator with periodic updates in the web UI #1403
- Added more explanation and links to documentation in the web UI #1391
- More informative `/api/health` endpoint, failing early when models are partially initialised #1373
- `-modelPath` CLI argument for training and eval-mode model loading #1383, #1389
- Evaluation script for running end-to-end evaluation from the repository root
- Enabled trivy security code scanning #1295
- Updated Citation.cff and SWID metadata #1341

### Changed
- Revised and updated the Crossref integration, with better handling of API limits and errors, in collaboration with Crossref team #1398
- Upgraded to JDK 21 and Gradle 9 #1321
- Updated TensorFlow to 2.17 with Python 3.10-3.11 support #1188
- Updated pdfalto to 0.6.0
- Updated wapiti to 1.5.1
- Updated JEP to 4.3.1 #1332
- Updated DeLFT to > 0.4.1 in documentation and Dockerfiles #1400
- Updated JRuby to 9.4.12.1 and pragmatic segmenter #1293
- Updated Docker base images from deprecated openjdk to eclipse-temurin (21.0.10_7)
- Updated Dropwizard to address Trivy vulnerability in Docker image
- Updated grobid-lucene-analyzers #1346
- Updated dependency versions in build.gradle #1377
- Extensive model retraining: header, segmentation, fulltext, article-light, and article-light-ref models updated across CRF, BidLSTM_CRF_FEATURES, and BidLSTM_ChainCRF_FEATURES architectures
- Significant expansion of training data for segmentation, fulltext, header, name, and affiliation-address models
- Refactored training framework for clearer extensibility #1393
- Updated benchmark results #1392
- Removed obsolete and unused models #1367
- Enhanced documentation structure and clarity for newcomers #1310, #1382
- Return XML by default when no HTTP Accept header is provided #1405
- CI speed-up #1374

### Fixed
- Figures, tables and equations identifier uniqueness and overlapping IDs in body and annex #1342
- IndexOutOfBoundException in ORCID search by annotation #1369
- Missing logic to correctly get conflicts and credits in the output TEI
- BibTeX index bug #1409
- Revision link format in the web UI #1404
- German wordforms failing to load in the Lexicon #1362
- Honour instance-level Wapiti params in `train()` #1383
- Evaluation script now works from the repository root
- Docker build crash caused by dynamic Python environment version fetching #1348
- Dockerfile for ARM Linux #1395
- Full Docker image build restored #1371
- `preload_embeddings.py` crash when download directory doesn't exist
- Security-oriented regex improvements #1366
- Coveralls build and Gradle deprecations #1347
- Numerous training data corrections across all models

## [0.8.2] - 2025-05-11

### Added
- New model specialization/variants (flavors) mechanism #1151
- Specialization/variant process for a lightweight processing that covers other types of scientific articles that are not following the general segmentation schema (e.g., corrections, editorial letters, etc.) #1202
- Additional training data covering additional cases where the Data Availability statements are over multiple pages #1200
- Added a flag that allows output the raw copyright information in TEI #1181
- New docker container for running end-to-end evaluation #1255
- New Grobid client in Go #1159
- Make the start/end page for header processing customizable #282
- Return configuration processing parameters in TEI XML response header #1274 

### Changed
- Update pdfalto recognition of non-standard fonts #1216
- Revert text that does not belong to graphics as paragraphs instead of dropping it #1266
- Updated Grobid lucene analyzers for CJK languages #1228

### Fixed
- Fix URL identification for certain edge cases #1190, #1191, #1185
- Fix fulltext model training data #1107 
- Fix header model training data #1128
- Updated the docker image's packages to reduce the vulnerabilities #1173
- Fixed a bug in the handling of badly formatted figures/tables #1207
- Correct replacement in the filenames of the fulltext generated files #1204
- Fixed full-text block start #1203
- Fix affiliation missing when using DL affiliation-address model #1166
- Fixed various security vulnerabilities #1125 #1123 #1205
- Avoid NPE when iterating over annotations that might have null bounding Boxes #1194


## [0.8.1] - 2024-09-14

### Added
 - Identified URLs are now added in the TEI output #1099
 - Added DL models for patent processing #1082
 - Copyrights owner and licenses identification models #1078 
 - Add research infrastructure recognition for funding processing #1085
 - Add paragraphs coordinates in the TEI output #1068
 - Specify the configuration file with DL models enabled for the full docker image #1117
 - Support for biblio-glutton 0.3 #1086

### Changed
 - Update affiliation process #1069
 - Improved the recognition of URLs using (when available) PDF annotations, such as clickable links
 - Updated TEI schema #1084
 - Review patent process #1082
 - Add Kotlin language to support development and testing #1096

### Fixed
 - Avoid splitting URLs between sentences #1097
 - Add missing sentence segmentation in funding and acknowledgement #1106
 - Docker image was optimized to reduce the needed space #1088
 - Fixed OOBE when processing large quantities of notes #1075
 - Corrected `<title>` coordinate attribute name #1070
 - Fix missing coordinates in paragraph continuation #1076
 - Fixed JSON log output
 - Fixed notes identification #1124
 - Fixed extraneous semicolon in the training data #1133
 - Reduced security vulnerabilities in the dependencies #1136 #1137

## New Contributors
* @tanaynayak made their first contribution in https://github.com/kermitt2/grobid/pull/1133
* @vipulg13 made their first contribution in https://github.com/kermitt2/grobid/pull/1137

## [0.8.0] - 2023-11-19

### Added

+ Extraction of funder and funding information with a specific new model, see https://github.com/kermitt2/grobid/pull/1046 for details
+ Optional consolidation of funder with CrossRef Funder Registry
+ Identification of acknowledged entities in the acknowledgement section
+ Optional coordinates in title elements

### Changed

+ Dropwizard upgrade to 4.0
+ Minimum JDK/JVM requirement for building/running the project is now 1.11
+ Logging now with Logback, removal of Log4j2, optional logs in json format
+ General review of logs
+ Enable Github actions / Disable circleci

### Fixed

+ Set dynamic memory limit in pdfalto_server #1038 
+ Logging in files when training models work now as expected
+ Various dependency upgrades
+ Fix #1051 with possible problematic PDF
+ Fix #1036 for pdfalto memory limit 
+ fix readthedocs build #1040 
+ fix for null equation #1030
+ Other minor fixes

## [0.7.3] – 2023-05-13

### Added

+ Support for JDK beyond 1.11, tested up to Java 17, thanks to removal of dynamic native library loading after the start of the JVM
+ Incremental training (all models and ML engines), add this option in training command line and training web service (#971)
+ Systematic benchmarking on two new sets: PLOS (1000 artilces) and eLife (984 articles)
+ All end-to-end evaluation datasets are now available from the same place: https://zenodo.org/record/7708580
+ Option to output coordinates in notes and figure/table captions
+ Support for Mac ARM architecture (#975)
+ Play With Docker documentation (#962)

### Changed

+ Update to DeLFT version 0.3.3
+ Demo now hosted as HuggingFace space
+ Additional training data, in particular for citation, reference-segmenter, segmentation, header, etc. 
+ Update Deep Learning models (and some of the CRF)
+ The standard analyzer for sub-lexical tokenization is available in grobid-core, and used for the citation model (in particular for improving CJK references) (#990)
+ Update evaluations

### Fixed

+ Correct wrong content type in doc for processCitation web service
+ Sentence segmentation applied to notes (#995)
+ Other minor fixes

## [0.7.2] – 2022-10-29

### Added

+ Explicit identification of data/code availability statements (#951) and funding statements (#959), including when they are located in the header
+ Link footnote and their "callout" marker in full text (#944)
+ Option to consolidate header only with DOI if a DOI is extracted (#742)
+ "Window" application of RNN model for reference-segmenter to cover long bibliographical sections
+ Add dynamic timeout on pdfalto_server (#926) 
+ A modest Python script to help to find "interesting" error cases in a repo of JATS/PDF pairs, grobid-home/scripts/select_error_cases.py

### Changed

+ Update to DeLFT version 0.3.2
+ Some more training data (authors in reference, segmentation, citation, reference-segmenter) (including #961, #864)
+ Update of some models, RNN with feature channels and CRF (segmentation, header, reference-segmenter, citation)
+ Review guidelines for segmentation model
+ Better URL matching, using in particular PDF URL annotation in account

### Fixed

+ Fix unexpected figure and table labeling in short texts
+ When matching an ORCID to an author, prioritize Crossref info over extracted ORCID from the PDF (#838)
+ Annotation errors for acknowledgement and other minor stuff
+ Fix for Python library loading for Mac
+ Update docker file to support new CUDA key
+ Do not dehyphenize text in superscript or subscript
+ Allow absolute temporary paths
+ Fix redirected stderr from pdfalto not "gobbled" by the java ProcessBuilder call (#923)
+ Other minor fixes

## [0.7.1] – 2022-04-16

### Added

+ Web services for training models (#778)
+ Some additional training data for bibliographical references from arXiv
+ Add a web service to process a list of reference strings, see https://grobid.readthedocs.io/en/processcitationlist/Grobid-service/#apiprocesscitationlist
+ Extended processHeaderDocument to get result in bibTeX

### Changed

+ Update to DeLFT version to 0.3.1 and TensorFlow 2.7, with many improvements, see https://github.com/kermitt2/delft/releases/tag/v0.3.0
+ Update of Deep Learning models
+ Update of JEP and add install script
+ Update to new biblio-glutton version 0.2, for improved and faster bibliographical reference matching
+ circleci to replace Travis
+ Update of processFulltextAssetDocument service to use the same parameters as processFulltextDocument
+ Pre-compile regex if not already done
+ Review features for header model

### Fixed

+ Improved date normalization (#760)
+ Fix possible issue with coordinates related to reference markers (#908) and sentence (#811)
+ Fix path to bitmap/vector graphics (#836)
+ Fix possible catastrophic regex backtracking (#867)
+ Other minor fixes

## [0.7.0] – 2021-07-17

### Added

+ New YAML configuration: all the settings are in one single yaml file, each model can be fully configured independently
+ Improvement of the segmentation and header models (for header, +1 F1-score for PMC evaluation, +4 F1-score for bioRxiv), improvements for body and citations 
+ Add figure and table pop-up visualization on PDF in the console demo
+ Add PDF MD5 digest in the TEI results (service only)
+ Language support packages and xpdfrc file for pdfalto (support of CJK and exotic fonts)
+ Prometheus metrics 
+ BidLSTM-CRF-FEATURES implementation available for more models
+ Addition of a "How GROBID works" page in the documentation

### Changed

+ JitPack release (RIP jcenter)
+ Improved DOI cleaning 
+ Speed improvement (around +10%), by factorizing some layout token manipulation
+ Update CrossRef requests implementation to align to the current usage of CrossRef's `X-Rate-Limit-Limit` response parameter

### Fixed

+ Fix base url in demo console
+ Add missing pdfalto Graphics information when `-noImage` is used, fix graphics data path in TEI
+ Fix the tendency to merge tables when they are in close proximity

## [0.6.2] – 2021-03-20

### Added

+ Docker image covering both Deep Learning and CRF models, with GPU detection and preloading of embeddings
+ For Deep Learning models, labeling is now done by batch: application of the citation DL model is 4 times faster for BidLSTM-CRF (with or without features) and 6 times faster for SciBERT
+ More tests for sentence segmentation
+ Add orcid of persons when available from the PDF or via consolidation (i.e. if in CrossRef metadata) 
+ Add BidLSTM-CRF-FEATURES header model (with feature channel)
+ Add bioRxiv end-to-end evaluation
+ Bounding boxes for optional section titles coordinates

### Changed

+ Reduce the size of docker images 
+ Improve end-to-end evaluation: multithreaded processing of PDF, progress bar, output the evaluation report in markdown format
+ Update of several models covering CRF, BidLSTM-CRF and BidLSTM-CRF-FEATURES, mainly improving citation and author recognitions
+ OpenNLP is the default optional sentence segmenter (similar result as Pragmatic Segmenter for scholar documents after benchmarking, but 30 times faster)
+ Refine sentence segmentation to exploit layout information and predicted reference callouts
+ Update jep version to 3.9.1

### Fixed

+ Ignore invalid utf-8 sequences
+ Update CrossRef multithreaded calls to avoid using the unreliable time interval returned by the CrossRef REST API service, update usage of `Crossref-Plus-API-Token` and update the deprecated crossref field `query.title`
+ Missing last table or figure when generating training data for the fulltext model
+ Fix an error related to the feature value for the reference callout for the fulltext model
+ Review/correct DeLFT configuration documentation, with a step-by-step configuration documentation
+ Other minor fixes

## [0.6.1] – 2020-08-12

### Added

+ Support of line number (typically in preprints)
+ End-to-end evaluation and benchmark for preprints using the bioRxiv 10k dataset 
+ Check whether PDF annotation is orcid and add orcid to author in the TEI result
+ Configuration for making sequence labeling engine (CRF Wapiti or Deep Learning) specific to models
+ Add a developers guide and a FAQ section in the documentation
+ Visualization of formulas on PDF layout in the demo console
+ Feature for subscript/superscript style in fulltext model

### Changed

+ New significantly improved header model: with new features, new training data (600 new annotated examples, old training data is entirely removed), new labels and updated data structures in line with the other models
+ Update of the segmentation models with more training data
+ Removal of heuristics related to the header
+ Update to gradle 6.5.1 to support JDK 13 and 14
+ TEI schemas 
+ Windows is not supported in this release

### Fixed

+ Preserve affiliations after consolidation of the authors 
+ Environment variable config override for all properties 
+ Unfrequent duplication of the abstract in the TEI result
+ Incorrect merging of affiliations
+ Noisy parentheses in the bibliographical reference markers
+ In the console demo, fix the output filename wrongly taken from the input form when the text form is used
+ Synchronisation of the language detection singleton initialisation in case of multithread environment
+ Other minor fixes

## [0.6.0] – 2020-04-24

### Added

+ Table content structuring (thanks to @Vitaliy-1), see [PR #546](https://github.com/kermitt2/grobid/pull/546)
+ Support for `application/x-bibtex` at `/api/processReferences` and `/api/processCitation` (thanks to @koppor)
+ Optionally include raw affiliation string in the TEI result
+ Add dummy model for facilitating test in Grobid modules
+ Allow environment variables for config properties values to ease Docker config 
+ ChangeLog

### Changed

+ Improve CORS configuration #527 (thank you @lfoppiano)
+ Documentation improvements
+ Update of segmentation and fulltext model and training data
+ Better handling of affiliation block fragments
+ Improved DOI string recognition
+ More robust n-fold cross validation (case of shared grobid-home)

### Fixed

+ Fixed flags of pdf2xml in `Dockerfile`
+ Some fixes for better TEI result format conformance 
+ Other minor fixes

## [0.5.6] – 2019-10-16

### Added

+ n-fold cross evaluation and better evaluation report (thanks to @lfoppiano)

### Changed

+ Better abstract structuring (with citation contexts)
+ Improved PMC ID and PMID recognition
+ Improved subscript/superscript and font style recognition (via [pdfalto](https://github.com/kermitt2/pdfalto))
+ Improved JEP integration (support of python virtual environment for using DeLFT Deep Learning library, thanks @de-code and @lfoppiano)
+ Improved dehyphenization (thanks to @lfoppiano)

### Fixed

+ Several bug fixes (thanks @de-code, @bnewbold, @Vitaliy-1 and @lfoppiano)

## [0.5.5] – 2019-05-29

### Changed

+ Using [pdfalto](https://github.com/kermitt2/pdfalto) instead of pdf2xml for the first PDF parsing stage, with many improvements in robustness, ICU support, unknown glyph/font normalization (thanks in particular to @aazhar)
+ Improvement and full review of the integration of consolidation services, supporting [biblio-glutton](https://github.com/kermitt2/biblio-glutton) (additional identifiers and Open Access links) and [Crossref REST API](https://github.com/CrossRef/rest-api-doc) (add specific user agent, email and token for Crossref Metadata Plus)
+ Updated lexicon #396

### Fixed

+ Fix bounding box issues for some PDF #330

## [0.5.4] – 2019-02-12

### Added

+ Transparent usage of [DeLFT](https://github.com/kermitt2/delft) deep learning models (BidLSTM-CRF/ELMo) instead of Wapiti CRF models, native integration via [JEP](https://github.com/ninia/jep)
+ Support of [biblio-glutton](https://github.com/kermitt2/biblio-glutton) as DOI/metadata matching service, alternative to crossref REST API

### Changed

+ Improvement of citation context identification and matching (+9% recall with similar precision, for PMC sample 1943 articles, from 43.35 correct citation contexts per article to 49.98 correct citation contexts per article)
+ Citation callout now in abstract, figure and table captions
+ Structured abstract (including update of TEI schema)

### Fixed

+ Bug fixes and some more parameters: by default using all available threads when training (thanks [@de-code](https://github.com/de-code)) and possibility to load models at the start of the service

## [0.5.3] – 2018-11-25

### Added

+ Support of proxy for calling crossref with Apache HttpClient

### Changed

+ Improvement of consolidation options and processing (better handling of CrossRef API, but the best is coming soon ;)
+ Better recall for figure and table identification (thanks to @detonator413)

### Fixed

+ Minor bug fixing

## [0.5.2] – 2018-10-17

### Added

+ Added [Grobid clients](https://grobid.readthedocs.io/en/latest/Grobid-service/#clients-for-grobid-web-services) for Java, Python and NodeJS
+ Added metrics in the REST entrypoint (accessible via <http://localhost:8071>)
+ Added counters for consolidation tasks and consolidation results
+ Added case sensitiveness option in lexicon/FastMatcher

### Changed

+ Updated documentation

### Fixed

+ Corrected back status codes from the REST API when no available engine (503 is back again to inform the client to wait, it was removed by error in version 0.5.0 and 0.5.1 for PDF processing services only, see documentation of the REST API)
+ Bugfixing #339, #322, #300, and others

## [0.5.1] – 2018-01-29

### Fixed

+ Various bug fixes

## [0.5.0] – 2017-11-09

### Changed

+ Migrate from maven to gradle for faster, more flexible and more stable build, release, etc.
+ Usage of Dropwizard for web services
+ Move the Grobid service manual to [readthedocs](http://grobid.readthedocs.io/en/latest/Grobid-service/)
+ (thanks to @detonator413 and @lfoppiano for this release! future work in versions 0.5.* will focus again on improving PDF parsing and structuring accuracy)

## [0.4.4] – 2017-10-13

### Fixed

+ Fixed issue that was making the release build not working

## [0.4.3] – 2017-10-07

### Added

+ New training data and features for bibliographical references, in particular for covering HEP domain (INSPIRE), arXiv identifier, DOI and url (thanks @iorala and @michamos !)
+ Support for CrossRef REST API (instead of the slow OpenURL-style API which requires a CrossRef account), in particular for multithreading usage (thanks @Vi-dot)
+ Unicode normalisation and more robust body extraction (thanks @aoboturov)

### Changed

+ Updated models: f-score improvement on the PubMed Central sample, bibliographical references +2.5%, header +7%  
+ Improve training data generation and documentation (thanks @jfix)
+ Update of the pdf2xml fork for Windows (thanks @lfoppiano)

### Fixed

+ fixes, tests, documentation

## [0.4.2] – 2017-08-05

### Added

+ Identification of equations (with PDF coordinates)
+ End-to-end evaluation with Pub2TEI conversions

### Changed

+ f-score improvement for the PubMed Central sample: fulltext +10-14%, header +0.5%, citations +0.5%
+ More robust PDF parsing

### Fixed

+ many fixes and refactoring

## [0.4.1] – 2016-10-02

### Added

+ Support for Windows thanks to the contributions of Christopher Boumenot!
+ Support for Docker
+ New web services for PDF annotation and updated web console application

### Changed

+ Some improvements on figure/table extraction - but still experimental at this stage (work in progress, as the whole full text model)

### Fixed

+ Fixes and refactoring.

## [0.4.0] – 2016-10-02

### Changed

+ Improvement of the recognition of citations thanks to refinements of CRF features - +4% in f-score for the PubMed Central sample.
+ Improvement of the full text model, with new features and the introduction of two additional models for figures and tables.
+ More robust synchronization of CRF sequence with PDF areas, resulting in improved bounding box calculations for locating annotations in the PDF documents.
+ Improved general robustness thanks to better token alignments.

[Unreleased]: https://github.com/kermitt2/grobid/compare/0.7.0...HEAD
[0.7.0]: https://github.com/kermitt2/grobid/compare/0.6.2...0.7.0
[0.6.2]: https://github.com/kermitt2/grobid/compare/0.6.1...0.6.2
[0.6.1]: https://github.com/kermitt2/grobid/compare/0.6.0...0.6.1
[0.6.0]: https://github.com/kermitt2/grobid/compare/0.5.6...0.6.0
[0.5.6]: https://github.com/kermitt2/grobid/compare/0.5.5...0.5.6
[0.5.5]: https://github.com/kermitt2/grobid/compare/0.5.4...0.5.5
[0.5.4]: https://github.com/kermitt2/grobid/compare/0.5.3...0.5.4
[0.5.3]: https://github.com/kermitt2/grobid/compare/0.5.2...0.5.3
[0.5.2]: https://github.com/kermitt2/grobid/compare/0.5.1...0.5.2
[0.5.1]: https://github.com/kermitt2/grobid/compare/0.5.0...0.5.1
[0.5.0]: https://github.com/kermitt2/grobid/compare/grobid-parent-0.4.4...0.5.0
[0.4.4]: https://github.com/kermitt2/grobid/compare/grobid-parent-0.4.3...grobid-parent-0.4.4
[0.4.3]: https://github.com/kermitt2/grobid/compare/grobid-parent-0.4.2...grobid-parent-0.4.3
[0.4.2]: https://github.com/kermitt2/grobid/compare/grobid-parent-0.4.1...grobid-parent-0.4.2
[0.4.1]: https://github.com/kermitt2/grobid/compare/grobid-parent-0.4.0...grobid-parent-0.4.1
[0.4.0]: https://github.com/kermitt2/grobid/compare/grobid-parent-0.3.9...grobid-parent-0.4.0

<!-- markdownlint-disable-file MD024 MD033 -->
