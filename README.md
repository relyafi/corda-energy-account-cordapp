<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>
<h1>Energy Account CorDapp</h1>

### Table of contents
1. [Getting started](#getting-started)
2. [Using the app](#using-the-app)
3. [Advanced](#advanced)

### Getting started
<a name="getting-started"/>
This project comes with a pre-configured test network to allow the application to be quickly run and 
tested. The test network is comprised of:

* A [Notary](https://docs.corda.net/key-concepts-notaries.html) node
* Two supplier nodes - "British Energy" and "UK Power", that represent two different energy suppliers.
  Each of these is formed of a Corda node, and a separate web-server that provides a Web UI which
  communicates with the corresponding Corda node's RPC interface via a RESTful API
* A government regulator node, representing the regulatory body that oversees suppliers. This also
    has a Corda node and corresponding web-server

The nodes can either be run up directly on your local machine, or via Docker compose. The latter
approach is recommended.

The `gradlew` and `runnodes` scripts referred to in the below steps have two variants. Run the `.bat`
versions on Windows, and the scripts without an extension on Linux.

##### Running with Docker Compose
As you might expect, you must have Docker Compose installed on your machine in order for this to work.
To run the application in this mode, you should:

1. Run `gradlew clean prepareDockerNodes`. You only need to run this the first time you are starting
   the application, unless you are changing between running locally or with Docker Compose.
   **Running this command will destroy any existing nodes and their associated data.**
2. In the project root directory, run `docker-compose up`. This will bring up all nodes
   and associated web servers.

To close the app, simply end the process in the terminal window.

##### Running Locally
To run the application locally, you should:

1. Run `gradlew clean prepareLocalNodes`. You only need to run this the first time
   you are starting the application, unless you are changing between running locally or with Docker Compose.
   **Running this command will destroy any existing nodes and their associated data.** 
2. Go to `build/nodes` and execute `runnodes`. This will bring up your Corda nodes. **Make sure your 
   nodes have successfully started before continuing.** You can verify this by looking for the following
   in the console:
   
       `Node for ... started up and registered in ... sec`
      
   If this doesn't happen, yoy may need to hit enter to get the shell to move on, as it can sometimes
   get stuck. You should have 4 nodes running in total; 2 suppliers, the government regulator, and the
   notary.
3. Run each of the following gradle commands in separate terminals:
   ```
   gradlew runBritishEnergyWebCli
   gradlew runUKPowerWebCli
   gradlew runGovtRegulatorWebCli
   ```
 
 When you have finished running the app, you need to execute the `bye` command in each Corda node, and
 also terminate each gradle task for the web servers manually (since these run synchronously).

### Using the app
<a name="using-the-app"/>
The supplier and government regulator nodes provide web interfaces via ports that are mapped to your
local machine:

| Node                 | Address           |
|----------------------|-------------------|
| British Energy       | <localhost:10050> |
| UK Power             | <localhost:10051> |
| Government Regulator | <localhost:10052> |

Each of these links will start with an index page that contains links to each of the nodes relevant
web pages, so you can use any of the above links and navigate to all nodes from there.

Each page provides different views of the data:

* The customer portal acts as a login for a single customer, based on a known account id
* The supplier portal provides access to all accounts that currently belong to that supplier
* The government regulator account monitor provides access to all accounts across all suppliers

You should create accounts via the supplier portal prior to using the other pages.

##### Baseline data
For convenience, a baseline data set is provided to allow quick creation of a number of accounts
after initialising the nodes. To load this data, you must install the 
[Postman](https://www.getpostman.com/) application. You can then use the following files under
`clients/src/main/resources/baselineData`:

* `dataLoad.postman_collection.json` - Postman collection which should be imported into the application.
* `baselineData.json` - Data file to load within the collection runner window.

### Advanced
<a name="advanced"/>
When running via Docker, it is possible to access the Corda shells for each node via SSH on ports
mapped to your local machine:

| Node                 | Port  |
|----------------------|------ |
| Notary               | 10113 |
| British Energy       | 10133 |
| UK Power             | 10143 |
| Government Regulator | 10123 |
