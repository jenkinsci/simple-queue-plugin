# For developers

## Testing build
mvn hpi:run
## Performing release
Always test connection before release. \
Testing connection: ssh -T git@github.com \
Release: mvn release:prepare release:perform