# JournalSystem_Search

Search microservice for the larger **JournalSystem** project.  
Responsible for performing **cross-entity and cross-service searches**, such as:

- Patients by doctor
- Patients by condition
- Patients by age or other attributes

This service is implemented using **Quarkus** and is optimized for fast, stateless query operations.  
It is designed to run as part of a **containerized microservices architecture** using Docker and Kubernetes.

---

## Features

- Search patients by assigned doctor
- Search patients by medical condition
- Search patients by age and other attributes
- Aggregates and queries data across databases
- REST API built with Quarkus
- MySQL persistence (reactive)
- Secured with Keycloak (OIDC)
- Containerized with Docker and deployable with Kubernetes
- Unit-tested service logic

---

## Tech Stack

- **Java 17**
- **Quarkus** (REST API)
- **MySQL (Reactive client)**
- **Keycloak** (OIDC authentication & authorization)
- **Docker**
- **Kubernetes (k3s)**
- **JUnit 5** (unit tests)

---

## Architecture (high level)

- `resource/` contains REST endpoints (Quarkus resources)
- Reactive, non-blocking database access using Quarkus

This service is stateless and designed to scale horizontally.

---

## Kubernetes

- This service is deployed as part of the project’s Kubernetes (k3s) setup
- Deployment manifests are managed in the **JournalSystem_Q8SFILES** repository
