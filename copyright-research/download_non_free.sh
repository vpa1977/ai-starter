#!/bin/bash
mkdir -p tests/licenses-non-free
# Manual list of source-available/non-free licenses with known URLs
declare -A licenses=(
    ["Elastic-2.0"]="https://raw.githubusercontent.com/elastic/elasticsearch/master/licenses/ELASTIC-LICENSE-2.0.txt"
    ["SSPL-1.0"]="https://raw.githubusercontent.com/mongodb/mongo/master/debian/copyright" # Contains text
    ["BSL-1.1"]="https://raw.githubusercontent.com/MariaDB/server/main/COPYING.bsl"
    ["Confluent-1.0"]="https://raw.githubusercontent.com/confluentinc/cp-ansible/6.0.x/licenses/CONFLUENT_COMMUNITY_LICENSE.txt"
    ["Redis-RSALv2"]="https://raw.githubusercontent.com/redis/redis/unstable/licenses/RSALv2.txt"
    ["Redis-SSPLv1"]="https://raw.githubusercontent.com/redis/redis/unstable/licenses/SSPLv1.txt"
    ["BUSL-1.1"]="https://raw.githubusercontent.com/spdx/license-list-data/main/text/BUSL-1.1.txt"
    ["FSL-1.1-MIT"]="https://raw.githubusercontent.com/spdx/license-list-data/main/text/FSL-1.1-MIT.txt"
    ["FSL-1.1-ALv2"]="https://raw.githubusercontent.com/spdx/license-list-data/main/text/FSL-1.1-ALv2.txt"
)

for id in "${!licenses[@]}"; do
    echo "Downloading $id from ${licenses[$id]}..."
    curl -s "${licenses[$id]}" -o "tests/licenses-non-free/$id.txt"
done
