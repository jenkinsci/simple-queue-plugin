# CLI
**Since 1.4.7**, breaking *changes* were introduced into *human* usable **api**:
https://github.com/jenkinsci/simple-queue-plugin/releases/tag/simple-queue-1.4.7

When hovering over priority arrows, you can see that it executes a special URL,
for example:
```
http://${JENKINS_URL}/simpleMove/move?moveType=DOWN_FAST&itemId=1074193&viewName=.executors
```
for moving an item to the bottom of the whole queue, or
```
http://${JENKINS_URL}/simpleMove/move?moveType=DOWN&itemId=1074184&viewName=.executors
```
for moving an item one step forward, or
```
http://${JENKINS_URL}/simpleMove/move?moveType=BOTTOM&itemId=1073889&viewName=.executors
```
for moving an item to the bottom of a specified view.

However, getting that "id" of a queue item is nearly impossible for human use,
so the `itemID` parameter can accept the job name (or job part name, if stages
require different executors), or a regular expression to select several queue
items at once (such as different parts of the same running job).

NOTE: Wherever this documentation mentions a "job name", actually the plugin currently
deals with "Queued Task Display Name" strings (not the internal, possibly stricter,
"job name" that any Jenkins model item has), which are easier to find in the UI but
may include complex characters which would need to be URL-escaped, as explained in
the [Complex names](#complex-names) section below.

## CLI-human api
 * [overview](#overview)
 * [examples](#examples)
   * [global movement](#global-movement)
     * DOWN/DOWN_FAST
     * UP/UP_FAST
   * [in-view movement](#in-view-movement)
     * BOTTOM
     * TOP
   * [Legacy API](#legacy-api)
   * [Regular expressions](#regular-expressions)
     * Groovy/PERL style expressions
     * Java style expressions
 * [Caveats](#caveats)
   * [Complex names](#complex-names)
     * escaping
     * full names
   * [HTTP return value](#http-return-value)
 * [strict api](#strict-api)

### overview
The `viewName` is optional and is obvious.

The `moveType` too (see its [full
enumeration](https://github.com/jenkinsci/simple-queue-plugin/blob/master/src/main/java/cz/mendelu/xotradov/MoveType.java)).

The `itemId` is super certain for Jenkins-to-Jenkins communication (numeric ID of the `Queue.Item` involved),
but useless for human usage. Thus, the https://github.com/jenkinsci/simple-queue-plugin/pull/2 added a feature
to move an item by name, so `itemId` can be *also job name*.

Subsequently, a feature was added in https://github.com/jenkinsci/simple-queue-plugin/pull/17 to select a number
of queue items by a regular expression, to move them in bulk (e.g. to change the priority of many stages of some
job which are queued to different worker nodes, or of several scheduled builds of the same pull request or branch
iterations). This mode is also attached to UI buttons since https://github.com/jenkinsci/simple-queue-plugin/pull/18
(handling the URL-escaping complications as needed), and can also be called in CLI mode (e.g. in a browser,
you can Copy Link of a button and edit the URL).

The `itemId` processing logic is as follows:

* if the `itemId` is a number, it is interpreted as a queue item number,
* otherwise the `itemId` string is interpreted as an exact name of a queue item (possibly needs URL-escaping in
  the HTTP query).
* If no ID is found (or the input is not an number), an exact match is attempted. If again nothing is found, the `itemId` is interpreted as a Java style regular expression. A Groovy/PERL style regular expression is also possible, if it follows tilde-slash markup like `~/.../`. Which literally just preffix+suffix the expression by `.*`. This mode is discussed in more detail in the [Regular expressions](#regular-expressions) section below.

If no job is found, the plugin will simply fall through without sorting the queue.

The default supported HTTP method is `POST` (but for [Legacy API](#legacy-api) mode you can specifically enable `GET`).

### examples
#### global movement
The `viewName` option has no effect, it is only for in-view movement (see later).
If you use some special custom default view, you may need to add it.
In this case, enhance below **four** DOWN/UP examples by `&viewName=my_weird_default_view`.

##### DOWN/DOWN_FAST
Used to speed up completion of the job called **my-job-name**:
```
curl -XPOST --user username:apitoken "http://${JENKINS_URL}/simpleMove/move?moveType=DOWN_FAST&itemId=my-job-name"
```
for moving an item to the bottom of the queue, meaning it would run before all others now.
```
curl -XPOST --user username:apitoken "http://${JENKINS_URL}/simpleMove/move?moveType=DOWN&itemId=my-job-name"
```
for moving an item one step forward: to run before the job, which was supposed to run before this one originally.

##### UP/UP_FAST
Used to slow down completion of the job called **my-job-name**:
```
curl -XPOST --user username:apitoken "http://${JENKINS_URL}/simpleMove/move?moveType=UP_FAST&itemId=my-job-name"
```
for moving an item to the top of the queue, meaning it would run last, after all other items known now.
```
curl -XPOST --user username:apitoken "http://${JENKINS_URL}/simpleMove/move?moveType=UP&itemId=my-job-name"
```
for moving an item one step up: to run later than the job, which was supposed to run right after this one originally.

#### in-view movement
In this mode a `&viewName=my_view` is **mandatory** right after `viewName`.

The `moveType` UP/UP_FAST/DOWN/DOWN_FAST values still behave in the global space, and as expected there.
To jump to the top/bottom of the specified view, there are two additional commands (`moveType` values) detailed below.

##### BOTTOM
```
curl -XPOST --user username:apitoken "http://${JENKINS_URL}/simpleMove/move?moveType=BOTTOM&itemId=my-job-name&viewName=my_view"
```
for moving an item to the bottom of view: the item would run before all others in this view
##### TOP
```
curl -XPOST --user username:apitoken "http://${JENKINS_URL}/simpleMove/move?moveType=TOP&itemId=my-job-name&viewName=my_view"
```
for moving an item to the top of view: the item would run as the last one of all others in this view

#### Legacy API
The old, unsecure HTTP `GET` method approach can still be used, but only if enabled in the main settings, e.g.:

```
curl "http://${JENKINS_URL}/simpleMoveUnsafe/move?moveType=DOWN_FAST&itemId=my-job-name&viewName=.executors"
```
for moving an item to the bottom of the queue, or
```
curl "http://${JENKINS_URL}/simpleMoveUnsafe/move?moveType=DOWN&itemId=my-job-name&viewName=.executors"
```
for moving an item one step forward, or
```
curl "http://${JENKINS_URL}/simpleMoveUnsafe/move?moveType=BOTTOM&itemId=my-job-name&viewName=.executors"
```
for moving an item to the bottom of the view.

Query parameters for `GET` queries are the same as in the `POST` examples above.

Even the queue reset action has a working unsafe variant (if enabled):
```
http://${JENKINS_URL}/simpleQueueResetUnsafe/reset
```

#### Regular expressions
The `itemId` parameter can also accept a regular expression to select several queue items at once (such as different
parts of the same running job).

Two regular expression markup modes are currently supported, as detailed below. Keep in mind that either way the
expression is handled by java `Pattern` and `Matcher` classes, so you can use any of the regular expression syntax
supported by those classes.

Both types of regular expressions can append `/switches`. Eg `/i` will switch to case insensitive. Next to that, there are also dDn,
which determines where to search d - display name - default; D - full display name and n - name.
So `.*job/idnD` will search case-insensitive for any "job" suffixes display name, full display name and in name.

NOTE: The examples below are illustrative and may require URL-escaping in an actual HTTP query.

##### Groovy/PERL style expressions

Regular expressions to match the queue item(s) to move should be encased in tilde-slash markup like `~/.../`,
with encased characters passed to Java relaxed `Matcher::find` method; this mode is deemed simpler for everyday
use with short queries for queue shuffling, e.g.:
```
curl -XPOST --user username:apitoken \
    "http://${JENKINS_URL}/simpleMove/move?moveType=DOWN_FAST&itemId=~/1234/"
```
to move any item with `1234` in its name (which would likely reflect a job/branch re-build number, or a PR number).

Case-insensitive regular expressions match can be used by adding `i` in the end, like `~/.../i`, e.g.:
```
curl -XPOST --user username:apitoken \
    "http://${JENKINS_URL}/simpleMove/move?moveType=DOWN_FAST&itemId=~/^[^X]*My-jOb-naMe.*#1234.*$/i"
```

##### Java style expressions
If the regular expressions mode is enabled, and the `itemId` was not a tilde-slash markup, the whole string
is interpreted as a complete Java style regular expression (passed to Java strict `Matcher::matches` method);
so to match sub-strings with possible preceding or following characters, you would need to explicitly add `.*`
on the corresponding side(s), e.g.:
```
curl -XPOST --user username:apitoken \
    "http://${JENKINS_URL}/simpleMove/move?moveType=DOWN_FAST&itemId=.*(?i)My-jOb-naMe(?-i).*#1234.*"
```

### Caveats

#### Complex names
As investigated at https://github.com/jenkinsci/simple-queue-plugin/pull/3#discussion_r1306649177 PR discussion,
there are two corner cases:

 * **escaping**: if your name contains `%` or `/` characters, they have to be URL-escaped.
   So a `/` will become `%2F` and a `%` will become `%25` and a `#` will become `%23`.
   * generally spoken, your **full name** (see below) should be **fully escaped**
   * it may be a bigger adventure to pass a regular expression through URL escape markup, but it should be possible
 * **full names**: some plugins (e.g. the "Pipeline: Nodes and Processes" plugin **or** "git branches" plugin) use,
   let's say, *fully qualified names*. Such a name must contain its full display name, e.g.:
   * `SAUR/Rex/release%2F1.5` must be passed in a display name like
   * `part of SAUR » Rex » release/1.5 #413` should pe passed in as
   * `itemId=part%20of%20SAUR%20%C2%BB%20Rex%20%C2%BB%20release%2F1.5%20%23413`

This is annoying, and PR to improve this is welcomed. However, such a cross-plugin play may require some
class-name/reflection work, or coordination with maintainers of those plugins.

Also note that since https://github.com/jenkinsci/simple-queue-plugin/pull/18 there is a "Build Queue Bulk Move"
UI pane available to Jenkins instance administrators, where they can enter the regular expression directly and
move any matching item(s) to the top or bottom of the queue. This also handles any required URL escaping.

#### HTTP return value
The plugin returns status code `200` if the move was successful. Status code `404` is returned when the queue
item was not found and `400` when the move type is invalid.
The error responses contain a json body with more details, e.g.:
```
{
  "message": "Queue item '1234' not found"
}
```
Many responses contains also reason of finding nothing.

## strict api
Because of the waterfall: number->match->regex(es), in addition with switches, and concerns about `display` to match against, an new strict api was introduced:

* `moveType` remains mandatory parameter to determine movement
* `viewName` remains optional parameter to specify view
* use `itemIdExt` instead of `itemId`. Those two are mutually exclusive. `itemId` value is resolved as old versatile api, but `itemIdExt` is resolving via new apu
* `itemMode` is mandatory argument for new api. Must be set exactly onece, by one of following (case insensitive) values:
  * `QID` - item id exact item selection for internal jenkins operations
  * `EXACT` - to match by exact match
  * `REGEX` - as java-like regex, withou `/args`
  * `GREGEX` - as groovy like regex withou `~//args` (so basically regex with prepend/append of `.*`.)
* `itemTarget` to specify where/how match against. Can be set multipletimes, but at least once. Each value can be one of the (case insensitive) following:
  * `DISPLAY` - search in display name
  * `FULLDISPLAY` - search in full display name
  * `NAME` - search in item name
  * `I` - search is case insensitive
  * Unlike original old `itemId` mode, item target is used also in `exact` match mode, and not only in regex mode.

Strict api is currently not used, is considered as experimental, but  shoudl resolve any issue the versatile api may failedue to its number->match->regex(es) waterfall nature.

## Print Queue API

**Since 1.4.13**, a new API endpoint has been added to retrieve the current queue state as a plaintext list.

### Overview

The Print Queue API allows you to retrieve the current build queue as a simple plaintext list, with one job display name per line. This is useful for monitoring, scripting, or integration with external tools.

### Endpoints

#### Safe API (with CSRF protection)
```
POST http://${JENKINS_URL}/simpleMove/printQueue
```
- Requires POST method with CSRF token (crumb)
- Requires `SIMPLE_QUEUE_MOVE_PERMISSION`

#### Unsafe API (without CSRF protection)
```
GET/POST http://${JENKINS_URL}/simpleMoveUnsafe/printQueue
```
- Works with both GET and POST methods
- No CSRF token required
- Must be explicitly enabled in plugin configuration (`enableUnsafe`)

### Parameters

- **`buildable`** (optional): Controls which items to include in the output
  - `true` or omitted (default): Shows only buildable items
  - `false`: Shows all items in the queue (including blocked/waiting items)
- **`viewName`** (optional): Filter queue items by view
  - If specified, only shows items visible in that view
  - If omitted, shows all queue items

### Output Format

The API returns a plaintext response with one job display name per line:
```
Project A
Project B
Project C
```

### Queue Order

The API returns the **sorted queue** order, which reflects:
- The current state after any manual reordering done through the Simple Queue plugin
- The actual execution order that Jenkins will follow
- All move operations (UP, DOWN, TOP, BOTTOM) that have been applied

### Examples

#### Get buildable items (default)
```bash
# Safe API (requires CSRF token)
curl -X POST --user username:apitoken \
  "http://${JENKINS_URL}/simpleMove/printQueue"

# Unsafe API (no CSRF token needed, must be enabled)
curl "http://${JENKINS_URL}/simpleMoveUnsafe/printQueue"
```

#### Get all items (including non-buildable)
```bash
# Safe API
curl -X POST --user username:apitoken \
  "http://${JENKINS_URL}/simpleMove/printQueue?buildable=false"

# Unsafe API
curl "http://${JENKINS_URL}/simpleMoveUnsafe/printQueue"
```

#### Get queue for a specific view
```bash
# Safe API
curl -X POST --user username:apitoken \
  "http://${JENKINS_URL}/simpleMove/printQueue?viewName=my_view"

# Unsafe API
curl "http://${JENKINS_URL}/simpleMoveUnsafe/printQueue?viewName=my_view"
```

#### Using in scripts
```bash
#!/bin/bash
# Get the queue and process each job
curl -s --user username:apitoken \
  "http://${JENKINS_URL}/simpleMove/printQueue" | \
while IFS= read -r job_name; do
  echo "Processing: $job_name"
  # Your processing logic here
done
```

```bash
curl "http://${JENKINS_URL}/simpleMoveUnsafe/printQueue" |  wc -l

```

### Security Considerations

- The **safe API** (`/simpleMove/printQueue`) requires proper authentication and CSRF protection, making it suitable for web-based integrations
- The **unsafe API** (`/simpleMoveUnsafe/printQueue`) bypasses CSRF protection for easier CLI/automation use, but must be explicitly enabled in the plugin configuration
- Both APIs require appropriate Jenkins permissions
