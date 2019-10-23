# phoebus core

A set of modules containing shared services, descriptions of service provider interfaces, protocol libraries, etc. These core modules simplify application development by allowing the developer to focus only on the functionality of the application and not have to worry about implementing clients to multiple controls protocols, a preference manager, etc... The use of these core modules also optimized the use of common resources, like connection to various web services and protocol clients.

## Core Modules

### launcher

**core-launcher:** Provides the launcher for starting cs-studio/phoebus. 
### framework

**core-framework:** contains many useful services for building applications.
1. SelectionService: The service allows applications to publish the user section as well as allows other applications to register listeners to be notified on selection events.
2. AdapterService:  The services allows application to registers adapters which describe how to adapt an object from one type to another. This used in conjunction with the selection service can allow effective data transfer between applications without creating hard dependencies or creating future compatibility issues. More info about [Adapter Design Pattern](https://dzone.com/articles/adapter-design-pattern-in-java)

3. ApplicationService: The application service finds and registers all applications contributed via the implementation of the `AppResourceDescriptor` SPI.
4. PreferencesReader: A wrapper on the java preferences which uses a `properties` file to populate defaults.

### ui

**core-ui:** Contains support for the docking and layout managment of the phoebus workbench along with a collection of commonly used widgets, dialogs, and other controls.