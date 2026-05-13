#!/bin/bash
mkdir -p tests/licenses
while read -r id; do
    echo "Downloading $id..."
    curl -s "https://raw.githubusercontent.com/spdx/license-list-data/main/text/$id.txt" -o "tests/licenses/$id.txt"
    if [ ! -s "tests/licenses/$id.txt" ]; then
        echo "Failed to download $id from SPDX, skipping..."
        rm "tests/licenses/$id.txt"
    fi
done < tests/license_ids.txt
