# SonarQube DaDaPush Notification Plugin

SonarQube Sends notifications via DaDaPush.


# Install
The plugin must be placed in *SONAR_HOME/extensions/plugins* directory and SonarQube must be restarted.


## Using latest release
You can find the latest release from [sonar-dadapush-notification-plugin](https://github.com/dadapush/sonar-dadapush-notification-plugin/releases/) release page.


## From sources
To build the plugin simply run
```
mvn clean package
```


# Configuration
After the plugin has been installed, you need to configure it.
```
http://YOUR_SONAR_SERVER/admin/settings?category=dadapush
```

## restart Sonar Server
```
http://YOUR_SONAR_SERVER/admin/system
```

## Wildcard support
The project key supports wildcards at the end.
look like this:
```
com.dadapush.client:sonar-dadapush-plugin:*
com.dadapush.client:*
```

## Only send notification when Quality Gate fails
Notifications can be sent for all Quality Gate statuses, or just for WARNING/ERROR statuses.

## test notification
```
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=40eeed61410561391c9eb4777a8ae59d90b8d19d
```
