#Sample app architecture in MVP

The MVP (Model - View - Presenter) architecture used in the Sample App mainly divided into three main parts: View - Presenter - Service

    - View: responsible for displaying GUI and getting user events.
    - Presenter: responsible for processing app logic and implementing callbacks sent from the SkylinkSDK
    - Service: responsible for sending requests to SkylinkSDK, using SkylinkConnection instance to communicate with the Skylink SDK, the service part also contain the models (M) of the application.

## Class diagram:
https://github.com/Temasys/skylink-android-sample/blob/master/sampleapp/SA_MVP_Class_Diagram.png

## App structure:
    - Put related classes into package for clearer design: audio, chat, data transfer, file transfer, multi party videos, video, service, setting, utils
      + audio package: contain classes like AudioCallActivity, AudioCallContract, AudioCallFragment, AudioCallPresenter to implement the audio function.
      + chat package: contain classes to implement the chat or message function.
      + datatransfer package: contain classes to implement the data transfer function.
      + filetransfer package: contain classes to implement the file transfer function.
      + multipartyvideo package: contain classes to implement the multi videos function.
      + video package: contain classes to implement the video function.
      + service package: contain AudioService, VideoService,...SkylinkCommonService, SkylinkConnectionManager, PermissionService and model package to implement the service tasks of all functions
      + setting package: contain classes to implement the setting like user and room setting, application key setting
      + utils package: contains some utility classes

    - Model package: inside the service package and contains all models in the application.
      + KeyInfo : encapsulate info about application Key
      + PermRequesterInfo: encapsulate info about permission request
      + SkylinkPeer: encapsulate info about peer id and peer name
      + VideoLocalState: encapsulate info about video state (audio muted, video muted, camera)
      + VideoResolution: encapsulate info about video resolution (video width, video height, video frame rate)

## Class details:
    + BaseView : a common interface for all views including fragments
    + BasePresenter: a common abstract class for all presenters.
                    This class defined all methods which responsible for updating UI requested by the SDK. Some of those which do not need to be override in the concrete classes can be
                    implemented in the BasePresenter (such as just displaying toast to inform changes to user or logging info)
    + BaseService: a common interface for all services which are responsible for communicating with Skylink SDK.
    + Contract(s): interfaces to define the public methods of views, presenters, services classes for invoking each others and avoid circle relations between 2 classes
    + Fragment(s): the view instance to display the UI of the app, and get the user interaction like click on buttons, input text,...
    + Presenter(s): the presenter instance to implement all logics of the app such as tell the view to update UI to correct states, using the services to call to API in the SDK,...
    + Service(s): the service instance to communicate with API in the SDK. Only service classes should directly communicate with the SDK to send the request from application, these classes also help the user
    to custom the skylink configurations by using SkylinkConfig object.
    + SkylinkCommonService: an abstract class responsible for implementing all listeners which sent from the SDK, using BasePresenter to update GUI and keep track of peers in room, and singleton SkylinkConnection instance.
    + SkylinkConnectionManager: a class responsible for managing connection to the SDK like initializeSkylinkConnection, connectToRoom, disconnectFromRoom, generate SkylinkConnectionString
    + PermissionService: responsible for processing runtime permissions required by media usages.
