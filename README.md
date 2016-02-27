# android-qr-data-transfer
Library that provides a secure channel for data transmission between Android devices. It exploits QR-codes and Wi-Fi Direct technologies. 

## Why is it secure?
Because the messages are exchanged via sequences of QR codes, while a Wi-Fi Direct channel is used for acknowledgement messages only. 

## How it works
* During the transmission the sender device will act as a Server in a Client-Server architecture, while the receiver will act as a Client.
* Server and Client turn on the Wi-Fi if not available.
* Server and Client start the peer discovery.
* The Server shows to the Client the first QR code, containing its MAC address ( necessary for the Wi-Fi Direct connection).
* Client uses its camera and captures the first QR code, parses the message and gets the Server MAC address.
* Client enstablishes a Wi-Fi Direct connection with the Server.
* While not all the messages have been exchanged, loop the following:
    1. The Server encodes a new message into a QR code and waits for the ACK response.
    1. The Client reads the new QR code, gets the message and checks the digest:
        * If the message is valid, send the ACK response.
        * If the message is not valid, try reading the QR code again.
    1. The Server receives the ACK response.
        * If the ACK is valid, repeat from 1.
        * If the ACK is not valid, show an error message and exit.
* If the transmission finishes with success, the data is returned to the Client. Otherwise an error message is shown.

## Features and advantages
* It uses (https://en.wikipedia.org/wiki/Stop-and-wait_ARQ)[stop-and-wait protocol].
* It receives an ArrayList<String> as input paramether, containing all the messages to be exchanges. For each String a new QR code will be created.
* If the Wi-Fi Direct is disabled, the library automatically turns it on.
* Every exchanged message is checked with a digest (SHA-256).
* Once set the data to be transferred, the library will do the rest, managing the connection and data transfer. 
* If an error occured, the entire process is interrupted and no mesages are returned to the Client.

## Usage
The Server starts the Activity passing to it an ArrayList<String> containing the messages to be sent.
```java
// Define the params
Bundle b = new Bundle();
Intent intent = new Intent(this, TransferActivity.class);
b.putBoolean("i_am_the_server", true);
b.putStringArrayList("messages", messages);
intent.putExtras(b);

// Start the Activity for result
startActivityForResult(intent, DATA_EXCHANGE_REQUEST);
```
The Client starts the Activity in this manner:
```java
// Define the params
Bundle b = new Bundle();
Intent intent = new Intent(this, TransferActivity.class);
b.putBoolean("i_am_the_server", false);
intent.putExtras(b);

// Start the Activity for result
startActivityForResult(intent, DATA_EXCHANGE_REQUEST);
```
The Client can than handle the response in the onActivityResult(...) method:
```java
@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == DATA_EXCHANGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                messages = data.getStringArrayListExtra("messages");
            }
        }
        ...
    }
```

## Diagrams
Some UML diagrams for this project are available: 
* Class diagram 
* Sequence diagram (Server device).
* Sequence diagram (Client device)

## Dependencies
android-qr-data-transfer depends on the following external libraries. [con i link]
* ZXing: used to encode/decode the QR codes.
* ZXing Android Embedded: used to scan sequences of QR codes.
* Others libraries from Android Support Library.

## License
	Copyright 2016 Riccardo Leschiutta

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
