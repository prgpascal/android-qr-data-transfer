# android-qr-data-transfer
[![](https://jitpack.io/v/prgpascal/android-qr-data-transfer.svg)](https://jitpack.io/#prgpascal/android-qr-data-transfer)


Library that provides a secure data transmission channel between Android devices. It uses QR codes and Wi-Fi Direct technologies. 
## Why is it secure?
Because data is exchanged via sequences of QR codes, while a Wi-Fi Direct channel is used for acknowledgement ([ACK](https://en.wikipedia.org/wiki/Acknowledgement_(data_networks))) messages only. 

## How it works
* During the transmission, the sender device will act as a Server in a [Client-Server architecture](https://en.wikipedia.org/wiki/Client%E2%80%93server_model), while the receiver will act as a Client.
* Server and Client turn on the Wi-Fi (if it's not already turned on).
* Server and Client start the peer discovery.
* The Server shows to the Client the first QR code, containing the MAC address (necessary for the Wi-Fi Direct connection).
* Client uses its camera and captures the first QR code, parses the message and gets the Server MAC address.
* Client establishes a Wi-Fi Direct connection with the Server.
* While not all the messages have been exchanged:
    1. The Server encodes a new message into a QR code and waits for the ACK response.
    1. The Client reads the new QR code, gets the message and checks the digest:
        * If the message is valid, sends the ACK response.
        * If the message is not valid, tries reading the QR code again.
    1. The Server receives the ACK response.
        * If the ACK is not valid, stops the transmission with an error.
* If the transmission finishes with success, the data is returned to the Client. Otherwise, an error message is shown.

## Features
* It uses the [stop-and-wait protocol](https://en.wikipedia.org/wiki/Stop-and-wait_ARQ).
* It receives an *ArrayList\<String>* as input parameter, containing all the messages to be exchanged. For each *String* a new QR code will be created.
* If the Wi-Fi Direct is disabled, the library will automatically turn it on.
* Every exchanged message is checked with a digest ([SHA-256](https://en.wikipedia.org/wiki/SHA-2)).
* If an error occurs, the entire process is interrupted and no data is returned to the receiver.

## Import dependency
You can use JitPack to easily import this library into your project.  
Put this into your build.gradle:

```groovy
repositories {
  maven {
    url "https://jitpack.io"
  }
}

dependencies {
  compile 'com.github.prgpascal:android-qr-data-transfer:2.0.0'
}
```

## Usage
The sender starts the Activity passing an *ArrayList\<String>* as parameter, containing the messages to be sent:
```java
val intent = Intent(this, ServerTransferActivity::class.java)
val bundle = Bundle()
bundle.putStringArrayList(ServerTransferActivity.PARAM_MESSAGES, chunkedTextToTransfer)
intent.putExtras(bundle)
startActivityForResult(intent, DATA_EXCHANGE_REQUEST)
```
The receiver starts the Activity:
```java
intent = Intent(this, ClientTransferActivity::class.java)
startActivityForResult(intent, DATA_EXCHANGE_REQUEST)
```
The Client can handle the response in the *onActivityResult(...)* method:
```java
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == DATA_EXCHANGE_REQUEST) {
        if (resultCode == RESULT_OK) {
            val messages: List<String> = data?.getStringArrayListExtra(ServerTransferActivity.PARAM_MESSAGES)
                    ?: emptyList()
            setOnFinishMessage(messages)
        }
    }
}
```

## Dependencies
android-qr-data-transfer depends on the following external libraries:
* [ZXing](https://github.com/zxing/zxing): used to encode/decode the QR codes.
* [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded): used to scan sequences of QR codes.
* Others libraries from the [Android Support Library](http://developer.android.com/tools/support-library/index.html).

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
