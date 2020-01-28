# Thesis

Web Application for a thesis about the Vaadin framework version 8+.

This application allows you to get and process data from NASA and MAPBOX restful API'S using the Vaadin framework version 8+.

Instructions on how to set up a working environment for using the Thesis application follow below.


## Setting up IntelliJ IDEA to work with the application.

1. Install and run IDEA. Community Edition should work but having the Ultimate Edition is highly recommended.
1. Make sure the Git and Maven plugins are installed and properly configured.
1. Clone the repository using menu VCS -> Checkout from Version Control -> Git -> Git Repository URL -> https://github.com/galasd/Thesis.
1. Open cloned repository as a maven object using the pom.xml file.


## Compilation

The Thesis application is structured as a Maven project. The project can be compiled via Maven compiler plugin using the 'compiler:compile' command.


## Running an application server

The application was developed using the Apache Tomcat server version 8. It is though recommended to use it to run the application.

Instructions on how to set up and run the application server can be found here: 
https://tomcat.apache.org/tomcat-8.5-doc/index.html

## Add the current application server to IntelliJ IDEA
1. Open 'Run' menu  and click 'Edit Configurations'.
1. Click green + sign at top left corner and select 'Tomcat server'.
1. Set up the configuration using the instructions from the link above.
1. Run the configuration and open URL [http://localhost:8080/DiplomovaPrace]



