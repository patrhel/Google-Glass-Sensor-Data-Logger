# Google-Glass-Sensor-Data-Logger
This software is used in the area of activity recognition using a supervised machine learning approach. It allows to record sensor data and to perform a real-time ground truth labeling 

## Functionality

### Sensor Data logging
This software is able to record motion, orientation, visual, acoustic, and beacon proximity sensor data on the Google Glass. Furthermore,  a module that allows one to read out Google Glass' infrared eye proximity sensor is integrated. This sensor information is used to perform real-time blink detection. For each sensor, the data is saved to a separate CSV file.

### Real-time ground truth labeling
An integrated activity logging interface allows real-time ground truth labeling by the use of Google Glass' touchpad.
First, a categorical overview is shown to the user by Google Glass' display. The respective activity label is simply logged through the act of selecting an activity. The overview consists of three categories. A category can be selected by the number of fingers that are used. Based on the swipe direction, the user chooses a particular activity. An activity label contains the name of the activity as well as the activity's start timestamp. Due to the way the activity logging interface was integrated into the sensor data logging software, it is possible to continuously label in a single sensor data recording process while performing several successive activities. Once the label is set, Google Glass' display is automatically turned off to avoid influencing the participant's behavior while executing the activity. 


## Possible Applications

### Machine Learning - Activity Recognition
This software is used in the area of activity recognition using a supervised machine learning approach. It allows to record sensor data and to perform real-time ground truth labeling. 

### Internet of Things
The integrated beacon detection module allows the Google Glass to detect nearby devices using Bluetooth Low Energy (BLE) technology. Devices sending out a beacon signal will thus be identifiable through their identification number. Furthermore, the integrated module allows calculating the distance to the beacon. Based on this data, applications in the area of IoT can be realized.

### Smart Home Applications
The integrated beacon detection module allows the Google Glass to detect nearby devices using Bluetooth Low Energy (BLE) technology. Devices sending out a beacon signal will thus be identifiable through their identification number. Furthermore, the integrated module allows calculating the distance to the beacon. Based on this data, smart home applications can be realized.

### Blink Detection
A module which allows one to read out Google Glass' infrared eye proximity sensor is integrated. This sensor information can be used to perform real-time blink detection.
