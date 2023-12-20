# JawaCore

JawaCore is a core plugin needed for use of JawaPermissions, JawaChat, and JawaCommands (JawaToolBox does not use JawaCore and is a standalone plugin). JawaCore contains many libraries needed by the JawaPlugins to perform common actions that each plugin may need to do.

JawaCore resolves use pre-join identificaiton vs bans, logs user session activities, manages ElasticSearch indexes, formatting and manipulation of common data types and files, chat logging, and more.

## Getting Started

1. Clone the repo:
```
git clone https://github.com/arthurbulin/JawaCommands
```
2. Either open the project in Netbeans and build with the Maven plugin or build from the CLI. NOTE: I never build from the CLI because I'm lazy.
3. You need the Elastic Search Database installed. Currently .
4. You will need to initialize the Elastic Search indexes for the plugin. To do this you need another Java application I have written, but not yet put on GitHub. Once it is up there you can create a proper index.

### Prerequisites

You will need 
    - Maven to build the plugin
    - Java 17
    - ElasticSearch (7.15.0 is the current version)
    - Minecraft server running on Paper/Spigot (Paper is prefered and I will probably not support Spigot by the end of 1.20 support)

### Installing

1. Place the JawaCore-1.20.X-#.#.jar into the server's plugin folder and start the server. Needed configuration files will be created.
2. Shutdown your server
2. Edit the config.yml and include your Elastic Search database details. See the Configuration Paramaters section below.
4. Restart your server. If you have any malformed config files the server will let you know, although I cannot guarantee how enlightening what it tells you will be.
#5. When you join the server it should inform you that you have been installed. This means it's working. Every time you join after that it will inform you that you have been loaded.

## Configuration Parameters
Within the JawaCore folder you will find a config.yml file. This config must be strictly YAML formatted or it may fail to load and will be overwritten.

* server-name: \<Arbitrary string to identify this server instance\>
  - This value is used in a number of places. This identifies the server not only within JawaCore but also within the other JawaPlugins. JawaChat will be big on this as the crosslink feature uses this name, along with a secret ID, to authenticate the server. This is also used identify chat and session logs within ElasticSearch.
* eshost: \<resolvable hostname or IP address of your ElasticSearch server>
* esport: \<port number for your ElasticSearch server\>
* esuser: \<the user within ElasticSearch that the server should login as\>
  - This user should be created within ElasticSearch (either manually or through Kibana) and have the manage, monitor, and transport permissions on the indexes needed by the JawaPlugins. For the love of the gods do not use the elastic user!
* espass: \<the plaintext password you will use to access the ElasticSearch server\>
  - This is the password for the esuser above.
* es-x-security: \<true/false\>
  - This is true by default. It enforces authentication for ElasticSearch. esuser and espass are not used if this is set to false. ElasticSearch must be configured with x-pack security enabled.
* chat-indexing:
    * track-chat: \<true/false\>
      - If true a data stream will be created within ElasticSearch to track chat details.
    * chatlog-index-identity: \<Arbitrary string for server index identification\>
      - This will be used to create an identiy for the data stream. i.e. chatlog-minecraft-<identity>. Individual messages will list the server, this comes from server-name.
    * log-routed-chat: \<true/false\>
      - When true messages routed to the server via CrossLink will be logged also. When false these messages will not be logged. I recommend turning this to false if you have multiple CrossLinked servers logging to data streams within the same ElasticSearch as an index pattern can grab them all and you won't end up with duplicates.
* messages:
    * permission-message: "&c > You do not have permission to perform this command. If you believe this is in error please contact your server administrator"
    * player-not-found: "&c > Error: That player wasn't found! Try their actual minecraft name instead of nickname"
      - I would leave all these alone. I don't even remember if they are getting used. But I wanted to be able to customize the messages.
* index-customization:
  * players: \<arbitrary string, no spaces please\>
    - This is the index name that will be used by the plugins to store player data. I recommend naming it players-<minecraft version> i.e. players-120. So that in the future you can keep database versions seperate if I change the schema. Although if I do I'll try to ensure the update is automatic but up to this point, it is NOT.
  * sessions: \<arbitrary string, np spaces please\>
  * - This is the index name that will be used by the plugins to store player session data. I recommend naming it sessions-<minecraft version> i.e. sessions-120. So that in the future you can keep database versions seperate if I change the schema. Although if I do I'll try to ensure the update is automatic but up to this point, it is NOT.

## Commands and Permissions

debug:
description: This command toggles the debug state of the server.
usage: /debug
permission: jawacore.debug

## Built With

* [NetBeans](https://netbeans.org/) - The IDE used
* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Arthur Bulin aka Jawamaster** - [Arthur Bulin](https://github.com/arthurbulin)

## License

This project is licensed under the MIT License. Just don't be a jerk about it.
