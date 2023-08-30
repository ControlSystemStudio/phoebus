#!/bin/bash

if [ $# -eq 0 ]; then
    echo "Invalid usage. Please specify path to the file tree_node_mapping.json as first argument."
    exit 1
fi

echo "Creating temporary index"
curl -XPUT "http://localhost:9200/saveandrestore_tree_tmp"  -H 'Content-Type: application/json' --data @$1
echo

echo "Copying data to temporary index"
curl -XPOST "http://localhost:9200/_reindex"  -H 'Content-Type: application/json' -d '{"source":{"index": "saveandrestore_tree"}, "dest":{"index":"saveandrestore_tree_tmp"}}'
echo

echo "Flushing data to temporary index"
curl -XPOST "http://localhost:9200/saveandrestore_tree_tmp/_flush"
echo

echo "Delete original index"
curl -XDELETE "http://localhost:9200/saveandrestore_tree"
echo

echo "Re-creating original index"
curl -XPUT "http://localhost:9200/saveandrestore_tree"  -H 'Content-Type: application/json' --data @$1
echo

echo "Copying data to original index"
curl -XPOST "http://localhost:9200/_reindex"  -H 'Content-Type: application/json' -d '{"source":{"index": "saveandrestore_tree_tmp"}, "dest":{"index":"saveandrestore_tree"}}'
echo

echo "Flushing data to original index"
curl -XPOST "http://localhost:9200/saveandrestore_tree/_flush"
echo

echo "Delete temporary index"
curl -XDELETE "http://localhost:9200/saveandrestore_tree_tmp"
echo