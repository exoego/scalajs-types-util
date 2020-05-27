#! /bin/bash

export SCALAJS_VERSION=0.6.33
sbt clean +test +macros/publish
unset SCALAJS_VERSION
sbt clean +test ++macros/publish
