# Google Docs Connector

The Google Docs connector presents cards as newly mentioned comments to the user. User can take actions like invite user ,post a message and reply to the mentioned comment to the notes.

# Clojure Engine Build

1. This connector uses a "Card Build Engine" to generate hero cards.

2. The schema of hero card is defined in a configuration file(written in [clojure](https://clojure.org/)) [config.clj](/src/main/resources/config.clj) which is picked up by the Card Build Engine at runtime to generate the cards.

3. This configuration file can have business rules like selection of the fields, filtering, sorting and calculations on the input data.

4. Any modifications to the notification card rules/structure can be made directly to the config.clj file. The config.clj is part of the app jar and this jar can be signed. once the new jar is redeployed these changes will be reflected in the card.

5. Because this jar can be signed, config.clj cannot be tampered. 

For generic details on how to build, install, and configure connectors, please see the [README](https://github.com/vmware/connectors-workspace-one/blob/master/README.md) at the root of this repository.


