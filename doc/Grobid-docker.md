# GROBID and Docker containers

Docker automates the deployment of applications inside software containers. The documentation on how to install it and start using it can be found [here](https://docs.docker.com/engine/understanding-docker/).

GROBID can be instantiated and run using Docker. Using two equivalent docker hub repositories sharing the same conventions for image tags: 
 - [Docker hub Grobid repository](https://hub.docker.com/r/grobid/grobid/tags)
 - [Docker hub Grobid mirror repository](https://hub.docker.com/r/lfoppiano/grobid/)

!!! tip "TL;DR"
    If you need only fulltext (no citations, no bibliographical information, no statements), for RAG/LLM preprocessing/search, use the lightweight image. Otherwise, use the full image. For more customized needs, you can reach out. 

For convenience, we provide two docker images:

- a **full image** (using the convention `{version}-full` e.g. `grobid/grobid:{version}-full` or `lfoppiano/grobid:{version}-full`) (~8GB) able to run both Deep Learning and CRF models (**recommended to run it with a GPU**): this image includes all the required python and TensorFlow libraries, GPU support and all DL model resources. It can provide more accurate results, notably for reference extraction/parsing and citation context identification. Depending on the availability of a GPU (recommended) or not, some Deep Learning models might introduce much slower runtime and significantly higher memory usage. This image is considerably larger than a CRF-only image. The full image contains Python and TensorFlow/Pytorch libraries (more than 3GB) and pre-loaded embeddings (around 5GB), but we recommend to use it.

- a **lightweight image** (using the convention `{version}-crf` e.g. `grobid/grobid:{version}-crf` or `lfoppiano/grobid:{version}-crf`) with only CRF models (~500MB): this image offers best performance in terms of runtime and memory usage, as well as limiting the size of the image, but it does not use some of the best performing models by accuracy. If possible, use the above full image. 

Note that these provided docker images are currently only for amd64 CPU host machines. These images can run on MacOS/arm64, but only emulated (so quite slower). They will not run on linux/arm64 host machines. We will try to build multi-arch build images in the next versions. 

We assume in the following that docker is installed and working on your system. Note that the default memory available for your container might need to be increased for using all the available GROBID services, in particular on `macos`, see the Troubleshooting section below.


## Deep Learning and CRF image

The process for retrieving and running a docker image is as follows:

- Pull the image from docker HUB (check the [latest version number](https://hub.docker.com/r/grobid/grobid/tags)):

```bash
docker pull grobid/grobid:${latest_grobid_version}-full
```
or 
```bash
docker pull lfoppiano/grobid:${latest_grobid_version}-full
```

Current latest version:

```bash
> docker pull grobid/grobid:0.9.0-full
```

- Run the container:

```bash
> docker run --rm --gpus all --init --ulimit core=0 -p 8070:8070 grobid/grobid:0.9.0-full
```

The image will automatically use the GPU and CUDA version available on your host machine, but only on Linux. GPU usage via a container on Windows and MacOS machine is currently not supported by Docker. If no GPU are available, CPU will be used. 

Note that starting the container with option `--ulimit core=0` avoids having possible core dumped inside the container, which can happen otherwise due to the crash of the PDF parsing C++ component. Starting the container with parameter `-it` allows to interact with the docker run process, which is of limited use here, except conveniently stopping the docker container with control-c.

To specify to use only certain GPUs (see the [nvidia container toolkit user guide](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/user-guide.html#gpu-enumeration) for more details):

```bash
> docker run --rm --gpus '"device=1,2"' --init --ulimit core=0 -p 8070:8070 -p 8081:8071 grobid/grobid:${latest_grobid_version}-full
```

You can run the image on CPU by omitting the `-gpus` parameters. 

Note the default version is running on port `8070`, however it can be mapped on the more traditional port `8080` of your host with the following command:

```bash
> docker run --rm --gpus all --init --ulimit core=0 -p 8080:8070 grobid/grobid:${latest_grobid_version}-full
```

Access the service:
  - open the browser at the address `http://localhost:8080`
  - the health check will be accessible at the address `http://localhost:8081`

Grobid web services are then available as described in the [service documentation](Grobid-service.md).

By default, this image runs Deep Learning models for:

- bibliographical parsing (adding 2-4 points in terms of F1-score for bibliographical reference parsing and 2-5 points in term of citation context identifications, as compared to CRF-only image)

- affiliation-address parsing,

- segmentation of the bibliographical reference section into individual references,

- parsing of the header metadata,

- parsing of funding and acknowledgement sections.

With a GPU (at least 4GB GPU memory required), the processing runtime is similar as with the CRF-only image with CPU only. 

It is then possible to select other Deep Learning models for other processing stages of the Grobid model cascade, instead of the default CRF, as explained in the section below "Configure using the yaml config file". In particular using the `BidLSTM-CRF-FEATURES` architecture for the reference-segmenter model improves reference parsing and citation context resolution by around 1 point F1-score with bioRxiv and 0.5 with PMC. However, the runtime impact is quite be important, 2-3 times slower overall. Other Deep Learning models should provide currently no or only modest accuracy improvement according to our current evaluations (including with transformers like SciBERT and LinkBERT). 

## CRF-only image

The process for retrieving and running the image is as follows:

- Pull the image from docker HUB (check the [latest version number](https://hub.docker.com/r/lfoppiano/grobid/tags)):

```bash
docker pull grobid/grobid:${latest_grobid_version}-crf
```

or 

```bash 
docker pull lfoppiano/grobid:${latest_grobid_version}-crf
```

Latest version:

```bash
> docker pull grobid/grobid:0.9.0-crf
```

- Run the container:

```bash
> docker run --rm --init --ulimit core=0 -p 8070:8070 grobid/grobid:${latest_grobid_version}-crf
```

Latest version:

```bash
> docker run --rm --init --ulimit core=0 -p 8070:8070 grobid/grobid:0.9.0-crf
```

Note the default version is running on port `8070`, however it can be mapped on the more traditional port `8080` of your host with the following command:

```bash
docker run --rm --init --ulimit core=0 -p 8080:8070 -p 8081:8071 grobid/grobid:${latest_grobid_version}-crf
```

Access the service:
  - open the browser at the address `http://localhost:8080`
  - the health check will be accessible at the address `http://localhost:8081`

Grobid web services are then available as described in the [service documentation](Grobid-service.md).

### ARM64

Since version 0.8.1 the CRF-only image is available for both x86_64 and arm64. 
The `lfoppiano/grobid:{latest_grobid_version}` support both architectures by default. 

To run it on arm64, use the following command:

```bash
docker run --ulimit core=0 --platform linux/amd64 --init -p 8070:8070 --name grobid lfoppiano/grobid:{latest_grobid_version}
```

As discussed [here](https://github.com/kermitt2/grobid/issues/1119#issuecomment-2362204879), it is mandatory to provide the parameter `--platform linux/amd64` to force the usage of amd64 architecture and avoid the raising of the rosetta error: 

```
rosetta error: failed to open elf at /lib64/ld-linux-x86-64.so.2
```

**NOTE**: The arm64 image has been tested only on `macOs` (not linux `arm64`).

## Configure using the yaml config file

The simplest way to pass a modified configuration to the docker image is to mount the yaml GROBID config file `grobid.yaml` when running the image. Modify the config file `grobid/grobid-home/config/grobid.yaml` according to your requirements on the host machine and mount it when running the image as follow: 

```bash
docker run --rm --gpus all --init --ulimit core=0 -p 8080:8070 -p 8081:8071 -v /home/lopez/grobid/grobid-home/config/grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml:ro  grobid/grobid:0.9.0
```

You need to use an absolute path to specify your modified `grobid.yaml` file.

## Configuration using Environment Variables

This usage is currently not supported anymore, due to the number and the hierarchical organization of the configuration parameters. Use the yaml configuration file to set production parameter to a docker image (see above to pass the yaml config file at launch of the container). 

## Troubleshooting

### Out of memory or container being killed while processing

This is usually be due to insufficient memory allocated to the docker machine. Depending on the intended usage, we recommend to allocate 4GB of RAM to structure entirely all the PDF content (`/api/processFulltextDocument`), otherwise 2GB are sufficient to extract only header information, and 3GB for citations. In case of more intensive usage and batch parallel processing, allocating 6 or 8GB is recommended.

On `macos`, see for instance [here](https://stackoverflow.com/questions/32834082/how-to-increase-docker-machine-memory-mac/39720010#39720010) on how to increase the RAM from the Docker UI.

The memory can be verified directly using the docker desktop application or via CLI:  

```bash
docker-machine inspect
```

You should see something like:

```json
{
    "ConfigVersion": 3,
    "Driver": {
        "IPAddress": "192.168.99.100",
        "MachineName": "default",
        "SSHUser": "docker",
        "SSHPort": 55933,
        "SSHKeyPath": "/Users/lfoppiano/.docker/machine/machines/default/id_rsa",
        "StorePath": "/Users/lfoppiano/.docker/machine",
        "SwarmMaster": false,
        "SwarmHost": "tcp://0.0.0.0:3376",
        "SwarmDiscovery": "",
        "VBoxManager": {},
        "HostInterfaces": {},
        "CPU": 1,
        "Memory": 2048,     #<---- Memory: 2GB
        "DiskSize": 204800,
        "NatNicType": "82540EM",
        "Boot2DockerURL": "",
        "Boot2DockerImportVM": "",
        "HostDNSResolver": false,
        "HostOnlyCIDR": "192.168.99.1/24",
        "HostOnlyNicType": "82540EM",
        "HostOnlyPromiscMode": "deny",
        "NoShare": false,
        "DNSProxy": true,
        "NoVTXCheck": false
    },
    "DriverName": "virtualbox",
    "HostOptions": {
      [...]
        },
        "SwarmOptions": {
         [...]
        },
        "AuthOptions": {
           [...]
        }
    },
    "Name": "default"
}
```

See for instance [here](https://stackoverflow.com/a/36982696) for allocating to the Docker machine more than the default RAM on `macos` with command lines.

### The TensorFlow library was compiled to use SSE4.1 instructions

If after starting the container with **CPU** only (Intel or AMD), the container crashes with an exit code 134 and the message:

```
The TensorFlow library was compiled to use SSE4.1 instructions, but these aren't available on your machine.
```

It means that your CPU does not support the extra SSE4.1 instructions needed by TensorFlow. This case doesn't happen with GPU. 

Since TensorFlow 1.6, the library is compiled to use this extra set instructions, because all modern CPU support them (all CPU after 2008) and it speeds up Deep Learning with CPU. However, when using a Virtual Machine, the default VM configuration of the CPU might not include to use these instructions. 

To solve this problem, you need to update the configuration of the CPU in the VM to support SSE4.1, SSE4.3 and AVX extension instruction sets. 

## Building an image

The following part is normally for development purposes. You can use the official stable docker images from the docker HUB as described above.
However if you are interested in using the master version of GROBID in container, or a customized branch/fork, building a new image is the way to go.

### Building the Deep Learning and image

In order to build an image supporting GPU, you need:

- preferably to have the nvidia driver and CUDA properly installed on the machine you are using to build the image - this can be check by the usual `nvidia-smi` command

Without this requirement, the image might default to CPU, even if GPU are available on the host machine running the image. 

For being able to use both CRF and Deep Learning models, use the dockerfile `./Dockerfile.delft`. The only important information then is the version which will be checked out from the tags.

```bash
docker build -t grobid/grobid:0.9.0-full --build-arg GROBID_VERSION=0.9.0 --file Dockerfile.delft .
```

Similarly, if you want to create a docker image from the current master, development version:

```bash
docker build -t grobid/grobid:0.9.1-SNAPSHOT --build-arg GROBID_VERSION=0.9.1-SNAPSHOT --file Dockerfile.delft .
```

In order to run the container of the newly created image, for example for the development version `0.9.1-SNAPSHOT`, using all GPU available:

```bash
docker run --rm --gpus all --init --ulimit core=0 -p 8080:8070 -p 8081:8071 grobid/grobid:0.9.1-SNAPSHOT
```

In practice, you need to indicate which models should use a Deep Learning model implementation and which ones can remain with a faster CRF model implementation, which is done currently in the `grobid.yaml` file. Modify the config file `grobid/grobid-home/config/grobid.yaml` accordingly on the host machine and mount it when running the image as follow: 

```bash
docker run --rm --gpus all --init --ulimit core=0 -p 8080:8070 -p 8081:8071 -v /home/lopez/grobid/grobid-home/config/grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml:ro grobid/grobid:0.9.1-SNAPSHOT
```

You need to use an absolute path to specify your modified `grobid.yaml` file.

For testing or debugging purposes, you can connect to the container with a bash shell (logs are under `/opt/grobid/logs/`):

```bash
docker exec -i -t {container_name} /bin/bash
```

The container name is given by the command:

```bash
docker container ls
```

### Building the CRF-only image

For building a CRF-only image, the dockerfile to be used is `./Dockerfile.crf`. The only important information then is the version which will be checked out from the tags.

```bash
docker build -t grobid/grobid:0.9.0-crf --build-arg GROBID_VERSION=0.9.0 --file Dockerfile.crf .
```

Similarly, if you want to create a docker image from the current master, development version:

```bash
docker build -t grobid/grobid:0.9.1-SNAPSHOT --build-arg GROBID_VERSION=0.9.1-SNAPSHOT --file Dockerfile.crf .
```

In order to run the container of the newly created image, for example for version `0.9.1-SNAPSHOT`:

```bash
docker run --rm --init --ulimit core=0 -p 8080:8070 -p 8081:8071 grobid/grobid:0.9.1-SNAPSHOT
```

For testing or debugging purposes, you can connect to the container with a bash shell (logs are under `/opt/grobid/logs/`):

```bash
docker exec -i -t {container_name} /bin/bash
```

The container name is given by the command:

```bash
docker container ls
```
