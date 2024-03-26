* [simple-queue-plugin](#simple-queue-plugin)
* [usage](#usage)
* [cli](#cli)
* [cli-human-api](#cli-human-api)
  * [overview](#overview)
  * [examples](#examples)
    * [global movement](#global-movement)
      * DOWN/DOWN_FAST
      * UP/UP_FAST
    * [in-view movement](#in-view-movement)
      * BOTTOM
      * TOP
    * [Complex names](#complex-names)
      * escaping
      * full names
    * [HTTP return value](#http-return-value)
* [screenshot](#screenshot)
* [other-useful-plugins](#other-useful-plugins)
* [question--issues](#question--issues)
* [for-developers](#for-developers)
* [esting-build](#testing-build)
* [performing-release](#performing-release)
# simple-queue-plugin
[![Jenkins Version](https://img.shields.io/badge/Jenkins-2.414.1-green.svg?label=min.%20Jenkins)](https://jenkins.io/download/)
[![Build Status](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/simple-queue-plugin/job/master/) \
Plugin for Jenkins enabling changing a build queue order from UI manually.
# Usage
Usage video: https://youtu.be/anyGsJIa020 \
There are two types of moves: one up/down or fast way to top/bottom. The third move type is added in filtered view to distinguish between top of filtered items and top of all items.
The user must have an Administer/Overall or MANAGE/Overall permission for changing the queue order. (since plugin version 1.3.5)\
    For using Manage permission is needed plugin: https://plugins.jenkins.io/manage-permission/
Orders buildable items only, for that reason [blocked](hhttps://stackoverflow.com/questions/56182285/difference-between-blocked-stuck-pending-buildable-jobs-in-jenkins) items do NOT have an arrow.<br />

#### Screenshot
![Screenshot](images/queue_screenshot.png "Simple Queue screenshot")

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
for move to bottom of view. However, get "id" is imposible for human use, so the `itemID` can accept the job name:
## CLI-human api
 * [overview](#overview)
 * [examples](#examples)
   * [global movement](#global-movement)
     * DOWN/DOWN_FAST
     * UP/UP_FAST
   * [in-view movement](#in-view-movement)
     * BOTTOM
     * TOP
   * [Complex names](#complex-names)
     * escaping
     * full names
   * [HTTP return value](#http-return-value)
### overview
The `viewName` is optional and is obvious. The `moveType` too (its [full enumeration](https://github.com/jenkinsci/simple-queue-plugin/blob/master/src/main/java/cz/mendelu/xotradov/MoveType.java)) .  The `itemId` is super sure for jenkins to jenkins communication, but useless for human usage. Thus the https://github.com/jenkinsci/simple-queue-plugin/pull/2 added feature to move by name, so `itemId` can be *also job name*. If no job is found, the plugin will simply fall throug, so to speed up job **my-job-name** you end up on:
### examples
#### global movement
`viewName` have no efect, it is only for in-view movement (see later). If you use some special custom default view, you may need to add it. If so, enhance below **four** DOWN/UP examples by `viewName=my_weird_default_view`.
##### DOWN/DOWN_FAST
```
curl "http://jenkins_url/simpleMove/move?moveType=DOWN_FAST&itemId=my-job-name"
```
for item to move to bottom - to run before all others now
```
curl "http://jenkins_url/simpleMove/move?moveType=DOWN&itemId=my-job-name"
```
for item one step forward - to run before the job, it was supposed to run before this one originally

To slow down job **my-job-name** (in view my_view) you end up on:
##### UP/UP_FAST
```
curl "http://jenkins_url/simpleMove/move?moveType=UP_FAST&itemId=my-job-name"
```
for item to move to top - to run last of all others now
```
curl "http://jenkins_url/simpleMove/move?moveType=UP&itemId=my-job-name"
```
for item one step up - to run later then the job, which was supposed to run righ after this one originally
#### in-view movement
in which `viewName=my_view` is **mandatory** righ after `viewName`. the UP/UP_FAST/DOWN/DOWN_FAST still behave in global space, and as expected. To jump to the top/bottom of view, there are two additional commands
##### BOTTOM
```
curl "http://jenkins_url/simpleMove/move?moveType=BOTTOM&itemId=my-job-name&viewName=my_view"
```
for move to bottom of view - the item run before all others in this view
##### TOP
```
curl "http://jenkins_url/simpleMove/move?moveType=TOP&itemId=my-job-name&viewName=my_view"
```
for move to top of view - the item run last of all others in this view
#### Complex names
As investiaget at https://github.com/jenkinsci/simple-queue-plugin/pull/3#discussion_r1306649177 ,  there are two cornercases
 * **escaping**: if your name contains **%** or **/** they have to be URL escaped. So / will become **%2F** and % will becom **%25**
   * generally spoken, your **full name** (see lower) should be **fully escaped**
 * **full names**: somem plugins - eg   Pipeline: Nodes and Processes plugin **or** git branches plugin  - uses lets say *fully qualified names*. Such name must contain its full dispaly name. eg
   * `SAUR/Rex/release%2F1.5` must be passed in a display name like
   * `part of SAUR » Rex » release/1.5 #413` should pe passed in as
   * `itemId=part%20of%20SAUR%20%C2%BB%20Rex%20%C2%BB%20release%2F1.5%20%23413`

This is annoying, and PR to improve this is welcomed. However such cross plugin playing is requiring some class-name/reflection playing.

#### HTTP return value
Unluckily, currently plugin always returns `302 Found` so you will not know if call suceeded. This may chanege, but is not planned

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
