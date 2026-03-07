#!/usr/bin/env python3
import os
import re
import xml.etree.ElementTree as ET
from collections import defaultdict
import difflib

def extract_notes_with_context(file_path):
    """Extract headnote and footnote content with line numbers from a TEI XML file"""
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        
        # Handle namespace
        ns = {'tei': 'http://www.tei-c.org/ns/1.0'}
        
        notes_info = []
        
        # Read the raw file content to get line numbers
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        # Find all note elements and their approximate line numbers
        for i, line in enumerate(lines):
            if '<note' in line and 'place=' in line:
                # Extract the full note content by parsing the line
                if 'place="headnote"' in line:
                    # Extract content between note tags
                    match = re.search(r'<note[^>]*place="headnote"[^>]*>(.*?)</note>', line, re.DOTALL)
                    if match:
                        content = match.group(1).strip()
                        content = re.sub(r'<[^>]+>', '', content)  # Remove XML tags
                        content = re.sub(r'\s+', ' ', content)  # Normalize whitespace
                        notes_info.append({
                            'type': 'headnote',
                            'content': content,
                            'line': i + 1,
                            'raw_line': line.strip()
                        })
                elif 'place="footnote"' in line:
                    # Extract content between note tags
                    match = re.search(r'<note[^>]*place="footnote"[^>]*>(.*?)</note>', line, re.DOTALL)
                    if match:
                        content = match.group(1).strip()
                        content = re.sub(r'<[^>]+>', '', content)  # Remove XML tags
                        content = re.sub(r'\s+', ' ', content)  # Normalize whitespace
                        notes_info.append({
                            'type': 'footnote',
                            'content': content,
                            'line': i + 1,
                            'raw_line': line.strip()
                        })
        
        return notes_info
    
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return []

def normalize_text(text):
    """Normalize text for comparison by removing extra whitespace and standardizing"""
    # Remove extra whitespace and newlines
    text = re.sub(r'\s+', ' ', text)
    # Remove leading/trailing whitespace
    text = text.strip()
    # Convert to lowercase for more flexible matching
    return text.lower()

def find_meaningful_matches(headnotes, footnotes, threshold=0.9):
    """Find matches between headnotes and footnotes that represent meaningful content"""
    matches = []
    
    # Group by content length to avoid matching trivial strings
    for hn in headnotes:
        hn_content = hn['content']
        hn_norm = normalize_text(hn_content)
        
        # Skip very short or trivial content
        if len(hn_norm) < 10:
            continue
            
        for fn in footnotes:
            fn_content = fn['content']
            fn_norm = normalize_text(fn_content)
            
            # Skip very short or trivial content
            if len(fn_norm) < 10:
                continue
            
            # Look for exact matches after normalization
            if hn_norm == fn_norm and len(hn_norm) > 10:
                matches.append({
                    'headnote': hn,
                    'footnote': fn,
                    'match_type': 'exact',
                    'similarity': 1.0
                })
            else:
                # Look for high similarity matches
                similarity = difflib.SequenceMatcher(None, hn_norm, fn_norm).ratio()
                if similarity >= threshold:
                    matches.append({
                        'headnote': hn,
                        'footnote': fn,
                        'match_type': 'similar',
                        'similarity': similarity
                    })
    
    return matches

def analyze_content_type(content):
    """Analyze the type of content to categorize matches"""
    content_lower = content.lower()
    
    if 'doi' in content_lower or 'http' in content_lower:
        return 'identifier'
    elif any(word in content_lower for word in ['vol', 'journal', 'pp', 'page', 'issue']):
        return 'citation'
    elif re.search(r'\b(19|20)\d{2}\b', content_lower):
        return 'date/year'
    elif '@' in content_lower:
        return 'email'
    elif re.search(r'[A-Z][a-z]+ [A-Z][a-z]+', content):
        return 'author'
    else:
        return 'other'

def main():
    tei_dir = '.'
    inconsistent_files = []
    
    # Get all TEI XML files
    tei_files = [f for f in os.listdir(tei_dir) if f.endswith('.tei.xml')]
    
    print(f"Analyzing {len(tei_files)} TEI files...")
    
    for filename in sorted(tei_files):
        file_path = os.path.join(tei_dir, filename)
        notes_info = extract_notes_with_context(file_path)
        
        if not notes_info:
            continue
        
        # Separate headnotes and footnotes
        headnotes = [n for n in notes_info if n['type'] == 'headnote']
        footnotes = [n for n in notes_info if n['type'] == 'footnote']
        
        if headnotes and footnotes:
            # Find meaningful matches
            matches = find_meaningful_matches(headnotes, footnotes, threshold=0.85)
            
            if matches:
                # Group matches by content type
                content_types = defaultdict(list)
                for match in matches:
                    content_type = analyze_content_type(match['headnote']['content'])
                    content_types[content_type].append(match)
                
                inconsistent_files.append({
                    'file': filename,
                    'total_matches': len(matches),
                    'content_types': dict(content_types),
                    'matches': matches[:5],  # Limit to first 5 matches for display
                    'headnote_count': len(headnotes),
                    'footnote_count': len(footnotes)
                })
    
    # Generate comprehensive report
    print(f"\n{'='*100}")
    print(f"COMPREHENSIVE ANALYSIS REPORT")
    print(f"{'='*100}")
    print(f"Found {len(inconsistent_files)} files with inconsistencies out of {len(tei_files)} total files ({len(inconsistent_files)/len(tei_files)*100:.1f}%)")
    
    # Summary by content type
    all_content_types = defaultdict(int)
    for file_info in inconsistent_files:
        for content_type, matches in file_info['content_types'].items():
            all_content_types[content_type] += len(matches)
    
    print(f"\nSUMMARY BY CONTENT TYPE:")
    print("-" * 50)
    for content_type, count in sorted(all_content_types.items(), key=lambda x: x[1], reverse=True):
        print(f"  {content_type}: {count} matches")
    
    # Detailed file analysis
    print(f"\n{'='*100}")
    print("DETAILED FILE ANALYSIS")
    print(f"{'='*100}")
    
    for file_info in inconsistent_files:
        print(f"\nFILE: {file_info['file']}")
        print(f"Total headnotes: {file_info['headnote_count']}")
        print(f"Total footnotes: {file_info['footnote_count']}")
        print(f"Total matches found: {file_info['total_matches']}")
        
        if file_info['content_types']:
            print("Content types found:")
            for content_type, matches in file_info['content_types'].items():
                print(f"  - {content_type}: {len(matches)} matches")
        
        print("\nSample matches:")
        for i, match in enumerate(file_info['matches'][:3]):  # Show first 3 matches
            print(f"  Match {i+1} ({match['match_type']}, similarity: {match['similarity']:.2f}):")
            print(f"    Headnote (line {match['headnote']['line']}): {match['headnote']['content'][:80]}...")
            print(f"    Footnote (line {match['footnote']['line']}): {match['footnote']['content'][:80]}...")
            print(f"    Content type: {analyze_content_type(match['headnote']['content'])}")
            print()
        
        print("-" * 80)
    
    # Generate file list for easy reference
    print(f"\n{'='*100}")
    print("FILES WITH INCONSISTENCIES (for easy reference):")
    print("-" * 100)
    for file_info in sorted(inconsistent_files, key=lambda x: x['total_matches'], reverse=True):
        print(f"{file_info['file']} ({file_info['total_matches']} matches)")

if __name__ == "__main__":
    main()
