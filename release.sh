#! /bin/bash

export SCALAJS_VERSION=0.6.33
sbt clean +test +publish
unset SCALAJS_VERSION
sbt clean +test +publish
