# Annotation guidelines for affiliation and address

## Introduction

The affiliation-address structure contains an informal description of an author’s present or past affiliation with one organization, for example an employer or a sponsor. It can group up to three elements: the name of the organization (`<orgName>`), its address (`<address>`) and an indice (`<marker>`).

As usually for GROBID models, text that do not belong to one of the left elements, in particular such as punctuations, syntactic sugar, etc. has to be be left untagged. A period that belongs to an abbreviation (`Univ.`, `Inc.`, `Blvd.`) is part of the word and stays inside the tag; a comma, semicolon or sentence-final period after the value goes outside it. Line break are indicated with `<lb>`. For example:

```xml
<affiliation>
	<orgName type="department">Institute of Materials Science</orgName>
	of the 
	<orgName type="institution">Technische Universität Darmstadt</orgName>,
	address:
	<lb/>
	<address>
		<addrLine>Petersenstraße 23</addrLine>,
		<postCode>D-64287</postCode>
		<settlement>Darmstadt</settlement>,
		<country>Germany</country>.
	</address>
</affiliation>
```

Note that this mark-up follows overall the [TEI](http://www.tei-c.org), which the addition of the custom element `<marker>`. 

## Analysis

### Affiliation components

We have selected three levels of organisations, which correspond to the three-tiered system of woS (Web of Science). Each one appear as value of the attribute `@type` of the element `<orgName>`:

* __institution__: corresponds to the global structure that hosts the author (can be a university or an institute, e.g. MIT, INRIA) - the largest scale of organization type.

* __department__: corresponds to a specialized division of the institution mentioned above - intermediate structure of organization type (department, faculty, institute)
if there is one.

* __laboratory__: corresponds to the research team or group, which the author belongs to (e.g. Joint Research Laboratory Nanomaterials) - the smallest scale of
organization type.


### Address components

This part contains the postal address of the organization which an author is affiliated to. 

Here are the different elements that can be used to structure the address:

* `<addrLine>` contains a postal address usually corresponding to a street and street number, or the name of known location for small village, hamlet, ... 

* `<postCode>` contains a numerical or alphanumeric code used as part of a postal address to simplify sorting or delivery of mail.

* `<postBox>` usually contains a numerical code corresponding to the postal box for mail delivery.

* `<settlement>` contains the name of a settlement such as a city, town, or village identified as a single geo-political or administrative unit.

* `<region>` contains the name of an administrative unit such as a state, province, or county, larger than a settlement, but smaller than a country.

* `<country>` contains the name of a geo-political unit, such as a nation, country, colony, or commonwealth, larger than or administratively superior to a region and smaller than a bloc. Optionally in the training data, the key attribute may be used to identify the country, according to ISO 3166-1 (this attribute is normally used for GROBID output results, but not in training data).

### Markers

_Markers_ are indice to relate an affiliation to one or several authors. They are tagged with the element `<marker>`, for example: 

```xml
	<affiliation>
		<marker>†</marker>
		<orgName type="laboratory">Grupo de Polıeros USB</orgName>,
		<orgName type="department">Departamento de Ciencia de los Materiales</orgName>,
		<orgName type="institution">Universidad Sim on Bolıar</orgName>,
		<lb/>
		<address>
			<postBox>Apartado 89000</postBox>,
			<settlement>Caracas</settlement>
			<postCode>1080-A</postCode>,
			<country>Venezuela</country>,
		</address>
	</affiliation>
```

```xml
	<affiliation>
		<marker>a</marker>
		<orgName type="institution">University of Bath</orgName>,
		<address>
			<addrLine>Claverton Down</addrLine>,
			<settlement>Bath</settlement>,
			<postCode>BA2 7AY</postCode>,
			<country>UK</country>
			<lb/>
		</address>
	</affiliation>
```

### Multiple institutions or multiple departments

This case typically corresponds to the affiliation of a joint laboratory. Use the “key” attribute to identify the different institutions (or departments), which the
joint laboratory belongs to.

Example: Joint Research Laboratory Nanomaterials, which is a joint laboratory of the Technische Universität Darmstadt and the Karlsruhe Institute of Technology.

```xml
<affiliation>
	<orgName type="laboratory">Joint Research Laboratory Nanomaterials</orgName>,
	<orgName type="institution" key="instit1">Technische Universität Darmstadt</orgName> and
	<orgName type="institution" key="instit2">Karlsruhe Institute of Technology</orgName>
	<lb/>
	<address>
		<addrLine> Petersenstrasse 23</addrLine>, 
		<postCode>D-64287</postCode>
		<settlement>Darmstadt</settlement>, 
		<country>Germany </country>
	</address>
</affiliation>
```

### Laboratory with several names

In the case of a laboratory described by several names (that's often the case for the UMR of French CNRS), we use a procedure similar to above.

Example: _GREMI_, also named _UMR 6606_, a joint laboratory of CNRS and Université d’Orléans

```xml
<affiliation>
	<orgName type="laboratory" key="lab1">GREMI</orgName>, 
	<orgName type="laboratory" key="lab1">UMR 6606</orgName>,
	<orgName type="institution" key="instit1">CNRS</orgName>, 
	<orgName type="institution" key="instit2">Université d'Orléans</orgName>
	<lb/>
	<address>
		<addrLine>14, rue d'Issoudun</addrLine>,
		<postBox>BP 6744</postBox>,
		<lb/>
		<postCode>45067</postCode>
		<settlement>Orléans cedex 2</settlement>,
		<country>France</country>
	</address>
</affiliation>
```

