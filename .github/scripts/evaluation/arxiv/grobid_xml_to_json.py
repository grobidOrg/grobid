import sys
import json
import logging
import argparse
import re
from pathlib import Path
import xml.etree.ElementTree as ET
from typing import Dict, List, Optional, Any


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

TEI_NS = {"tei": "http://www.tei-c.org/ns/1.0"}

# One or more affiliation labels at the start of a raw affiliation: up to 3 digits, a single
# letter not followed by another letter, or a footnote symbol (incl. mathematical italic letters),
# each optionally bracketed and followed by punctuation. Longer numbers (street numbers) are kept.
_LEADING_MARKERS = re.compile(
    r"^(?:\s*[(\[]?\s*(?:(?:\d{1,3}|[^\W\d_])(?=[\s.,;:)\]]|$)|[*†‡§¶#⋆∗♠♦♣♥]|[\U0001D400-\U0001D7FF])"
    r"\s*[)\]]?\s*[.,;:]?)+\s*"
)


def normalize_arxiv_id(arxiv_id: str) -> str:
    """Return a normalized identifier suitable for lookup comparisons."""
    if not arxiv_id:
        return arxiv_id

    cleaned = arxiv_id.strip()
    if cleaned.lower().startswith('arxiv:'):
        cleaned = cleaned[6:]

    if '_' in cleaned and '/' not in cleaned:
        prefix, suffix = cleaned.split('_', 1)
        if prefix and suffix:
            cleaned = f"{prefix}/{suffix}"

    match = re.search(r'(\d{4}\.\d{4,5}(v\d+)?|[a-z\-]+/\d{7}(v\d+)?)', cleaned, re.IGNORECASE)
    if match:
        return match.group(1)

    return cleaned


