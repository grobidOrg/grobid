# Using Deep Learning models instead of default CRF

## Integration with DeLFT

Since GROBID version `0.5.4` (2018), it is possible to use in GROBID recent Deep Learning sequence labelling models trained with [DeLFT](https://github.com/kermitt2/delft). The available neural models include in particular BidLSTM-CRF with Glove embeddings, with additional feature channel (for layout features), and transformer-based fine-tuned architectures with or without CRF activation layer (e.g. SciBERT-CRF), which can be used as alternative to the default Wapiti CRF.

These architectures have been tested on Linux 64bit, macOS 64bit (Intel), and macOS ARM.

GROBID uses DeLFT **1.x**, which runs on [PyTorch](https://pytorch.org). DeLFT up to 0.4.x ran on TensorFlow/Keras: its models (`model_weights.hdf5`) cannot be loaded by DeLFT 1.x and the TensorFlow runtime is no longer needed. The models shipped with GROBID have been retrained with DeLFT 1.x (see [Shipped models](#shipped-models) below); a model trained by yourself with DeLFT 0.4.x has to be retrained. See [Upgrading](Upgrading.md) when moving an existing installation.

Integration is realized via Java Embedded Python [JEP](https://github.com/ninia/jep), which uses a JNI of CPython. This integration was measured two times faster than the TensorFlow Java API and is significantly faster than RPC serving (see [here](https://www.slideshare.net/FlinkForward/flink-forward-berlin-2017-dongwon-kim-predictive-maintenance-with-apache-flink). Additionally, it does not require to modify DeLFT as it would be the case with Py4J gateway (socket-based).

There are currently no neural model for the fulltext models, because the input sequences for this model are too large for the current supported Deep Learning architectures. The problem would need to be formulated differently for this task or to use alternative DL architectures (with sliding window, etc.).

Low-level models not using layout features (author name, dates, affiliations...) perform usually better than CRF and does not require a feature channel. When layout features are involved, neural models with an additional feature channel should be preferred (e.g. `BidLSTM_CRF_FEATURES` in DeLFT) to those without feature channel.

See some evaluations under `grobid-trainer/docs`.

Current neural models can be up to 50 times slower than CRF, depending on the architecture and available CPU/GPU. However when sequences can be processed in batch (e.g. for the citation model), overall runtime remains good with some clear accuracy gain for some models. This is where the possibility to mix CRF and Deep Learning models for different structuring tasks is very useful, as it permits to adjust the balance between possible accuracy and scalability in a fine-grained manner, using a reasonable amount of memory. 

## Recommended Deep Learning models

By default, only CRF models are used by Grobid. You need to select the Deep Learning models you would like to use in the GROBID configuration yaml file (`grobid/grobid-home/config/grobid.yaml`). See [here](Configuration.md#configuring-the-models) for more details on how to select these models. The most convenient way to use the Deep Learning models is to use the full GROBID Docker image and pass a configuration file at launch of the container describing the selected models to be used instead of the default CRF ones. Note that the full GROBID Docker image is already configured to use Deep Learning models for bibliographical reference and affiliation-address parsing. 

For current GROBID version 0.9.1, we recommend considering the usage of the following Deep Learning models: 

- `citation` model: for bibliographical parsing, the `BidLSTM_CRF_FEATURES` architecture provides currently the best accuracy, significantly better than CRF (+3 to +5 points in F1-Score). With a GPU, there is normally no runtime impact by selecting this model. SciBERT fine-tuned model performs currently at  lower accuracy. 

- `affiliation-address` model: for parsing affiliation and address blocks, `BidLSTM_CRF_FEATURES` architecture provides better accuracy than CRF at the cost of a minor runtime impact. 

- `reference-segmenter` model: this model segments a bibliographical reference section into individual references, the `BidLSTM_CRF_FEATURES` architecture provides better accuracy than CRF (even on very very very long reference sections), but at the cost of a global runtime 2 to 3 times slower. 

- `header` model: this model extracts the header metadata, the `BidLSTM_CRF_FEATURES` architecture provides slightly better results than CRF, especially with less mainstream domains and publisher. With a GPU, there is normally almost no runtime impact by selecting this DL model. 

- `funding-acknowledgement` model: this is a typical NER model that extracts funder names, funding information, acknowledged persons and organizations, etc. The `BidLSTM_CRF_FEATURES` provides more accurate results than CRF.

Other Deep Learning models do not show better accuracy than old-school CRF according to our benchmarkings, so we do not recommend using them in general at this stage. However, some of them tend to be more portable and can be more reliable than CRF for document layouts and scientific domains far from what is available in the training data.

Finally, the model `fulltext` (structuring the content body of a document) is currently only based on CRF, due to the long input sequences to be processed. 

### Shipped models

GROBID ships the following DeLFT models under `grobid-home/models/`, all with the `BidLSTM_CRF_FEATURES` architecture and `glove-840B` word embeddings: `segmentation`, `header`, `reference-segmenter`, `name-header`, `name-citation`, `date`, `figure`, `table`, `affiliation-address`, `citation` and `funding-acknowledgement`.

Each model is a directory named `<model>-<architecture>-<suffix>`, for instance `header-BidLSTM_CRF_FEATURES-glove_840B`, holding:

- `config.json`: the model configuration (architecture, embeddings, features, maximum sequence length...),
- `preprocessor.json`: the vocabularies and the feature preprocessing,
- `model.safetensors`: the weights, in the [safetensors](https://huggingface.co/docs/safetensors) format.

The suffix, `glove_840B` here, tells the variant of the model; it is selected in the configuration with `delft.suffix`, see [Configuration](#configuration) below. A model without a suffix lives in `<model>-<architecture>`.

There is currently no DeLFT model shipped for `patent-citation`, for the flavors (e.g. `header-article-light`, which then use the model of their base, e.g. `header`), nor for other architectures (`BidLSTM_ChainCRF_FEATURES`, `BidLSTM_CRF` and the `BERT` ones): to use them, train the model first (see [Training the models](Training-the-models-of-Grobid.md)).

The two text classifiers `copyright` and `license` are still TensorFlow models (`grobid-home/models/copyright_gru` and `license_gru`), which DeLFT 1.x refuses to load. Keep their `engine` to `wapiti` (the default): the copyright and license of a header is then not classified.

### Getting started with Deep Learning

Using Deep Learning model in GROBID with a normal installation/build is not straightforward at the present time, due to the required availability of various native libraries and to the Python dynamic linking and packaging mess, which leads to force some strict version and system dependencies. GROBID supports Python environment managers (virtualenv, conda) for DeLFT integration, which simplifies the setup compared to earlier versions.

The most simple solution is to use the ["full" GROBID docker image](Grobid-docker.md), which allows to use Deep Learning models without further installation and which provides automatic GPU support. 

However, if you need a "local" library installation and build, prepare a lot of coffee, here are the step-by-step instructions to get a working local Deep Learning GROBID.

#### Classic python and Virtualenv

<span>0.</span> Install GROBID as indicated [here](Install-Grobid.md).

The following was tested with Java 21.

<span>1.</span> Install [DeLFT](https://github.com/kermitt2/delft) 1.x, see instructions [here](https://github.com/kermitt2/delft#install). DeLFT 1.x requires Python 3.10 to 3.13 and brings PyTorch with it, no TensorFlow is needed. Use a new Python environment rather than one holding the TensorFlow-based DeLFT 0.4.x.

Until the next DeLFT release, GROBID uses a given commit of DeLFT, the one indicated by `DELFT_REF` in `Dockerfile.delft`. Clone DeLFT next to GROBID and install it from this commit:

```shell
git clone https://github.com/kermitt2/delft.git ../delft
cd ../delft
git checkout 137f59c3c1cb97055205831e2f499a5af75e06fa

# Linux with an NVIDIA GPU (CUDA 12.8)
pip install -e ".[gpu]" --extra-index-url https://download.pytorch.org/whl/cu128

# Linux without GPU (x86_64 or arm64)
pip install -e . --extra-index-url https://download.pytorch.org/whl/cpu

# macOS (PyTorch uses the Apple Silicon GPU with MPS)
pip install -e .
```

On Linux, the default PyTorch wheel of PyPI is a CUDA build, which brings several GB of CUDA libraries: without GPU, take the CPU build from the PyTorch index as above (`python -c "import torch; print(torch.__version__)"` then shows `2.11.0+cpu`). The CUDA libraries come with the PyTorch wheel, only a recent NVIDIA driver needs to be installed on the machine. For older GPUs not supported by CUDA 12.8 (pre-Turing, e.g. V100/P100), DeLFT provides the `gpu-pre-turing` extra (CUDA 12.6), see the [DeLFT installation guide](https://github.com/kermitt2/delft/blob/master/doc/Install-DeLFT.md). DeLFT uses the GPU when one is available (CUDA, or MPS on Apple Silicon), the CPU otherwise.

A source checkout is needed to train models from GROBID, which runs DeLFT's `delft/applications/grobidTagger.py`. For running models only, DeLFT can also be installed with pip (`pip install "delft @ git+https://github.com/kermitt2/delft.git@<commit>"`); `delft.install` (see step 3) is then just the directory where the embeddings are stored.

The `glove-840B` embeddings used by the shipped models are downloaded (around 2GB) and stored as an lmdb database under `data/db/` of the DeLFT directory the first time a model using them is loaded. To prepare them in advance:

```shell
cd ../delft
python ../grobid/grobid-home/scripts/preload_embeddings.py --registry ../grobid/grobid-home/config/resources-registry.json
```

<span>2.</span> Test your DeLFT installation with a GROBID model, from the DeLFT directory:

```shell
python -c "from delft.sequenceLabelling import Sequence; m = Sequence('header-BidLSTM_CRF_FEATURES-glove_840B'); m.load(dir_path='../grobid/grobid-home/models')"
```

If it works (it prints `Model loaded: BidLSTM_CRF_FEATURES` and `Word embeddings: glove-840B`), you have a working DeLFT environment for the GROBID models. The next steps address the native bridge between DeLFT and the JVM running GROBID. 

<span>3.</span> Configure your GROBID config file. 

Indicate the path to the DeLFT install in the GROBID config file `grobid.yaml` (`grobid-home/config/grobid.yaml`), 

```yaml
  delft:
    install: "../delft"
```

Indicate the GROBID model that should use a Deep Learning implementation in the same config file, for instance if you wish to use a Deep Learning model for the citation model (it provides 2-3 additional points to the F1-score for bibliographical reference recognition) use:

```yaml
  models:
    - name: "citation"
      engine: "delft"
      delft:
        architecture: "BidLSTM_CRF_FEATURES"
        suffix: "glove_840B"
```

The default Deep Learning architecture is `BidLSTM_CRF`, which is the best sequence labelling RNN architecture (basically a slightly revised version of [(Lample et al., 2016)](https://arxiv.org/abs/1603.01360) with Glove embeddings). However for GROBID, an architecture also exploiting features (in particular layout features, which are not captured at all by the pretrained language models) gives usually better results and the preferred choice is `BidLSTM_CRF_FEATURES`. If you wish to use another architecture, you need to specify it in the same config file. 

For instance to use a model integrating a fine-tuned transformer, you can select a `BERT_CRF` fine-tuned model (basically the transformer layers with CRF as final activation layer) and indicate in the field `transformer` the name of the transformer model in the [Hugging Face transformers Hub](https://huggingface.co/models) to be used to instantiate the transformer layer, typically [allenai/scibert_scivocab_cased](https://huggingface.co/allenai/scibert_scivocab_cased) for `SciBERT` in the case of scientific articles:

```yaml
  models:
    - name: "citation"
      engine: "delft"
      delft:
        architecture: "BERT_CRF"
        transformer: "allenai/scibert_scivocab_cased"
```

However, it will work only if the model is available under `grobid-home/models/`, currently only `BidLSTM_CRF_FEATURES` models are shipped with GROBID (see [Shipped models](#shipped-models)), given the size of BERT transformer models (400MB). To use a different architecture, you will thus need to train the new architecture model first with DeLFT and copy all the model files under the model directory `grobid-home/models/<model>-<architecture>[-<suffix>]/`. 

If you are using a Python environment for the DeLFT installation, you can set the environment path in the config file as well:

```yaml
  delft:
    pythonVirtualEnv: /where/my/damned/python/virtualenv/is/ 
```

Normally by setting the Python environment path in the config file (e.g. `pythonVirtualEnv: "../delft/env"`), you will not need to launch GROBID in the same activated environment. 

<span>4.</span> Install [JEP](https://github.com/ninia/jep) manually and preferably globally (outside a virtual env. and not under `~/.local/lib/python3.*/site-packages/`). Use the version of the `black.ninia:jep` dependency in `build.gradle` (currently `4.3.2`), installed after DeLFT so that it builds against the numpy version DeLFT installs: `pip install jep==4.3.2` (it needs a JDK, `JAVA_HOME` pointing to it). 

We provide an installation script for Linux under `grobid-home/scripts`. This script should be launched from GROBID root directory (`grobid/`), e.g.:

```shell
./grobid-home/scripts/install_jep_lib.sh 
```

This script will install the right version of the native JEP library according to the local architecture and python version.

<span>5.</span> Run GROBID, this is the "*but on my machine it works*" moment: 

To run the grobid service, under `grobid/`:

```shell
./gradlew run
```

In case you have installed DeLFT with `virtualenv` and you have not indicated the virtual environment in the GROBID config file, you should start GROBID after having activated the `virtualenv`. 

When running the GROBID service on a first input, you should see in the logs that the expected Deep Learning models are loaded and the JEP threads are activated:

```
INFO  [2026-10-18 15:57:30,053] org.grobid.core.jni.DeLFTModel: Loading DeLFT model for citation with architecture BidLSTM_CRF_FEATURES...
Loading weights from /opt/grobid/grobid-home/models/citation-BidLSTM_CRF_FEATURES-glove_840B/model.safetensors
Model loaded: BidLSTM_CRF_FEATURES
Word embeddings: glove-840B
```

If the model directory does not exist, the model fails to load with an error naming the directory looked for (`No DeLFT model citation-BidLSTM_CRF_FEATURES-glove_840B under .../grobid-home/models`): check the `architecture` and `suffix` of the model in the configuration.

It is then possible to [benchmark end-to-end](End-to-end-evaluation.md) the selected Deep Learning models as any usual GROBID benchmarking exercise. In practice, the CRF models should be mixed with Deep Learning models to keep the process reasonably fast and memory-hungry. In addition, note that, currently, due to the limited amount of training data, Deep Learning models perform better than CRF for a subset of models (`citation`, `affiliation-address`, `reference-segmenter`, `header` and `funding-acknowledgement`). This should of course certainly change in the future! 

#### Anaconda 

In case you use an anaconda environment instead of a Virtualenv:

- create the anaconda environment

```shell
conda create --name grobidDelft pip 
```

- activate the environment: 

```shell
conda activate grobidDelft
```

**NOTE**: make sure you are using the pip inside the environment (sometimes conda uses the root pip and mess up with your system): `which pip` should return a path within your environment. The conda creation command should already ensure that.  

Follow the steps described above, having the conda environment activated. Do not indicate a virtualEnv path in the GROBID config file and launch the GROBID service with the conda environment activated. For conda too, it is preferable to install JEP globally as indicated above. 

## Configuration

GROBID allows running models in mixed mode, i.e. to use DeLFT Deep Learning architectures for certain models, and Wapiti CRF for others. It is advised to select the preferred engine for each individual models in the GROBID config file. 

By default, all the models are set to use Wapiti CRF as sequence labeling implementation. Each model has its own configuration block, where it is possible to select a particular model implementation, with its own parameters. 

The following models (citation) will run with DeLFT `BidLSTM_CRF_FEATURES` by adding: 

```yaml
  models:
    - name: "citation"
      engine: "delft"
      delft:
        architecture: "BidLSTM_CRF_FEATURES"
        suffix: "glove_840B"
```

The model is loaded from `grobid-home/models/<name>-<architecture>-<suffix>`, here `citation-BidLSTM_CRF_FEATURES-glove_840B`:

- `architecture` is the DeLFT architecture of the model,
- `suffix` is the suffix of the model directory, the one given to DeLFT with `--suffix` when the model was trained. The shipped models use `glove_840B`, the embeddings they are trained with. Without `suffix`, the model is loaded from `<name>-<architecture>`. It can contain only letters, digits, `.`, `_` and `-`.

A flavor (e.g. `header-article-light`) inherits both from its base model (e.g. `header`) when its configuration does not set them. When the model directory of a flavor does not exist, the model of the base is used.

`embeddings_name` (default `glove-840B`) is the word embeddings used when training a model; at runtime, the embeddings come from the `config.json` of the model. An empty `embeddings_name` trains a model without word embeddings, which does not need any embeddings database at runtime.

### CPU threads

When running on CPU, PyTorch spreads the work of each model call on a pool of threads. DeLFT sizes this pool from the cores available to the process, taking into account the limits of a container (`--cpus`, `--cpuset-cpus`). Setting `OMP_NUM_THREADS` (and `MKL_NUM_THREADS`) before starting GROBID sets the size of the pool instead, it has to be set in the environment as it is read when PyTorch loads.

When GROBID processes many documents in parallel, a large pool for every model call makes the calls compete for the cores; the full Docker image therefore sets `OMP_NUM_THREADS=4` and `MKL_NUM_THREADS=4`, which measured best on 10 to 20 core machines. Avoid `1`, which made tagging around 40% slower.

## Troubleshooting

1. If there is a dependency problem when JEP starts, usually the JVM stops. 
We are still discovering this part, please feel free to submit issues should you incur in these problems. 
See discussion [here](https://github.com/kermitt2/grobid/pull/454)

2. If the GLIBC causes an error,  

```
! ImportError: /lib64/libstdc++.so.6: version `GLIBCXX_3.4.20' not found (required by .../site-packages/...)
```

here a quick solution (libgcc should be already installed, if so, just skip that pass): 

```shell
conda install libgcc

export export LD_PRELOAD=$anaconda_path/lib/libstdc++.so.6.0.25
    
./gradlew run
```
