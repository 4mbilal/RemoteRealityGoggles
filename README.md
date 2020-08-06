# RemoteRealityGoggles  

Android Studio project to implement a VR Goggles application. The application simply receives the stereo side-by-side video as an MJPEG stream and sends back Head Tracking signals. The app is based on Google Cardboard framework. The host platform (Jetson Nano/Raspberry Pi) receives the head tracking signals to control a servo-motor structure which moves a stereo webcam and encodes the video as an MJPEG stream.  

Host Platform -> Stereo video stream -> Android App  	(This repository)  
Android App   -> Headtracking Data   -> Host Platform (https://github.com/4mbilal/VR_RemoteReality)