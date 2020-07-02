# simple-queue-plugin
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.164.1-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)

# Usage
Plugin for Jenkins enabling changing build queue from UI manually.\
There are two types of moves: one up/down or fast way to top/bottom.
The user must have ADMINISTER permission for changing queue order.\
Orders buildable items only, for that reason blocked items do NOT have an arrow.<br />
![Screenshot](images/queue_screenshot.png "Simple Queue screenshot")
# For developers
![Sequence diagram](images/basicUsageSequence.png "Simple Queue screenshot")
![Sequence diagram](images/moveUpSequence.png "Simple Queue screenshot")
![Sequence diagram](images/resetSequence.png "Simple Queue screenshot")
