# anecdot
A file based CMS on top of Java

# Installation

It's recommendet to install anecdot either in ```/opt/anecdot``` or in ```/home/anecdot```.

Follow the instructions on the official [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html) 
to install the application as a service.

## Configuration

The ```anecdot.hosts``` property contains a list of keys used to configure your hosts.

### Example
```
anecdot.hosts=foo,bar

anecdot.host.foo.names=foo.tld,www.foo.tld
anecdot.host.foo.directory=./foo
anecdot.host.foo.home=/home

anecdot.host.bar.names=bar.tld,www.bar.tld
anecdot.host.bar.directory=./bar
anecdot.host.bar.home=/start
```