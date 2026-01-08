'''
Standalone script to preload embeddings for GROBID ONNX Docker image.
This version has no DeLFT dependency - uses only standard library + lmdb + requests.

Embeddings are stored as raw float32 bytes (little-endian) for direct use by Java ONNX inference.
'''

import os
import argparse
import json
import struct
import gzip
import zipfile
import tempfile
import shutil

try:
    import lmdb
except ImportError:
    print("Error: lmdb package is required. Install with: pip install lmdb")
    exit(1)

try:
    import requests
except ImportError:
    print("Error: requests package is required. Install with: pip install requests")
    exit(1)

map_size = 100 * 1024 * 1024 * 1024  # 100GB max size


def download_file(url, download_path):
    """Download a file from URL to the specified path."""
    local_filename = os.path.join(download_path, url.split('/')[-1])
    
    print(f"Downloading {url}...")
    with requests.get(url, stream=True) as r:
        r.raise_for_status()
        total_size = int(r.headers.get('content-length', 0))
        downloaded = 0
        with open(local_filename, 'wb') as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)
                downloaded += len(chunk)
                if total_size > 0:
                    pct = (downloaded / total_size) * 100
                    print(f"\rDownloaded {downloaded / (1024*1024):.1f}MB / {total_size / (1024*1024):.1f}MB ({pct:.1f}%)", end='', flush=True)
    print()
    return local_filename


def extract_file(filepath):
    """Extract a compressed file and return the path to the extracted content."""
    extract_dir = tempfile.mkdtemp()
    
    if filepath.endswith('.zip'):
        print(f"Extracting {filepath}...")
        with zipfile.ZipFile(filepath, 'r') as zf:
            zf.extractall(extract_dir)
        # Find the .txt or .vec file in the extracted contents
        for root, dirs, files in os.walk(extract_dir):
            for f in files:
                if f.endswith(('.txt', '.vec')):
                    return os.path.join(root, f), extract_dir
    elif filepath.endswith('.gz') and not filepath.endswith('.tar.gz'):
        print(f"Extracting {filepath}...")
        output_path = os.path.join(extract_dir, os.path.basename(filepath)[:-3])
        with gzip.open(filepath, 'rb') as f_in:
            with open(output_path, 'wb') as f_out:
                shutil.copyfileobj(f_in, f_out)
        return output_path, extract_dir
    
    return filepath, None


def open_embedding_file(filepath):
    """Open an embedding file, handling gzip compression."""
    if filepath.endswith('.gz'):
        return gzip.open(filepath, 'rt', encoding='utf-8', errors='ignore')
    else:
        return open(filepath, 'r', encoding='utf-8', errors='ignore')


def preload(embeddings_name, input_path=None, registry_path=None):
    """
    Preload embeddings into LMDB database as raw float32 bytes.

    Args:
        embeddings_name: Name of the embeddings (e.g., 'glove-840B')
        input_path: Optional path to embeddings file
        registry_path: Optional path to embedding registry JSON
    """
    # Load registry
    registry = None
    if registry_path and os.path.exists(registry_path):
        with open(registry_path, 'r') as f:
            registry = json.load(f)
    
    if registry is None:
        print("Error: registry file is required")
        return
    
    # Find embedding description
    description = None
    for emb in registry.get('embeddings', []):
        if emb.get('name') == embeddings_name:
            description = emb
            break
    
    if description is None:
        print(f"Error: embedding name '{embeddings_name}' is not registered")
        return
    
    embeddings_path = input_path
    temp_dir = None
    downloaded_file = None
    
    if embeddings_path is None:
        # Download if url is available
        url = description.get('url', '')
        if url:
            download_path = registry.get('embedding-download-path', 'data/download')
            if not os.path.isdir(download_path):
                os.makedirs(download_path)
            
            print(f"Downloading resource file for {embeddings_name}...")
            downloaded_file = download_file(url, download_path)
            
            if downloaded_file and os.path.isfile(downloaded_file):
                print(f"Download successful: {downloaded_file}")
                # Extract if compressed
                embeddings_path, temp_dir = extract_file(downloaded_file)
            else:
                print(f"Failed to download embedding file")
                return
        else:
            print(f"Embeddings resource URL is not specified for: {embeddings_name}")
            return
    
    if embeddings_path is None or not os.path.exists(embeddings_path):
        print(f"Fail to retrieve embedding file for {embeddings_name}")
        return
    
    # Load and store as raw float32 bytes for Java compatibility
    load_embeddings_raw_format(registry, embeddings_name, embeddings_path)
    
    # Cleanup
    if temp_dir and os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)
    if downloaded_file and os.path.exists(downloaded_file):
        os.remove(downloaded_file)


