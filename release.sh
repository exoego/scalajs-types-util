#! /bin/bash

cat version.sbt
unset SCALAJS_VERSION
sbt clean +test +macros/publishSigned sonatypeBundleRelease
