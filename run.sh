#!/bin/sh
exec java -Dappserver.base=/opt/archiva/data -Dappserver.home=/opt/archiva/data -jar /opt/archiva/app/app.jar --spring.profiles.active=prod --apache-archiva.configuration.path=/opt/archiva/webapp/webapp.war 
