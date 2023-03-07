# simple-queue-plugin
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.222.3-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/) \
Plugin for Jenkins enabling changing a build queue order from UI manually.
# Usage
Usage video: https://youtu.be/anyGsJIa020 \
There are two types of moves: one up/down or fast way to top/bottom. The third move type is added in filtered view to distinguish between top of filtered items and top of all items.
The user must have an Administer/Overall or MANAGE/Overall permission for changing the queue order. (since plugin version 1.3.5)\
    For using Manage permission is needed plugin: https://plugins.jenkins.io/manage-permission/
Orders buildable items only, for that reason [blocked](hhttps://stackoverflow.com/questions/56182285/difference-between-blocked-stuck-pending-buildable-jobs-in-jenkins) items do NOT have an arrow.<br />
# CLI
When hovering over priority arrows, you cans see that it executes special url aka:
```
http://jenkins_url/simpleMove/move?moveType=DOWN_FAST&itemId=1074193&viewName=.executors
```
for item to bottom, or
```
http://jenkins_url/simpleMove/move?moveType=DOWN&itemId=1074184&viewName=.executors
```
for item one step forward, or
```
http://jenkins_url/simpleMove/move?moveType=BOTTOM&itemId=1073889&viewName=.executors
```
for move to bottom of view

The `viewName` is optional and is obvious. The `moveType` too (its full enumeration is in https://github.com/jenkinsci/simple-queue-plugin/blob/master/src/main/java/cz/mendelu/xotradov/MoveType.java .  The `itemId` is super sure for jenkins to jenkins communication, but useless for human usage. Thus the https://github.com/jenkinsci/simple-queue-plugin/pull/2 added feature to move by name, so `itemId` can be also job name. If no job is found, the plugin will simply fall throug, so to speed up job **my-job-name** (in view my_view) you end up on:
```
curl "http://jenkins_url/simpleMove/move?moveType=DOWN_FAST&itemId=my-job-name"
```
for item to bottom, or
```
curl "http://jenkins_url/simpleMove/move?moveType=DOWN&itemId=my-job-name"
```
for item one step forward, or
```
curl "http://jenkins_url/simpleMove/move?moveType=BOTTOM&itemId=my-job-name&viewName=my_view"
```
for move to bottom of view


![Screenshot](images/queue_screenshot.png "Simple Queue screenshot")
# Other useful plugins
If this plugin does not fit your needs, try using some of the plugins below that use more automatic approach:\
https://plugins.jenkins.io/PrioritySorter/ \
https://plugins.jenkins.io/dependency-queue-plugin/ \
https://plugins.jenkins.io/multi-branch-priority-sorter/ 
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
