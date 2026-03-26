---
title: "Affiliation Model"
description: "Affiliation and address labels"
sidebar_position: 5
---

# Affiliation Model

This model structures affiliation and postal-address blocks attached to authors.

## Main components

An affiliation can contain:

- organization names
- address parts
- markers that connect affiliations back to authors

## Main tags

- `<affiliation>` as the main container
- `<orgName type="institution">`
- `<orgName type="department">`
- `<orgName type="laboratory">`
- `<address>` for address structure
- `<marker>` for author-affiliation linking symbols or letters

Common address children include:

- `<addrLine>`
- `<postCode>`
- `<postBox>`
- `<settlement>`
- `<region>`
- `<country>`

## Important annotation habits

- leave punctuation and glue text outside tags when possible
- keep author-linking markers inside the affiliation material
- use multiple `orgName` levels when the source clearly distinguishes institution, department, and laboratory

## Complex affiliations

The old guidelines document two important patterns:

- joint laboratories linked to multiple institutions
- organizations or laboratories that appear under multiple names

In those cases, keyed organization entries can be used so the annotation still captures the affiliation structure cleanly.

## Related pages

- [Header Model](./header)
