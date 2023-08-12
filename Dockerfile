FROM eclipse-temurin:8-jre

# Create a non root user
ARG USERNAME=archiva
ARG USERGROUP=$USERNAME
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN mkdir -p /opt/archiva/app \
    && mkdir -p /opt/archiva/data/conf/ \
    && mkdir /opt/archiva/webapp \
    && groupadd --gid $USER_GID $USERGROUP \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && chown -R $USER_UID:$USER_GID /opt/archiva

USER $USERNAME

COPY --chown=$USER_UID:$USER_GID target/*.jar /opt/archiva/app/app.jar
COPY --chown=$USER_UID:$USER_GID webapp.war /opt/archiva/webapp
COPY --chown=$USER_UID:$USER_GID run.sh /opt/archiva/

EXPOSE 8080

ENTRYPOINT ["/opt/archiva/run.sh"]
