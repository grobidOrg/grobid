# Evaluation CRF and Deep Learning models

Here are some evaluations of individual Grobid models, using Grobid version **0.6.0**, using cross-validation (partitions of the annotated data into train and eval sets). 

## Accuracy 

For information, we report here some evaluations made with deep learning architectures for sequence labelling in comparison to CRF Wapiti. All results have been obtained with 10-fold cross-evaluations (average based on 10 sucessive partition of the training data). 

The Deep Learning architectures have been extended to use a feature channel for exploiting layout features (position of tokens, font, size, etc.), see [DeLFT](https://github.com/kermitt2/delft) and [here](https://github.com/kermitt2/delft/pull/82) for more details. Although we are working since more than 2 years on this approach, the usage of Deep Learning for Grobid tasks is still experimental and very far from something production ready with similar performances and scalability as the current GROBID stable version with CRF Wapiti.

However note that for some simpler NER-style tasks or especially for text classification, we found that Deep Learning approaches are significantly superior and usable, and, in contrast, we already developed and deployed several systems in production based on DeLFT. 

**Summary** 

Architectures: 

- [Architecture 1](https://github.com/kermitt2/delft/pull/82#issuecomment-587280497): using normal dropout after the BidLSTM of the feature channel

- [Architecture 2](https://github.com/kermitt2/delft/pull/82#issuecomment-588570868): using normal dropouts between embeddings and BidLSTM of the feature channel

- [Architecture 3](https://github.com/kermitt2/delft/pull/82#issuecomment-589442381): using recurrent dropouts on the BidLSTM of the feature channel

- Ignored features: using the standard BidLSTM-CRF without the use of any layout feature information

`Trainable=true` indicate that the features embeddings are trainable. 

All metrics has been calculated by running n-fold cross-validation with n = 10.

|Model |  CRF Wapiti | [Architecture 1](https://github.com/kermitt2/delft/pull/82#issuecomment-589447087) | [Architecture 1](https://github.com/kermitt2/delft/pull/82#issuecomment-593787846) (Trainable = true) | [Architecture 2](https://github.com/kermitt2/delft/pull/82#issuecomment-589439496) | [Architecture 2](https://github.com/kermitt2/delft/pull/82#issuecomment-593788260) (Trainable = true) | [Architecture 3](https://github.com/kermitt2/delft/pull/82#issuecomment-589523067) | [Architecture 3](https://github.com/kermitt2/delft/pull/82#issuecomment-594249488) (Trainable = true) | [Ignore features](https://github.com/kermitt2/delft/pull/82#issuecomment-586652333) |
|-- | -- | -- | -- | -- | -- | -- | -- | -- | 
|Affiliation-address | 0.8587 | 0.8709 | 0.8714 | 0.8721 | 0.872 | **0.873** | 0.8677 | 0.8668 | 
|Citation | 0.9448 | 0.9516 | **0.9522** | 0.9501 | 0.9503 | 0.9518 | 0.951 | 0.95 | 
|Date | **0.9833** | 0.9628 | 0.96 | 0.9606 | 0.9616 | 0.9631 | 0.961 | 0.9663 | 
|Figure | **0.9839** | 0.5594 | 0.5397 | 0.5907 | 0.4714 | 0.5515 | 0.6219 | 0.2949 | 
|Header | **0.7425** |0.7107 | 0.7102 | 0.7139 | 0.7156 | 0.7215 | 0.713 | 0.6764 | 
|Software | 0.7764 | 0.8112 | **0.8128** | 0.807 | 0.8039 | 0.8038 | 0.8084 | 0.7915 | 
|  |   |   |   |   |   |   |   |  |
|**Average** | **0.8816** | 0.8111 | 0.8077 | 0.8157 | 0.7958 | 0.8107 | 0.8205 | 0.7576 | 


## Runtime

To appreciate the runtime impact of Deep Learning models over CRF Wapiti, we report here some relevant comparisons. The following runtimes were obtained based on a Ubuntu 16.04 server Intel i7-4790 (4 CPU), 4.00 GHz with 16 GB memory. The runtimes for the Deep Learning architectures are based on the same machine with a nvidia GPU GeForce 1080Ti (11 GB). We run here a [software mention recognizer](https://github.com/ourresearch/software-mentions) model with Grobid as reference model, but any Grobid model would exhibit similar relative differences. 

|CRF Wapiti ||
|--- | --- |
|threads | tokens/s | 
|1 | 23,685 | 
|2 | 43,281|
|3 | 59,867 | 
|4 | 73,339|
|6 | 92,385 | 
|7 | 97,659|
|8 | 100,879 | 

| BiLSTM-CRF || 
| --- |--- | 
| batch size | tokens/s | 
| 50 | 24,774 | 
| 100 | 28,707| 
| 150 | 30,247|
| 200 | 30,520|

| BiLSTM-CRF+ELMo||
| ---| --- |
| batch size | tokens/s|
| 5 | 271|
| 7 | 365|

| SciBERT+CRF||
| ---| --- |
| batch size | tokens/s|
| 5 | 4,729|
| 6 | 5,060|

Additional remarks:

- Batch size is a parameter constrained by the capacity of the available GPU. An improvement of the performance of the deep learning architecture requires increasing the number of GPU and the amount of memory of these GPU, similarly as improving CRF Wapiti capacity requires increasing the number of available threads and CPU. Running a Deep Learning architectures on CPU is around 50 times slower than on GPU (although it depends on the amount of RAM available with the CPU, which can allow to increase the batch size significantly). 

- The BERT-CRF architecture in DeLFT is a modified and heavily optimized version of the Google Research [reference distribution of BERT](https://github.com/google-research/bert) (which does not support sequence labelling as such), with a final CRF activation layer instead of a softmax (a CRF activation layer improves F1-score in average by +0.30 for sequence labelling task). Above we run SciBERT, a BERT base model trained on scientific literature. Also note that given their limit of the size of the input sequence (512 tokens), BERT models are challenging to apply to several Grobid tasks which are working at document or paragraph levels. 

- Finally we present here the runtime for a single model. When using a cascade of models as in the Grobid core PDF structuring task, involving 9 different sequence labelling models, the possibility to use efficiently the batch size with the DL architecture is very challenging. In practice, as the batches will be often filled by 1 or a few input sequences, the runtime for a single document will be significantly longer (up to 100 times slower), and adapting the processing of multiple PDF in parallel with DL batches will require an important development effort. 
