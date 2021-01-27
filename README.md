# simple-queue-plugin
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.164.1-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/)
# Usage
Plugin for Jenkins enabling changing a build queue from UI manually.\
There are two types of moves: one up/down or fast way to top/bottom. The third move type is added in filtered view to distinguish between top of filtered items and top of all items.
The user must have an ADMINISTER permission for changing the queue order.\
Orders buildable items only, for that reason blocked items do NOT have an arrow.<br />
![Screenshot](images/queue_screenshot.png "Simple Queue screenshot")
# Question & issues
As well as Jenkins core our plugin uses JIRA for reporting issues. https://issues.jenkins.io
# For developers
![Sequence diagram](images/basicUsageSequence.png "Simple Queue screenshot")
![Sequence diagram](images/moveUpSequence.png "Simple Queue screenshot")
![Sequence diagram](images/resetSequence.png "Simple Queue screenshot")
