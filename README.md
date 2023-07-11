# SHACL and ShEx in the Wild❗

## A Community Survey on Validating Shapes Generation and Adoption

Knowledge Graphs (KGs) are the de-facto standard to represent heterogeneous domain knowledge on the Web and within organizations. Various tools and approaches exist to manage KGs and ensure the quality of their data. Among these, the Shapes Constraint Language (SHACL) and the Shapes Expression Language (ShEx) are the two state-of-the-art languages to define validating shapes for KGs. In the last few years, the usage of these constraint languages has increased, and hence new needs arose. One such need is to enable the efficient generation of these shapes. Yet, since these languages are relatively new, we witness a lack of understanding of how they are effectively employed for existing KGs. Therefore, in this work, we answer **How validating shapes are being generated and adopted?** Our contribution is threefold. First, we conducted a community survey to analyze the needs of users (both from industry and academia) generating validating shapes. Then, we cross-referenced our results with an extensive survey of the existing tools and their features. Finally, we investigated how existing automatic shape extraction approaches work in practice on real, large KGs. **Our analysis shows the need for developing semi-automatic methods that can help users generate shapes from large KGs.**

Read the paper: [https://dl.acm.org/doi/10.1145/3487553.3524253](https://dl.acm.org/doi/10.1145/3487553.3524253)

Visit our website for more details: [https://relweb.cs.aau.dk/validatingshapes/](https://relweb.cs.aau.dk/validatingshapes/)

## Datasets
We have used the following datasets:

1. **DBPedia:** We used  [dbpedia script](https://github.com/Kashif-Rabbani/validatingshapes/blob/main/dbpedia/download-dbpedia.sh) to download all the dbpedia files listed [here](https://github.com/Kashif-Rabbani/validatingshapes/blob/main/dbpedia/dbpedia-files.txt).
2. **YAGO-4:** We downloaded YAGO-4 English version from [https://yago-knowledge.org/data/yago4/en/](https://yago-knowledge.org/data/yago4/en/).
2. **LUBM:** We generated LUBM dataset following the guidelines available at [LUBM's official Website](http://swat.cse.lehigh.edu/projects/lubm/).

Statistics of these datasets is shown in the table below:

|                                	| DBpedia 	| YAGO-4 	| LUBM  	|
|--------------------------------	|--------:	|-------:	|-------	|
| # of triples                   	|    52 M 	|  210 M 	| 91 M  	|
| # of distinct objects          	|    19 M 	|  126 M 	| 12 M  	|
| # of distinct subjects         	|    15 M 	|    5 M 	| 10 M  	|
| # of distinct literals         	|    28 M 	|  111 M 	| 5.5 M 	|
| # of distinct RDF type triples 	|     5 M 	|   17 M 	| 1 M   	|
| # of distinct classes          	|     427 	|  8,902 	| 22    	|
| # of distinct properties       	|   1,323 	|    153 	| 20    	|
| Size in GBs                    	|     6.6 	|  28.59 	| 15.66 	|

You can download a copy of these datasets from our [single archive](http://130.226.98.152/www_datasets/).


## SHACL Shapes
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.5958986.svg)](https://doi.org/10.5281/zenodo.5958986)

We have published the extracted SHACL shapes of all three datasets on [Zenodo](https://doi.org/10.5281/zenodo.5958986).
Additionally, we have also made available an executable Jar file of our application on Zenodo to extract SHACL shapes from RDF datasets in `.nt` format.

---
:star: **Good News** :star: Source Code is also available now!

We have made the source code available in the [code](https://github.com/kashif-rabbani/validatingshapes/tree/main/code/shacl) directory along with instructions on how to run the code. 

---

### How to run the Jar?

- Download the Jar from the [Zenodo](https://doi.org/10.5281/zenodo.5958986)
- Update the configuration in [config.properties](https://github.com/Kashif-Rabbani/validatingshapes/blob/main/config.properties) file 
- Follow [these](https://sdkman.io/install) steps to install sdkman and execute the following commands to install the specified version of Java and Gradle.

        sdk list java
        sdk install java 17.0.2-open 
        sdk use java java 17.0.2-open 
        
        sdk list gradle
        sdk install gradle Gradle 7.4-rc-1
        sdk use gradle Gradle 7.4-rc-1

- In case you are using docker, you should use `gradle:7.3.3-jdk17-alpine`.
- Run the jar file by passing the config file as a parameter: `java -jar shacl-generator-program.jar config.properties`


### Analyzing the State-of-the-art tools 

We ran some experiments to find out the real capabilities of the following existing tools for automatically extracting shapes from RDF graphs.

#### 1. SheXer
[https://github.com/DaniFdezAlvarez/shexer](https://github.com/DaniFdezAlvarez/shexer)


#### 2. ShapeDesigner
[https://gitlab.inria.fr/jdusart/shexjapp](https://gitlab.inria.fr/jdusart/shexjapp)

#### 3. SHACLGEN
[https://pypi.org/project/shaclgen/](https://pypi.org/project/shaclgen/)


##### Persistent URI & Licence:
The content present in this repository is available at
[https://github.com/Kashif-Rabbani/validatingshapes](https://github.com/Kashif-Rabbani/validatingshapes) under [Apache License 2.0](https://github.com/Kashif-Rabbani/validatingshapes/blob/main/LICENSE) .

### Citing the work
Please cite us if you use the code in your project or publication

```bibtex
@inproceedings{DBLP:conf/www/RabbaniLH22,
  author       = {Kashif Rabbani and
                  Matteo Lissandrini and
                  Katja Hose},
  title        = {{SHACL} and ShEx in the Wild: {A} Community Survey on Validating Shapes
                  Generation and Adoption},
  booktitle    = {{WWW} (Companion Volume)},
  pages        = {260--263},
  publisher    = {{ACM}},
  year         = {2022}
}
```

