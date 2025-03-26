# CLI
When hovering over priority arrows, you can see that it executes special url aka:
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
for move to bottom of view. However, get "id" is impossible for human use, so the `itemID` can accept the job name:
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
curl -XPOST --user username:apitoken "http://jenkins_url/simpleMove/move?moveType=DOWN_FAST&itemId=my-job-name"
```
for item to move to bottom - to run before all others now
```
curl -XPOST --user username:apitoken "http://jenkins_url/simpleMove/move?moveType=DOWN&itemId=my-job-name"
```
for item one step forward - to run before the job, it was supposed to run before this one originally

To slow down job **my-job-name** (in view my_view) you end up on:
##### UP/UP_FAST
```
curl -XPOST --user username:apitoken "http://jenkins_url/simpleMove/move?moveType=UP_FAST&itemId=my-job-name"
```
for item to move to top - to run last of all others now
```
curl -XPOST --user username:apitoken "http://jenkins_url/simpleMove/move?moveType=UP&itemId=my-job-name"
```
for item one step up - to run later than the job, which was supposed to run right after this one originally
#### in-view movement
in which `viewName=my_view` is **mandatory** right after `viewName`. the UP/UP_FAST/DOWN/DOWN_FAST still behave in global space, and as expected. To jump to the top/bottom of view, there are two additional commands
##### BOTTOM
```
curl -XPOST --user username:apitoken "http://jenkins_url/simpleMove/move?moveType=BOTTOM&itemId=my-job-name&viewName=my_view"
```
for move to bottom of view - the item run before all others in this view
##### TOP
```
curl -XPOST --user username:apitoken "http://jenkins_url/simpleMove/move?moveType=TOP&itemId=my-job-name&viewName=my_view"
```
for move to top of view - the item run last of all others in this view

### Legacy Api
The old, unsecure GET approach can still be used, if enabled in main settings:

When hovering over priority arrows, you cans see that it executes special url aka:
```
curl "http://jenkins_url/simpleMoveUnsafe/move?moveType=DOWN_FAST&itemId=1074193&viewName=.executors"
```
for item to bottom, or
```
curl "http://jenkins_url/simpleMoveUnsafe/move?moveType=DOWN&itemId=1074184&viewName=.executors"
```
for item one step forward, or
```
curl "http://jenkins_url/simpleMoveUnsafe/move?moveType=BOTTOM&itemId=1073889&viewName=.executors"
```
for move to bottom of view

The `viewName` is optional and is obvious. The `moveType` too (its full enumeration is in https://github.com/jenkinsci/simple-queue-plugin/blob/master/src/main/java/cz/mendelu/xotradov/MoveType.java .  The `itemId` is super sure for jenkins to jenkins communication, but useless for human usage. Thus the https://github.com/jenkinsci/simple-queue-plugin/pull/2 added feature to move by name, so `itemId` can be also job name. If no job is found, the plugin will simply fall throug, so to speed up job **my-job-name** (in view my_view) you end up on:
```
curl "http://jenkins_url/simpleMoveUnsafe/move?moveType=DOWN_FAST&itemId=my-job-name"
```
for item to bottom, or
```
curl "http://jenkins_url/simpleMoveUnsafe/move?moveType=DOWN&itemId=my-job-name"
```
for item one step forward, or
```
curl "http://jenkins_url/simpleMoveUnsafe/move?moveType=BOTTOM&itemId=my-job-name&viewName=my_view"
```
for move to bottom of view

even the reset
```
http://jenkins_url/simpleQueueResetUnsafe/reset
```
have working unsafe variant (if enabled)

#### Complex names
As investigated at https://github.com/jenkinsci/simple-queue-plugin/pull/3#discussion_r1306649177 ,  there are two cornercases
 * **escaping**: if your name contains **%** or **/** they have to be URL escaped. So / will become **%2F** and % will become **%25**
   * generally spoken, your **full name** (see lower) should be **fully escaped**
 * **full names**: somem plugins - e.g.   Pipeline: Nodes and Processes plugin **or** git branches plugin  - uses lets say *fully qualified names*. Such name must contain its full dispaly name. eg
   * `SAUR/Rex/release%2F1.5` must be passed in a display name like
   * `part of SAUR » Rex » release/1.5 #413` should pe passed in as
   * `itemId=part%20of%20SAUR%20%C2%BB%20Rex%20%C2%BB%20release%2F1.5%20%23413`

This is annoying, and PR to improve this is welcomed. However, such cross plugin playing is requiring some class-name/reflection playing.

#### HTTP return value
Unluckily, currently plugin always returns `302 Found` so you will not know if call succeeded. This may change, but is not planned

