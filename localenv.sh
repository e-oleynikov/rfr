#!/bin/bash

echo '### Stop and remove containers ###'
echo '### Stop containers ###'
docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)

echo '### Run mysql db ###'
docker run --name rfr-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret -v mysql-volume:/var/lib/mysql -d mysql:8.4.7


docker pull node:24-slim
docker run -it --rm --entrypoint sh node:24-slim
node -v # Should print "v24.22.1".
npm -v # Should print "11.19.1".