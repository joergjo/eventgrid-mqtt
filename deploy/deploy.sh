#!/bin/bash

resource_group=messaging-weu
namespace=mqtt-demo
client=client1
topic_space=test
perm_binding1=permissionbinding1
perm_binding2=permissionbinding2

thumbprint=$(openssl x509 -noout -fingerprint -sha256 -inform pem -in ../example.org.pem | awk -F= '{gsub(/:/,"",$2); print $2}')
# thumprint=$(openssl x509 -noout -fingerprint -sha256 -inform pem -in ../example.org.pem | awk -F= '{gsub(/:/,":",$2); sub(/^.* /,"",$0); print $2}')

az eventgrid namespace create \
    -n $namespace \
    -g $resource_group \
    --topic-spaces-configuration "{state:Enabled}"

az eventgrid namespace client create \
    -n $client \
    -g $resource_group \
    --namespace-name $namespace \
    --authentication-name $client-authnID \
    --client-certificate-authentication "{validationScheme:ThumbprintMatch,allowed-thumbprints:[$thumbprint]}"

az eventgrid namespace topic-space create \
    -g $resource_group \
    --namespace-name $namespace \
    -n $topic_space \
    --topic-templates "['contosotopics/topic1']"

az eventgrid namespace permission-binding create \
    -g $resource_group \
    --namespace-name $namespace \
    -n $perm_binding1 \
    --client-group-name '$all' \
    --permission publisher \
    --topic-space-name $topic_space

az eventgrid namespace permission-binding create \
    -g $resource_group \
    --namespace-name $namespace \
    -n $perm_binding2 \
    --client-group-name '$all' \
    --permission subscriber \
    --topic-space-name $topic_space