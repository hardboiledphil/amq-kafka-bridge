# amq-kafka-bridge

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Purpose of this repo

This Quarkus app is created just to test a flow that includes the transfer of messages
from Kafka to AMQ or the reverse.

It's not intended to run in dev mode - just ./mvnw clean test

I haven't taken the build through to the build/deploy stage here but I will in a similar
piece of work at work and will feed back any findings from that application that will
end up in production

## Purpose of this repo

UPDATE: We ended up using this kind of flow BUT for java serialized objects and it generally
worked however the objects flowing through the bridges are unmarshalled from bytes 
and then marshalled from bytes back to objects as they pass through.  This means that
your code needs to have access to the source for the objects passing through or else
it can't do this unnecessary action.

In the end we got around to implementing the transfer objects as protos and the bridge
will happily just pass the byte arrays through the bridge without interfering with them.
Big advantages are less code dependency and zero chance of (un)marshalling issues in the
bridge flow.
