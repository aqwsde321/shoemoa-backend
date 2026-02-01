# GEMINI.md - Shoemoa Backend

## Project Overview

This project is the back-end for Shoemoa, a shoe e-commerce platform. It is a Spring Boot application written in Java 17. The project follows a layered architecture, separating concerns into presentation (REST API), application (business logic), domain (entities), and infrastructure (database, S3).

**참고: 이 에이전트의 모든 답변은 한국어로 제공됩니다.**

**Key Technologies:**

*   **Framework:** Spring Boot 3.5.9
*   **Language:** Java 17
*   **Database:** JPA with Hibernate, QueryDSL, H2 (for development), and PostgreSQL (for production).
*   **API:** RESTful API with Swagger documentation.
*   **File Storage:** AWS S3 for product image storage, with CloudFront for content delivery.
*   **Build Tool:** Gradle
*   **Code Style:** Spotless with Palantir Java Format.

## Building and Running

### Prerequisites

*   Java 17
*   (Optional) Docker

### Building the Project

To build the project, run the following command:

```bash
./gradlew build
```

### Running the Application

There are several ways to run the application:

**1. Using Gradle (local profile):**

This method uses the `local` Spring profile, which is configured to use an in-memory H2 database.

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`.

**2. Using Docker:**

The project includes a `Dockerfile` and helper scripts for building and running the application in a Docker container.

```bash
# Build the Docker image
./docker/docker-build.sh

# Run the Docker container
./docker/docker-run.sh
```

**API Documentation (Swagger):**

Once the application is running, you can access the Swagger UI for API documentation at:

`http://localhost:8080/swagger-ui/index.html`

### Running Tests

To run the tests, use the following command:

```bash
./gradlew test
```

## Development Conventions

*   **Code Formatting:** The project uses the Spotless plugin with the Palantir Java Format to maintain a consistent code style. Before committing any changes, it's recommended to run `./gradlew spotlessApply` to format the code.
*   **Branching Strategy:** (Not specified in the provided files, but a common practice would be to use a feature-branching workflow).
*   **Testing:** The project includes unit and integration tests. New features should be accompanied by corresponding tests.
*   **Commits:** (Not specified, but it's good practice to write clear and concise commit messages).
