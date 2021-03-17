# simple-queue-plugin
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.222.3-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/)
# Usage
Plugin for Jenkins enabling changing a build queue from UI manually.\
There are two types of moves: one up/down or fast way to top/bottom. The third move type is added in filtered view to distinguish between top of filtered items and top of all items.
The user must have an Administer/Overall or MANAGE/Overall permission for changing the queue order. (since plugin version 1.3.5)\
    For using Manage permission is needed plugin: https://plugins.jenkins.io/manage-permission/
Orders buildable items only, for that reason [blocked](hhttps://stackoverflow.com/questions/56182285/difference-between-blocked-stuck-pending-buildable-jobs-in-jenkins) items do NOT have an arrow.<br />
![Screenshot](images/queue_screenshot.png "Simple Queue screenshot")
# Question & issues
Javadoc & releases can be found on https://repo.jenkins-ci.org/releases/io/jenkins/plugins/simple-queue/ \
As well as Jenkins core our plugin uses JIRA for reporting issues. https://issues.jenkins.io \
If you want to read more about this plugin, Jenkins queue and plugin development help yourself with 
44 pages long document in Czech language - https://github.com/otradovec/baka/blob/master/bakaText.pdf 
# For developers
![Sequence diagram](images/basicUsageSequence.png "Simple Queue screenshot")
![Sequence diagram](images/moveUpSequence.png "Simple Queue screenshot")
![Sequence diagram](images/resetSequence.png "Simple Queue screenshot")
## Testing build
mvn hpi:run
## Performing release
Always test connection before release. \
Testing connection: ssh -T git@github.com \
Release: mvn release:prepare release:perform
