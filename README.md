Streaming implemented for both: downloading csv from web and for serving jsons result by this service.

# Running tests
Internet connection required for one test suit.

`sbt test`

# Running
`sbt run`

# Testing running instance:
For smaller files the OpenApi subpage will be fine:

`http://localhost:8080/docs`

For bigger files better to use the command line:

``curl -X 'GET'   'http://localhost:8080/result/3299b01a-1745-424a-9606-3a659740a07c'   -H 'accept: text/plain'``
