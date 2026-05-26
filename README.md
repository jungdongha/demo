# demo-dong

`demo-dong` is a Spring Boot 3.4.0 application designed to provide an AI chat interface, specifically integrated with **Groq** using the **Spring AI** framework (OpenAI-compatible). It serves as a boilerplate or demonstration project for building AI-powered applications with modern Java and Spring technologies.

## Core Technologies
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.0
- **AI Integration**: Spring AI (OpenAI Starter configured for Groq)
- **Database**: H2 (In-memory) with Spring Data JPA
- **Build System**: Gradle
- **Other**: Lombok for boilerplate reduction

## Architecture & Directory Structure
The project follows a DDD-influenced layered architecture:

- `src/main/java/com/obigo/demodong/domain`: Contains business logic organized by domain.
    - `ai/presentation`: Controllers and response codes (`AiController`, `AiResponseCode`).
    - `ai/application`: Use cases/Service logic (`AiUseCase`).
- `src/main/java/com/obigo/demodong/global/common`: Shared infrastructure and common patterns.
    - `exception`: Global exception handling (`GlobalExceptionHandler`, `ApplicationException`).
    - `response`: Unified API response wrapper (`ApiResponse`).
    - `infrastructure/ai`: AI-specific configuration and properties (`AiConfig`, `AiProperties`).
- `src/main/resources`: Configuration files.
    - `application.yaml`: Server port, DB settings, and Groq API configuration.

## Key Development Conventions

### 1. Unified API Responses
All API endpoints should return the `ApiResponse` record to ensure consistency.
- Success: `ApiResponse.ok(ResponseCode, data)`
- Failure: `ApiResponse.fail(ErrorCode)`

### 2. Exception Handling
Custom business exceptions should extend `ApplicationException`. These are caught by the `GlobalExceptionHandler` to return a standardized error response.

### 3. AI Client Configuration
The project uses `ChatClient` from Spring AI. A specific bean `groqChatClient` is configured in `AiConfig` to connect to Groq's OpenAI-compatible API.

## Building and Running

### Prerequisites
- JDK 21
- `GROQ_API_KEY` environment variable must be set for AI features to work.

### Commands
- **Build**: `./gradlew build`
- **Run**: `./gradlew bootRun` (Server starts on port 8090 by default)
- **Test**: `./gradlew test`
- **H2 Console**: Accessible at `/h2-console` when the application is running.

## TODO / Future Improvements
- [ ] Implement robust logging strategy.
- [ ] Add more domain logic and entities.
- [ ] Enhance test coverage for AI use cases.
