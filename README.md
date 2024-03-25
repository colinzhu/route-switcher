# Route Switcher

Route Switcher is a reverse proxy server application developed using Java and Vert.x. 
It is designed to route the network traffic to different destinations based on the specified routing rules.

## Features

- Dynamic route network traffic to different destinations based on the URI prefix.
- Web interface to add, update, and delete routing rules on the fly.
- Web interface to monitor and debug your network traffic.

## Setup and Run

To set up and run the application, you need to have Java and Maven installed. Then, you can clone the repository and run the application using Maven.

```bash
git clone https://github.com/colinzhu/routeswitcher.git
cd routeswitcher
# update config.json to set the port
mvn clean install
mvn exec:java
```

The application will start and listen on the configured port.

## Using the UI

Once the application is running, you can open your browser and navigate to `http://localhost:<port>` to access the Route Switcher interface. Here, you can manage your routing rules.

Each rule consists of a `uriPrefix` and a `target`. The `uriPrefix` is the beginning of the URI that the rule should match, and the `target` is the server to which requests matching the `uriPrefix` should be routed.