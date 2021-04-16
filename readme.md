# Java in Statix Evaluation Projects

This repository contains several projects used to evaluate the Statix
specification in the `java-front` project. It consists projects
from Apache Commons and synthetic benchmarks.

## Apache Commons

The projects from Apache Commons were selected because they require only JDK8
and do not depend on generics. The specific projects are stand-alone and only
depend on JDK8.

## Synthetic Benchmarks

The synthetic benchmarks are a baseline for the other benchmarks. Currently
there is only one, which consists of isolated classes in separate packages, and
requires minimal interaction between different compilation units.
