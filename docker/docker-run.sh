#!/bin/bash

docker run -d \
  --name shoemoa-postgres \
  -p 5432:5432 \
  -v shoemoa_pgdata:/var/lib/postgresql/data \
  shoemoa-postgres
