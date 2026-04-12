# Getting started

!!! tip "Already using GROBID? Read the upgrade guide first"
    If you are upgrading an existing installation from an earlier version, start with the [Upgrade guide](Upgrading.md) — GROBID 0.9.0 introduces build-environment, runtime and model changes that require attention before pulling the new version.

## TL;DR

- **Docker:** The easiest way to run Grobid is via Docker on Linux, Mac and Windows. 
- **Clients**: Grobid is written in Java, but, fear not, various clients in Python and other languages are available. See [here](Grobid-service.md#clients-for-grobid-web-services) for more information.
- **Consolidation:** Grobid can merge and reconcile extracted bibliographic data and citations using Crossref. With great power comes great responsibility, read [here](Consolidation.md) before using consolidation. **Consolidation has impact on performances**.
- **Deep Learning & GPU:** Grobid’s best accuracy relies on deep learning models, which benefit from GPU acceleration. For fulltext-only extraction, use the CPU-only lightweight image. 
- **Production configuration**: See [here](Frequently-asked-questions.md#could-we-have-some-guidance-for-server-configuration-in-production) for tuning Grobid for production use.

!!! tip
    Keep reading until the end of the page.

## Using Grobid from the cloud  

The simpler way to play with Grobid is to use the [Grobid instance](https://grobidOrg-grobid.hf.space) or [Mirror Grobid instance](https://grobidOrg-grobid2.hf.space) where a **light instance** of Grobid is deployed.
You can use it to process a PDF file, or to test the Grobid web service. 
The space is free and does not require any authentication.

!!! warning "Grobid space is for demonstration only"
    This grobid space is not intended for production use, it is only a demonstration of Grobid capabilities. For production use, please deploy a local version or contact us.

!!! tip "Quick AWS grobid deployment"
    For quickly spin up and down a Grobid instance on Amazon Web Service EC2, you can check out the [aws-grobid](https://github.com/evamaxfield/aws-grobid) project. 

## Running Grobid locally

!!! tip 
    The standard way to run Grobid locally is to use [Docker](https://docs.docker.com/engine/understanding-docker/) for starting a Grobid server. 


For installing Docker on your system, please visit the official Docker documentation [here](https://docs.docker.com/get-docker/).

Grobid docker images are available on both at [Docker Grobid Hub](https://hub.docker.com/r/grobid/grobid) (`grobid/grobid`) and [Docker lfoppiano Hub (mirror)](https://hub.docker.com/r/lfoppiano/grobid) (`lfoppiano/grobid`) repositories using the same tag naming conventions.

!!! warning "Changes of the docker image naming conventions"
    The `lfoppiano/grobid` without any suffix (e.g. `lfoppiano/grobid:0.7.2`) was used for the CRF image. Starting from version 0.8.2 (included), the tag without any suffix will refer to the full image. To avoid problems, please use the `grobid/grobid` repository instead.


For convenience, we provide two Grobid docker images:

- the **full** image (docker tag `{version}-full`, e.g. `grobid/grobid:0.9.0-full`) provides the best accuracy, because it includes all the required Python and TensorFlow libraries, GPU support and all Deep Learning model resources. However, it requires more resources, ideally a GPU (it will be automatically detected on Linux). If you have a limited amount of PDF, a good machine, and prioritize [accuracy](Deep-Learning-models.md#recommended-deep-learning-models), use this Grobid flavor. To run this version of Grobid, the command is: 

```console
docker run --rm --gpus all --init --ulimit core=0 -p 8070:8070 grobid/grobid:0.9.0-full
```

- the **lightweight** image (docker tag `{version}-crf`, e.g. `grobid/grobid:0.9.0-crf`) offers the best runtime performance (**runs on CPUs only**), memory usage and Docker image size. However, it does not use some of the best performing models in terms of accuracy. If you have a lot of PDF to process, a low resource system, and accuracy is not so important, use this flavor:

```console
docker run --rm --init --ulimit core=0 -p 8070:8070 grobid/grobid:0.9.0-crf
```

More documentation on the Docker images can be found [here](Grobid-docker.md).

From there, you can check on your browser if the service works fine by accessing the welcome page of the service console, available at the URL <http://localhost:8070>. The GROBID server can be used via the [web service](Grobid-service.md).
