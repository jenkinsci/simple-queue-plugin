# For plugin developers
This section is intended for people who want to further extend this plugin.

## Testing build
mvn hpi:run
## Performing release
Always test connection before release.
Testing connection: ssh -T git@github.com

Release: mvn release:prepare release:perform

## Documentation
The documentation framework used is mkdocs.
To see the documentation before publishing use 'pip install mkdocs-material' followed by 'mkdocs serve'

## How it works inside
![Sequence diagram](images/basicUsageSequence.png "Simple Queue screenshot")
![Sequence diagram](images/moveUpSequence.png "Simple Queue screenshot")
![Sequence diagram](images/resetSequence.png "Simple Queue screenshot")