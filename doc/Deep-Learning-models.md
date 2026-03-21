# Using Deep Learning models instead of default CRF

## Integration with DeLFT

Since GROBID version `0.5.4` (2018), it is possible to use in GROBID recent Deep Learning sequence labelling models trained with [DeLFT](https://github.com/kermitt2/delft). The available neural models include in particular BidLSTM-CRF with Glove embeddings, with additional feature channel (for layout features), with ELMo, and transformer-based fine-tuned architectures with or without CRF activation layer (e.g. SciBERT-CRF), which can be used as alternative to the default Wapiti CRF.

These architectures have been tested on Linux 64bit and macOS 64bit. The support to the macOS ARM is in progress.

Integration is realized via Java Embedded Python [JEP](https://github.com/ninia/jep), which uses a JNI of CPython. This integration is two times faster than the Tensorflow Java API and significantly faster than RPC serving (see [here](https://www.slideshare.net/FlinkForward/flink-forward-berlin-2017-dongwon-kim-predictive-maintenance-with-apache-flink). Additionally, it does not require to modify DeLFT as it would be the case with Py4J gateway (socket-based).

There are currently no neural model for the fulltext models, because the input sequences for this model are too large for the current supported Deep Learning architectures. The problem would need to be formulated differently for this task or to use alternative DL architectures (with sliding window, etc.).

Low-level models not using layout features (author name, dates, affiliations...) perform usually better than CRF and does not require a feature channel. When layout features are involved, neural models with an additional feature channel should be preferred (e.g. `BidLSTM_CRF_FEATURES` in DeLFT) to those without feature channel.

See some evaluations under `grobid-trainer/docs`.

Current neural models can be up to 50 times slower than CRF, depending on the architecture and available CPU/GPU. However when sequences can be processed in batch (e.g. for the citation model), overall runtime remains good with some clear accuracy gain for some models. This is where the possibility to mix CRF and Deep Learning models for different structuring tasks is very useful, as it permits to adjust the balance between possible accuracy and scalability in a fine-grained manner, using a reasonable amount of memory. 

## Recommended Deep Learning models

By default, only CRF models are used by Grobid. You need to select the Deep Learning models you would like to use in the GROBID configuration yaml file (`grobid/grobid-home/config/grobid.yaml`). See [here](Configuration.md#configuring-the-models) for more details on how to select these models. The most convenient way to use the Deep Learning models is to use the full GROBID Docker image and pass a configuration file at launch of the container describing the selected models to be used instead of the default CRF ones. Note that the full GROBID Docker image is already configured to use Deep Learning models for bibliographical reference and affiliation-address parsing. 

For current GROBID version 0.8.2, we recommend considering the usage of the following Deep Learning models: 

- `citation` model: for bibliographical parsing, the `BidLSTM_CRF_FEATURES` architecture provides currently the best accuracy, significantly better than CRF (+3 to +5 points in F1-Score). With a GPU, there is normally no runtime impact by selecting this model. SciBERT fine-tuned model performs currently at  lower accuracy. 

- `affiliation-address` model: for parsing affiliation and address blocks, `BidLSTM_CRF_FEATURES` architecture provides better accuracy than CRF at the cost of a minor runtime impact. 

- `reference-segmenter` model: this model segments a bibliographical reference section into individual references, `BidLSTM_ChainCRF_FEATURES` architecture provides better accuracy than CRF (even on very very very long reference sections), but at the cost of a global runtime 2 to 3 times slower. 

- `header` model: this model extracts the header metadata, the `BidLSTM_CRF_FEATURES` or `BidLSTM_ChainCRF_FEATURES` (a faster variant) provides sligthly better results than CRF, especially with less mainstream domains and publisher. With a GPU, there is normally almost no runtime impact by selecting this DL model. 

- `funding-acknowledgement` model: this is a typical NER model that extracts funder names, funding information, acknowledged persons and organizations, etc. The `BidLSTM_CRF_FEATURES` provides more accurate results than CRF.

Other Deep Learning models do not show better accuracy than old-school CRF according to our benchmarkings, so we do not recommend using them in general at this stage. However, some of them tend to be more portable and can be more reliable than CRF for document layouts and scientific domains far from what is available in the training data.

Finally, the model `fulltext` (structuring the content body of a document) is currently only based on CRF, due to the long input sequences to be processed. 

### Getting started with Deep Learning

Using Deep Learning model in GROBID with a normal installation/build is not straightforward at the present time, due to the required availability of various native libraries and to the Python dynamic linking and packaging mess, which leads to force some strict version and system dependencies. Interfacing natively to a particular Python virtual environment (which is "session-based") is challenging. We are exploring different approach to facilitate this and get a "out-of-the-out" working system. 

The most simple solution is to use the ["full" GROBID docker image](Grobid-docker.md), which allows to use Deep Learning models without further installation and which provides automatic GPU support. 

However, if you need a "local" library installation and build, prepare a lot of coffee, here are the step-by-step instructions to get a working local Deep Learning GROBID.

#### Classic python and Virtualenv

<span>0.</span> Install GROBID as indicated [here](Install-Grobid.md).

The following was tested with Java version up to 17.

<span>1.</span> install [DeLFT](https://github.com/kermitt2/delft), see instructions [here](https://github.com/kermitt2/delft#install).
DeLFT version `0.4.4` has been tested successfully with Python 3.8 and above. For GPU support, CUDA >=11.2 must be installed. 

<span>2.</span> Test your DeLFT installation for GROBID models: 

```shell
cd deflt/
python -m delft.applications.grobidTagger citation tag --architecture BidLSTM_CRF
```

If it works (you see some annotations in JSON format), you are sure to have a working DeLFT environment for **all** GROBID models. The next steps address the native bridge between DeLFT and the JVM running GROBID. 

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
      architecture: "BidLSTM_CRF_FEATURES"
```

The default Deep Learning architecture is `BidLSTM_CRF`, which is the best sequence labelling RNN architecture (basically a slightly revised version of [(Lample et al., 2016)](https://arxiv.org/abs/1603.01360) with Glove embeddings). However for GROBID, an architecture also exploiting features (in particular layout features, which are not captured at all by the pretrained language models) gives usually better results and the prefered choise is `BidLSTM_CRF_FEATURES`. If you wish to use another architecture, you need to specify it in the same config file. 

For instance to use a model integrating a fine-tuned transformer, you can select a `BERT_CRF` fine-tuned model (basically the transformer layers with CRF as final activation layer) and indicate in the field `transformer` the name of the transformer model in the [Hugging Face transformers Hub](https://huggingface.co/models) to be used to instantiate the transformer layer, typically [allenai/scibert_scivocab_cased](https://huggingface.co/allenai/scibert_scivocab_cased) for `SciBERT` in the case of scientific articles:

```yaml
  models:
    - name: "citation"
      engine: "delft"
      architecture: "BERT_CRF"
      transformer: "allenai/scibert_scivocab_cased"
```

However, it will work only if the model is available under `grobid-home/models/`, currently only the `BidLSTM_CRF` and `BidLSTM_CRF_FEATURES` models are shipped with GROBID, given the size of BERT transformer models (400MB). To use a different architecture, you will thus need to train the new architecture model first with DeLFT and copy all the model files under the specific model subdirectory `grobid-home/models/**model**/`. 

If you are using a Python environment for the DeLFT installation, you can set the environment path in the config file as well:

```yaml
  delft:
    python_virtualEnv: /where/my/damned/python/virtualenv/is/ 
```

Normally by setting the Python environment path in the config file (e.g. `pythonVirtualEnv: "../delft/env"`), you will not need to launch GROBID in the same activated environment. 

<span>4.</span> Install [JEP](https://github.com/ninia/jep) manually and preferably globally (outside a virtual env. and not under `~/.local/lib/python3.*/site-packages/`). 

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
INFO  [2020-10-30 23:04:07,756] org.grobid.core.jni.DeLFTModel: Loading DeLFT model for citation with architecture BidLSTM_CRF...
INFO  [2020-10-30 23:04:07,758] org.grobid.core.jni.JEPThreadPool: Creating JEP instance for thread 44
```

It is then possible to [benchmark end-to-end](End-to-end-evaluation.md) the selected Deep Learning models as any usual GROBID benchmarking exercise. In practice, the CRF models should be mixed with Deep Learning models to keep the process reasonably fast and memory-hungry. In addition, note that, currently, due to the limited amount of training data, Deep Learning models perform significantly better than CRF only for a few models (`citation`, `affiliation-address`, `reference-segmenter`). This should of course certainly change in the future! 

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

The following models (citation) will run with DeLFT `BidLSTM-BidLSTM_CRF_FEATURES` by adding: 

```yaml
  models:
    - name: "citation"
      engine: "delft"
      architecture: "BidLSTM_CRF_FEATURES"
```

**NOTE**: model names normally all use underscore and no hyphen. If not the case, replace hyphen by underscore. 

## Troubleshooting

1. If there is a dependency problem when JEP starts, usually the JVM stops. 
We are still discovering this part, please feel free to submit issues should you incur in these problems. 
See discussion [here](https://github.com/kermitt2/grobid/pull/454)

2. If the GLIBC causes an error,  

```
! ImportError: /lib64/libstdc++.so.6: version `GLIBCXX_3.4.20' not found (required by /home/Luca/.conda/envs/tensorflow/lib/python3.6/site-packages/tensorflow/python/_pywrap_tensorflow_internal.so)
```

here a quick solution (libgcc should be already installed, if so, just skip that pass): 

```shell
conda install libgcc

export export LD_PRELOAD=$anaconda_path/lib/libstdc++.so.6.0.25
    
./gradlew run
```
