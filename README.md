# PeerToPeerBle
This is an example project showing how to configure Android devices as both a BLE client (central) and BLE server (peripheral). This project is meant to accompany a talk I gave at [DroidKaigi 2020](https://droidkaigi.jp/2020/en/).

**Slides:** https://drive.google.com/file/d/1gASMtgCIra0u35VPVcULZEblJzxlZ-aK/view?usp=sharing

**Video:** *Coming soon*

## Use Case
This sample projects demonstrates a one-off BLE connection where two Android devices, both acting as a client and a server, come into contact with each other, exchange some data, then disconnect. However, with minor changes, the implementation shown here can be altered to fit any use case.

#### One-off data exchange
<img width="932" alt="Screen Shot 2020-06-02 at 15 13 33" src="https://user-images.githubusercontent.com/26476452/83487050-4a587a80-a4e5-11ea-810a-f28e683459f6.png">

#### BLE class structure
<img width="886" alt="Screen Shot 2020-06-02 at 15 29 03" src="https://user-images.githubusercontent.com/26476452/83487331-cfdc2a80-a4e5-11ea-915c-dd8ff8104143.png">


## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
