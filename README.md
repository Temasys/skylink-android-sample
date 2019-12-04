# Temasys SDK for Android - Sample Application

The Sample Application(SA), which uses the latest SkylinkSDK for Android, demonstrates use of the SkylinkSDK to provide embedded real time communication in the easiest way. Just download, import to Android Studio, build and run!

In the SA, there are simple demos on:

  - Audio
  - Video
  - Chat/Message
  - DataTransfer
  - FileTransfers
  - ScreenShare

# Sample App structure

The architecture of Sample app is the way we organize the code to have a clear structure. We try to separate between the application layer and the SDK usage layer. With the separated parts, the user can easily change each part without changing the others and extend the functionality of the application. For example, the user can using different view components to display GUI of the application while keeping the same logics of using the SDK.

The **MVP (Model - View - Presenter)** architecture used in the Sample App mainly divided into three main parts: View - Presenter - Service

  - View: responsible for displaying GUI and getting user events.
  - Presenter: responsible for processing app logic and implementing callbacks sent from the SkylinkSDK
  - Service: responsible for sending requests to SkylinkSDK, using SkylinkConnection instance to communicate with the SkylinkSDK, the service part also contain the models (M) of the application.

For more details in the Sample app's architecture, please refer to (https://github.com/Temasys/skylink-android-sample/blob/master/SAArch.md)

## How to run the sample project

### Step-by-step guide for Android Studio

#### STEP 1
Clone this repository.

#### STEP 2
Import the project into Android Studio with File -> Open and select the project.

#### STEP 3
Follow the instructions [here](https://temasys.io/creating-an-account-generating-a-key/) to create an App and a key on the Temasys Console.

#### STEP 4
Create a copy of **config_example.xml** under **res/values** and name it **config.xml** (OR any other name unused in the res/value/ directory).
You may also choose to not create a new file and edit **config_example.xml** itself as needed.

#### STEP 5
Add your preferred values for **app_key** and **app_secret**. An appropriate App key and corresponding secret are required to connect to Skylink successfully.

# Instructions for populating the config.xml file.

SMR stands for Skylink Media Relay. For more information about SMR, see our [FAQs](http://support.temasys.com.sg/support/solutions/12000000313)

  - [What is MCU/SFU/Skylink Media Relay?](http://support.temasys.com.sg/support/solutions/articles/12000047799) and
  - [How can I enable/ disable Skylink Media Relay (MCU) for my App?](http://support.temasys.com.sg/support/solutions/articles/12000047800)

In the Sample App, keys with SMR enabled and those without are kept in 2 separate categories. You may provide a default App Key for each category. If not, simply uncomment the xml elements without changing their values.

Populate the boolean value for **is_app_key_smr** with whether the app should start by selecting **app_key_no_smr (false)** or **app_key_smr (true)** as the App key to use.

- Next, populate the values for **app_key_no_smr** and/or **app_key_smr** with the appropriate app keys.
- Next, populate the values for **app_key_secret_no_smr** and/or **app_key_secret_smr** with the appropriate app secret.
- Next, populate the values for **app_key_desc_no_smr** and/or **app_key_desc_smr** with the appropriate app description.

# Sample Configuration of a config.xml file

Example of config.xml file using a single App Key: 12345678-abc2-abc3-abc4-abc5abc6abc7 with the corresponding secret: 123456789123 with MCU set to OFF on the console:



    <!-- App Keys and secrets. -->
    <!--Uncomment in config.xml-->
    <!--Since MCU has been set to OFF on the console, the boolean value of is_app_key_smr has been set to false-->
    <bool name="is_app_key_smr">false</bool>

    <!--Uncomment in config.xml-->
    <!--Enter Details of your App Key for which SMR has been set to false below-->
    <string name="app_key_no_smr">12345678-abc2-abc3-abc4-abc5abc6abc7</string>
    <string name="app_key_secret_no_smr">123456789123</string>
    <string name="app_key_desc_no_smr">Non SMR Key for my awesome application</string>

    <!--Uncomment in config.xml-->
    <!--Since this is a case where only 1 key has been created with SMR set to false, you need not make any changes to the below-->
    <string name="app_key_smr">Any string.</string>
    <string name="app_key_secret_smr">Any string</string>
    <string name="app_key_desc_smr">Any string</string>


### For more examples, you may wish to refer to our guide: [How does the Sample App handle SMR Functionality?](http://support.temasys.com.sg/support/solutions/articles/12000064630)

#### STEP 6
Build the project

#### STEP 7
Run the sampleapp module


# Temasys SDK for Android

## SDK documentation

For more information on the usage of the Temasys SDK for Android, please refer to the following:

 - [Temasys SDK for Android Readme](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/readme.md)

## Tutorials and FAQs

[FAQS](http://support.temasys.com.sg/support/solutions/12000000313)



## Subscribe

Star this repo to be notified of new release tags. You can also view [release notes on our support portal](http://support.temasys.com.sg/support/solutions/folders/12000009705)

## Feedback

Please do not hesitate to reach get in touch with us if you encounter any issue or if you have any feedback or suggestions on how we could improve the Temasys SDK for Android or Sample Application.
You can raise tickets on our [support portal](http://support.temasys.io/) or on github.


## Copyright and License

Copyright 2019 Temasys Communications Pte Ltd Licensed under [APACHE 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
