Docker
===============

In an effort to ensure consistency of deployment, testing and build environments, we have a Dockerfile that developers
may use to build a docker image. The following instructions assume that the developer is using linux and has Docker installed::

    cd misc/
    sudo docker build -t phoebus:latest .
    sudo docker run -it phoebus:latest



From another shell(make sure to leave the previous shell open)::

    sudo docker cp phoebus [CONTAINER_ID]:/


You may also pull the following image from docker hub if you don't want to build the image yourself::

   sudo docker pull lgomezwhl/phoebus-ci:latest