def build_ground_truth_id_lookup(ground_truth_path: Optional[Path]) -> Dict[str, str]:
    """Create a mapping of normalized arXiv IDs to their canonical form."""
    if not ground_truth_path:
        return {}

    if not ground_truth_path.exists():
        logger.warning("Ground truth file %s not found; continuing without ID remapping", ground_truth_path)
        return {}

    try:
        with open(ground_truth_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as exc:
        logger.warning("Failed to load ground truth file %s (%s); continuing without ID remapping", ground_truth_path, exc)
        return {}

    lookup: Dict[str, str] = {}

    def _ingest_doc(doc: Any) -> None:
        if not isinstance(doc, dict):
            return
        arxiv_id = doc.get('arxiv_id')
        if not arxiv_id:
            return
        normalized = normalize_arxiv_id(str(arxiv_id))
        if normalized and normalized not in lookup:
            lookup[normalized] = str(arxiv_id)

    if isinstance(data, list):
        for entry in data:
            _ingest_doc(entry)
    elif isinstance(data, dict):
        if 'documents' in data and isinstance(data['documents'], list):
            for entry in data['documents']:
                _ingest_doc(entry)
        else:
            _ingest_doc(data)

    return lookup


class GrobidXMLParser:
    def __init__(self, xml_content: str, affiliation_mode: str = 'hybrid'):
        """
        Initialize the GROBID XML parser.

        Args:
            xml_content: The XML content to parse
            affiliation_mode: How to parse affiliations ('structured', 'raw', or 'hybrid')
                - 'structured': Only use structured sub-elements
                - 'raw': Only use raw_affiliation note content
                - 'hybrid': Try structured first, fall back to raw (default, preserves original behavior)
        """
        self.xml_content = xml_content
        self.root = None
        self.affiliations_dict = {}
        self.affiliation_mode = affiliation_mode

        if affiliation_mode not in ['structured', 'raw', 'hybrid']:
            raise ValueError(f"Invalid affiliation_mode: {affiliation_mode}. Must be 'structured', 'raw', or 'hybrid'")

    def parse(self) -> List[Dict[str, Any]]:
        try:
            self.root = ET.fromstring(self.xml_content)
        except ET.ParseError as e:
            logger.error(f"Failed to parse XML: {e}")
            return []

        self.tei_header = self.root.find("tei:teiHeader", TEI_NS)
        if self.tei_header is None:
            logger.warning("No teiHeader element found in XML")
            return []

        self._build_affiliations_dict()

        return self._parse_authors()

    def _build_affiliations_dict(self) -> None:
        for affiliation in self.tei_header.findall(".//tei:affiliation[@key]", TEI_NS):
            key = affiliation.get("key")
            if key:
                self.affiliations_dict[key] = self._parse_affiliation(
                    affiliation)
                logger.debug(f"Found affiliation {key}: {self.affiliations_dict[key]}")

    def _parse_affiliation(self, affiliation_elem) -> str:
        """
        Parse affiliation based on the configured mode.

        Args:
            affiliation_elem: The affiliation XML element to parse

        Returns:
            The parsed affiliation string
        """
        if self.affiliation_mode == 'raw':
            # Only use raw affiliation
            return self._extract_raw_affiliation(affiliation_elem)
        elif self.affiliation_mode == 'structured':
            # Only use structured elements
            return self._extract_structured_affiliation(affiliation_elem)
        else:  # hybrid mode
            # Try structured first, fall back to raw
            structured = self._extract_structured_affiliation(affiliation_elem)
            if structured:
                return structured

            raw = self._extract_raw_affiliation(affiliation_elem)
            if raw:
                return raw

            # Last resort: direct text content
            text_content = affiliation_elem.text
            if text_content:
                return text_content.strip()
            return ""

    def _extract_raw_affiliation(self, affiliation_elem) -> str:
        """Extract raw affiliation from note element."""
        raw_note = affiliation_elem.find(
            "tei:note[@type='raw_affiliation']", TEI_NS)
        if raw_note is not None:
            # Get all text content including from child elements
            raw_text = ''.join(raw_note.itertext()).strip()

            if raw_text:
                # Strip leading labels: "1", "12", "a)", "*", "†", "(1)", "1, 2" ...
                return _LEADING_MARKERS.sub("", raw_text).strip()
        return ""

    def _extract_structured_affiliation(self, affiliation_elem) -> str:
        """Extract and join structured affiliation elements."""
        parts = []

        # Department
        dept = affiliation_elem.findtext(
            "tei:orgName[@type='department']", namespaces=TEI_NS)
        if dept:
            parts.append(dept.strip())

        # Institution
        inst = affiliation_elem.findtext(
            "tei:orgName[@type='institution']", namespaces=TEI_NS)
        if inst:
            parts.append(inst.strip())

        # Laboratory
        lab = affiliation_elem.findtext(
            "tei:orgName[@type='laboratory']", namespaces=TEI_NS)
        if lab:
            parts.append(lab.strip())

        # Address components
        addr = affiliation_elem.find("tei:address", TEI_NS)
        if addr is not None:
            addr_line = addr.findtext("tei:addrLine", namespaces=TEI_NS)
            if addr_line:
                parts.append(addr_line.strip())

            settlement = addr.findtext("tei:settlement", namespaces=TEI_NS)
            if settlement:
                parts.append(settlement.strip())

            region = addr.findtext("tei:region", namespaces=TEI_NS)
            if region:
                parts.append(region.strip())

            post_code = addr.findtext("tei:postCode", namespaces=TEI_NS)
            if post_code:
                parts.append(post_code.strip())

            country_elem = addr.find("tei:country", TEI_NS)
            if country_elem is not None:
                country_text = country_elem.text
                if not country_text:
                    country_text = country_elem.get("key", "")
                if country_text:
                    parts.append(country_text.strip())

        return ", ".join(parts) if parts else ""

    def _parse_authors(self) -> List[Dict[str, Any]]:
        authors = []

        for author_elem in self.tei_header.findall(".//tei:author", TEI_NS):
            author_info = self._parse_single_author(author_elem)
            if author_info:
                authors.append(author_info)

        return authors

    def _parse_single_author(self, author_elem) -> Optional[Dict[str, Any]]:
        pers_name = author_elem.find("tei:persName", TEI_NS)
        if pers_name is None:
            return None
        forenames = []
        for forename_elem in pers_name.findall("tei:forename", TEI_NS):
            if forename_elem.text:
                forenames.append(forename_elem.text.strip())

        surname = pers_name.findtext(
            "tei:surname", default="", namespaces=TEI_NS).strip()

        if not surname and not forenames:
            return None

        full_name = self._format_name(forenames, surname)

        affiliations = self._get_author_affiliations(author_elem)

        return {
            "name": full_name,
            "affiliations": affiliations
        }

    def _format_name(self, forenames: List[str], surname: str) -> str:
        if not forenames:
            return surname

        if len(forenames) == 1:
            return f"{forenames[0]} {surname}"
        elif len(forenames) == 2:
            return f"{forenames[0]} {forenames[1][0]} {surname}"
        else:
            name_parts = [forenames[0]]
            for fname in forenames[1:]:
                if fname:
                    name_parts.append(fname[0])
            name_parts.append(surname)
            return " ".join(name_parts)

    def _get_author_affiliations(self, author_elem) -> List[str]:
        affiliations = []

        for aff_elem in author_elem.findall("tei:affiliation", TEI_NS):
            aff_key = aff_elem.get("key")
            if aff_key and aff_key in self.affiliations_dict:
                aff_text = self.affiliations_dict[aff_key]
                if aff_text and aff_text not in affiliations:
                    affiliations.append(aff_text)
            else:
                aff_text = self._parse_affiliation(aff_elem)
                if aff_text and aff_text not in affiliations:
                    affiliations.append(aff_text)

        return affiliations


def convert_xml_to_json(
    xml_file_path: Path,
    affiliation_mode: str = 'hybrid',
    id_lookup: Optional[Dict[str, str]] = None,
) -> Dict[str, List[Dict[str, Any]]]:
    try:
        with open(xml_file_path, 'r', encoding='utf-8') as f:
            xml_content = f.read()
    except Exception as e:
        logger.error(f"Failed to read file {xml_file_path}: {e}")
        return {}

    parser = GrobidXMLParser(xml_content, affiliation_mode=affiliation_mode)
    authors = parser.parse()
    paper_id = xml_file_path.stem
    if paper_id.endswith('.tei'):
        paper_id = paper_id[:-4]

    normalized_id = normalize_arxiv_id(paper_id)
    if id_lookup and normalized_id in id_lookup:
        paper_id = id_lookup[normalized_id]

    return {paper_id: authors}


def main():
    parser = argparse.ArgumentParser(
        description="Convert GROBID TEI XML to JSON format"
    )
    parser.add_argument(
        "-i", "--input",
        type=str,
        help="Input XML file or directory containing XML files"
    )
    parser.add_argument(
        "-o", "--output",
        type=str,
        help="Output JSON file (default: input_file.json or parsed_affiliations.json for directories)"
    )
    parser.add_argument(
        "--affiliation-mode",
        type=str,
        choices=['structured', 'raw', 'hybrid'],
        default='hybrid',
        help="Mode for parsing affiliations: 'structured' (only use structured elements), 'raw' (only use raw text), or 'hybrid' (default: try structured first, fall back to raw)"
    )
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Enable verbose logging"
    )
    parser.add_argument(
        "-g", "--ground-truth",
        type=str,
        help="Optional ground truth JSON path for ID remapping"
    )

    args = parser.parse_args()

    if args.verbose:
        logger.setLevel(logging.DEBUG)

    input_path = Path(args.input)
    ground_truth_lookup = build_ground_truth_id_lookup(Path(args.ground_truth)) if args.ground_truth else {}

    if not input_path.exists():
        logger.error(f"Input path does not exist: {input_path}")
        sys.exit(1)

    results = {}

    if input_path.is_file():
        if not input_path.suffix == '.xml':
            logger.error("Input file must be an XML file")
            sys.exit(1)

        results = convert_xml_to_json(
            input_path,
            affiliation_mode=args.affiliation_mode,
            id_lookup=ground_truth_lookup or None,
        )
        if args.output:
            output_file = Path(args.output)
        else:
            output_file = input_path.with_suffix('.json')

    elif input_path.is_dir():
        xml_files = list(input_path.glob("*.xml"))

        if not xml_files:
            logger.error(f"No XML files found in directory: {input_path}")
            sys.exit(1)

        logger.info(f"Processing {len(xml_files)} XML files...")

        for xml_file in xml_files:
            logger.debug(f"Processing {xml_file}")
            file_results = convert_xml_to_json(
                xml_file,
                affiliation_mode=args.affiliation_mode,
                id_lookup=ground_truth_lookup or None,
            )
            results.update(file_results)

        if args.output:
            output_file = Path(args.output)
        else:
            output_file = Path("parsed_affiliations.json")

    else:
        logger.error(f"Input must be a file or directory: {input_path}")
        sys.exit(1)

    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        logger.info(f"Successfully wrote results to {output_file}")
    except Exception as e:
        logger.error(f"Failed to write output file: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
