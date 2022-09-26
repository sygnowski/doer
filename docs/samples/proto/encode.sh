#!/bin/bash


doer proto \
  -d ../../../proto-doer/build/descriptors/test.desc \
  -m GreekSymbols \
  -e json \
  --base64 MssBCj90eXBlLmdvb2dsZWFwaXMuY29tL2lvLmdpdGh1Yi5zN2kuZG9lci5wcm90by5kdW1teS5HcmVla1N5bWJvbHMShwEKQgovdHlwZS5nb29nbGVhcGlzLmNvbS9nb29nbGUucHJvdG9idWYuU3RyaW5nVmFsdWUSDwoNdGhpcyBpcyBhbHBoYRJBCi90eXBlLmdvb2dsZWFwaXMuY29tL2dvb2dsZS5wcm90b2J1Zi5TdHJpbmdWYWx1ZRIOCgx0aGlzIGlzIGJldGE=

doer proto \
  -d ../../../proto-doer/build/descriptors/test.desc \
  -m GreekSymbols \
  -f ./greekSample01.json \
  -e bin