def load_embeddings_raw_format(registry, embeddings_name, embeddings_path):
    """
    Load embeddings from file and store as raw float32 bytes in LMDB.

    This format is compatible with Java's WordEmbeddings class which expects
    little-endian float32 arrays without pickle serialization.
    """
    print(f"Loading embeddings from {embeddings_path} in raw float32 format...")

    embedding_file = open_embedding_file(embeddings_path)
    if embedding_file is None:
        print("Error: could not open embeddings file", embeddings_path)
        return

    # Create LMDB environment
    embedding_lmdb_path = registry.get("embedding-lmdb-path", "data/db")
    if not os.path.isdir(embedding_lmdb_path):
        os.makedirs(embedding_lmdb_path)

    env_path = os.path.join(embedding_lmdb_path, embeddings_name)
    env = lmdb.open(env_path, map_size=map_size)

    max_key_size = env.max_key_size()  # Get the max key size for this LMDB instance

    count = 0
    skipped = 0
    batch_size = 10000
    batch = []

    # Read header line for some formats (e.g., word2vec binary)
    first_line = True
    embedding_dim = None

    for line in embedding_file:
        try:
            if isinstance(line, bytes):
                line = line.decode('utf-8', errors='ignore')

            line = line.rstrip()
            if not line:
                continue

            parts = line.split(' ')
            if len(parts) < 10:  # Skip header or malformed lines
                if first_line:
                    first_line = False
                    continue
                continue

            first_line = False
            word = parts[0]

            # Parse vector values
            try:
                values = [float(x) for x in parts[1:] if x]
            except ValueError:
                continue

            if embedding_dim is None:
                embedding_dim = len(values)
                print(f"Detected embedding dimension: {embedding_dim}")

            if len(values) != embedding_dim:
                continue

            # Check key size before storing (LMDB has a max key size, typically 511 bytes)
            key = word.encode('utf-8')
            if len(key) >= max_key_size:
                skipped += 1
                continue

            # Convert to raw float32 bytes (little-endian)
            raw_bytes = struct.pack(f'<{len(values)}f', *values)

            batch.append((key, raw_bytes))
            count += 1

            if len(batch) >= batch_size:
                with env.begin(write=True) as txn:
                    for k, v in batch:
                        txn.put(k, v)
                batch = []
                if count % 100000 == 0:
                    print(f"Processed {count} embeddings...")

        except Exception as e:
            print(f"Error processing line: {e}")
            continue

    # Write remaining batch
    if batch:
        with env.begin(write=True) as txn:
            for k, v in batch:
                txn.put(k, v)

    embedding_file.close()
    env.close()

    print(f"Loaded {count} embeddings with dimension {embedding_dim}")
    if skipped > 0:
        print(f"Skipped {skipped} entries with keys exceeding max key size ({max_key_size} bytes)")
    print(f"Stored in raw float32 format at: {env_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Standalone preload embeddings for GROBID ONNX docker image (no DeLFT dependency)"
    )
    parser.add_argument("--embedding", default='glove-840B',
        help=(
            "the desired pre-trained word embeddings using their descriptions in the file"
            " resources-registry.json,"
            " be sure to use here the same name as in the registry (e.g. 'glove-840B', 'fasttext-crawl', 'word2vec')"
        )
    )
    parser.add_argument("--input", 
        help="path to the embeddings file to be loaded located on the host machine (where the docker image is built),"
             " this is optional, without this parameter the embeddings file will be downloaded from the url indicated"
             " in the embeddings registry")
    parser.add_argument("--registry", required=True,
        help="path to the embedding registry JSON file (resources-registry.json)")

    args = parser.parse_args()

    embeddings_name = args.embedding
    input_path = args.input
    registry_path = args.registry

    preload(embeddings_name, input_path, registry_path)
