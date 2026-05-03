# Spring WebDAV

RFC 4918 compliant WebDAV server framework built on Spring WebMVC.

## Features

- RFC 4918 method support: `OPTIONS`, `GET`, `PUT`, `DELETE`, `MKCOL`, `PROPFIND`, `PROPPATCH`, `COPY`, `MOVE`, `LOCK`, `UNLOCK`
- Spring Boot auto-configuration
- Pluggable `WebDavResourceStore` SPI for custom storage backends
- Built-in backends: local filesystem (NIO), Amazon S3, PostgreSQL
- Pluggable lock manager (`WebDavLockManager`) and property store (`WebDavPropertyStore`)
- Requires a servlet-based Spring Boot application

## Requirements

| Dependency | Version |
|---|---|
| Java | 21+ |
| Spring Boot | 4.0+ |

## Installation

Artifacts are available via [JitPack](https://jitpack.io/#larsw/spring-webdav), which requires no authentication and works with any build tool.

### Maven

Add the JitPack repository and the starter dependency:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>io.github.larsw.webdav</groupId>
    <artifactId>spring-webdav-starter</artifactId>
    <version>v1.0.1</version>
</dependency>
```

### Gradle

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("io.github.larsw.webdav:spring-webdav-starter:v1.0.1")
}
```

Use any published git tag as the version (e.g. `v1.0.1`). A full list is available on the [releases page](https://github.com/larsw/spring-webdav/releases).

## Quick Start

### 1. Add the starter

See [Installation](#installation) above for Maven and Gradle coordinates.

The starter pulls in `spring-webdav-autoconfigure` and `spring-boot-starter-webmvc`.

### 2. Choose a backend

#### Local filesystem

Add the filesystem module and set the root directory:

```xml
<dependency>
    <groupId>io.github.larsw.webdav</groupId>
    <artifactId>spring-webdav-filesystem</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
spring:
  webdav:
    prefix: /dav
    filesystem:
      root: /var/data/webdav
```

#### Amazon S3

Add the S3 module and configure your bucket:

```xml
<dependency>
    <groupId>io.github.larsw.webdav</groupId>
    <artifactId>spring-webdav-s3</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
spring:
  webdav:
    prefix: /dav
    s3:
      bucket: my-webdav-bucket
      region: eu-west-1   # optional, defaults to SDK auto-detection
```

AWS credentials are resolved via the standard [DefaultCredentialsProvider](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html) chain (environment variables, instance profile, `~/.aws/credentials`).

#### PostgreSQL

Add the PostgreSQL module and map one or more database tables to virtual directory trees:

```xml
<dependency>
    <groupId>io.github.larsw.webdav</groupId>
    <artifactId>spring-webdav-postgresql</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
spring:
  webdav:
    prefix: /dav
    postgresql:
      enabled: true
      mappings:
        - name: reports
          table: monthly_reports
          path-column: region        # e.g. "europe/2024"
          name-column: report_name   # e.g. "revenue" => /dav/reports/europe/2024/revenue.csv
          format: CSV

        - name: configs
          table: app_settings
          name-column: setting_key
          format: JSON
          json-column: setting_value  # a jsonb column
```

### 3. Run

```bash
./mvnw spring-boot:run
```

The WebDAV endpoint is available at `http://localhost:8080/dav`.

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `spring.webdav.prefix` | `""` | URL prefix for all WebDAV endpoints. Use `""` to serve from root. |
| `spring.webdav.filesystem.root` | `~/webdav` | Root directory for the filesystem backend. Setting this property activates the backend. |
| `spring.webdav.s3.bucket` | (none) | S3 bucket to serve. Setting this property activates the S3 backend. |
| `spring.webdav.s3.region` | SDK default | AWS region (e.g. `us-east-1`). |
| `spring.webdav.postgresql.enabled` | `false` | Set to `true` to activate the PostgreSQL backend. |
| `spring.webdav.postgresql.mappings` | `[]` | List of table mappings (see [PostgreSQL backend](#postgresql) above). |

## Custom Storage Backend

Implement `WebDavResourceStore` and register it as a Spring bean. The auto-configuration backs off when it detects your bean.

```java
@Component
public class MyResourceStore implements WebDavResourceStore {

    @Override
    public Optional<WebDavResource> getResource(String path) { // ... }

    @Override
    public List<WebDavResource> listChildren(String path) { // ... }

    @Override
    public void createCollection(String path) throws WebDavException { // ... }

    @Override
    public void delete(String path) throws WebDavException { // ... }

    @Override
    public void copy(String src, String dst, boolean overwrite, boolean recursive) throws WebDavException { // ... }

    @Override
    public void move(String src, String dst, boolean overwrite) throws WebDavException { // ... }

    @Override
    public InputStream getContent(String path) throws WebDavException { // ... }

    @Override
    public void putContent(String path, InputStream content, String contentType, long contentLength) throws WebDavException { // ... }
}
```

Throw `WebDavException(statusCode, message)` from any method to return the appropriate HTTP status to the client.

## Advanced Customization

Override individual beans to customize behavior without replacing the entire stack:

| Bean type | Default | Purpose |
|---|---|---|
| `WebDavResourceStore` | (none, required) | Storage backend |
| `WebDavLockManager` | `InMemoryLockManager` | `LOCK`/`UNLOCK` handling |
| `WebDavPropertyStore` | `DefaultWebDavPropertyStore` | `PROPFIND`/`PROPPATCH` handling |
| `WebDavConfigurer` | reads `spring.webdav.prefix` | URL prefix and wiring hooks |

### Programmatic prefix configuration

```java
@Bean
public WebDavConfigurer webDavConfigurer() {
    return new WebDavConfigurer() {
        @Override
        public String getDavPrefix() { return "/dav"; }
    };
}
```

### Non-Boot Spring applications

Use `@EnableWebDav` on a `@Configuration` class instead of relying on auto-configuration:

```java
@EnableWebDav
@Configuration
public class AppConfig { }
```

## Module Overview

| Module | Description |
|---|---|
| `spring-webdav-core` | Framework core: SPI interfaces, HTTP method handlers, handler mapping |
| `spring-webdav-autoconfigure` | Spring Boot auto-configuration |
| `spring-webdav-starter` | Convenience starter (autoconfigure + WebMVC) |
| `spring-webdav-filesystem` | NIO local-filesystem backend |
| `spring-webdav-s3` | Amazon S3 backend (AWS SDK v2) |
| `spring-webdav-postgresql` | PostgreSQL backend (JDBC / Spring `JdbcTemplate`) |
| `spring-webdav-sample` | Sample Spring Boot application |

## Building

```bash
./mvnw clean install
```

To run the sample application:

```bash
./mvnw -pl spring-webdav-sample spring-boot:run
```

The sample serves WebDAV at `http://localhost:8080/dav`, rooted at `~/webdav-root`.

## License

See [LICENSE](LICENSE) for details.
