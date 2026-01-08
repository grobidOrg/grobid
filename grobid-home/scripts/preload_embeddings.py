'''
This script is an optional part of the GROBID docker image build, to pre-load selected embeddings in 
the image.

The script is supposed to be copied under the delft installation in the docker image, then executed
either with just an embedding name (e.g. "glove-840B") for online download of the embedding file
or with an embedding name (e.g. "glove-840B") and a local path to the embedding file copied temporary 
in the image.
If the embedding file is downloaded, it will be removed by the script. 
If the embedding file is copied in the image and passed as argument, it's up to the docker build file to
remove the embedding file. 

Obviously it will add a few GB more to the docker image. Without pre-loading, the embedding file will be 
downloaded and loaded in lmdb at each run of the docker container.

Embeddings are stored as raw float32 bytes (little-endian) for direct use by Java ONNX inference.
'''

import os
import argparse
from delft.utilities.Embeddings import Embeddings, open_embedding_file
from delft.utilities.Utilities import download_file
import lmdb
import json
import struct

map_size = 100 * 1024 * 1024 * 1024

def preload(embeddings_name, input_path=None, registry_path=None):
    """
    Preload embeddings into LMDB database as raw float32 bytes.

    Args:
        embeddings_name: Name of the embeddings (e.g., 'glove-840B')
        input_path: Optional path to embeddings file
        registry_path: Optional path to embedding registry JSON
    """
    resource_registry = None
    if registry_path != None:
        with open(registry_path, 'r') as f:
            resource_registry = json.load(f)

    embeddings = Embeddings(embeddings_name, resource_registry=resource_registry, load=False)

    description = embeddings.get_description(embeddings_name)
    if description is None:
        print("Error: embedding name", embeddings_name, "is not registered")
        return

    if input_path is None:
        embeddings_path = None
        # download if url is available
        if description is not None and "url" in description and len(description["url"])>0:
            url = description["url"]
            download_path = embeddings.registry['embedding-download-path']
            # if the download path does not exist, we create it
            if not os.path.isdir(download_path):
                try:
                    os.mkdir(download_path)
                except OSError:
                    print ("Creation of the download directory", download_path, "failed")

            print("Downloading resource file for", embeddings_name, "...")
            embeddings_path = download_file(url, download_path)
            if embeddings_path != None and os.path.isfile(embeddings_path):
                print("Download sucessful:", embeddings_path)
        else:
            print("Embeddings resource is not specified in the embeddings registry:", embeddings_name)
    else:
        embeddings_path = input_path

    if embeddings_path == None:
        print("Fail to retrieve embedding file for", embeddings_name)
        return

    # Load and store as raw float32 bytes for Java compatibility
    load_embeddings_raw_format(embeddings, embeddings_name, embeddings_path)
    embeddings.clean_downloads()


def load_embeddings_raw_format(embeddings, embeddings_name, embeddings_path):
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
    embedding_lmdb_path = embeddings.registry["embedding-lmdb-path"]
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
    parser = argparse.ArgumentParser(description = "preload embeddings during the GROBID docker image build as embedded lmdb")
    parser.add_argument("--embedding", default='glove-840B',
        help=(
            "the desired pre-trained word embeddings using their descriptions in the file"
            " embedding-registry.json,"
            " be sure to use here the same name as in the registry (e.g. 'glove-840B', 'fasttext-crawl', 'word2vec')"
        )
    )
    parser.add_argument("--input", help="path to the embeddings file to be loaded located on the host machine (where the docker image is built),"
                                       " this is optional, without this parameter the embeddings file will be downloaded from the url indicated"
                                       " in the embeddings registry, embedding-registry.json")
    parser.add_argument("--registry", help="path to the embedding registry to be considered for setting the paths/urls to embeddings")

    args = parser.parse_args()

    embeddings_name = args.embedding
    input_path = args.input
    registry_path = args.registry

    preload(embeddings_name, input_path, registry_path)
