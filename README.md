# MediTicket2 Frontend

## Project Overview

MediTicket2 is a healthcare management desktop application built using **Java Swing**, **FlatLaf**, **MigLayout**, and a shared REST API client.

The frontend provides role-based interfaces for:

* Patients
* Doctors
* Clinic Staff
* Administrators
* Appointments
* Patient Tickets
* Payments
* Notifications
* User Profiles
* Authentication

The desktop client communicates with the MediTicket2 Spring Boot backend through REST API endpoints.

---

## Technology Stack

* Java 21
* Java Swing
* FlatLaf
* MigLayout
* Maven
* Jackson
* REST API
* JWT Authentication
* JUnit 5
* Mockito
* IntelliJ IDEA
* GitHub

---

## Frontend Architecture

The frontend follows a component-based structure built around Swing panels and shared UI components.

The main areas of the application include:

* **API Layer** — Handles communication with the backend REST API
* **Model Layer** — Contains frontend representations of backend domain objects
* **Session Layer** — Maintains the currently authenticated user's session
* **UI Layer** — Contains dashboards, pages, dialogs, navigation and reusable components
* **Theme Layer** — Provides shared styling, colours, fonts and UI configuration

---

## Project Structure

```text
src
└── main
    └── java
        └── za.ac.cput
            ├── api
            │   ├── ApiClientProvider
            │   ├── BaseApiClient
            │   └── ...
            │
            ├── model
            │   └── domain
            │       ├── Appointment
            │       ├── ClinicStaff
            │       ├── Doctor
            │       ├── Notification
            │       ├── Patient
            │       ├── PatientTicket
            │       ├── Payment
            │       └── ...
            │
            ├── session
            │   └── SessionManager
            │
            └── ui
                ├── clinicstaff
                │   ├── admin
                │   ├── components
                │   └── nurse
                │
                ├── doctor
                │   ├── pages
                │   └── DoctorDashboard
                │
                ├── patient
                │   ├── pages
                │   └── PatientDashboard
                │
                ├── layout
                │   ├── Sidebar
                │   ├── TopHeader
                │   └── NavItem
                │
                └── theme
                    ├── AppTheme
                    └── FontManager
```

---

## Authentication

The frontend integrates with the backend's JWT authentication system.

The client supports:

* User login
* JWT access token storage
* Automatic JWT attachment to API requests
* Session management
* Logout
* Role-based dashboards
* Authenticated API communication

The `SessionManager` maintains information about the currently authenticated user, while `BaseApiClient` handles authenticated requests to the backend.

---

## API Integration

The frontend communicates with the backend through a shared API client architecture.

### Base API Client

`BaseApiClient` provides common functionality for REST communication, including:

* GET requests
* POST requests
* PUT requests
* PATCH requests
* DELETE requests
* JWT authentication headers
* JSON serialization/deserialization
* API error handling

### API Client Provider

`ApiClientProvider` provides access to the application's API clients.

The backend URL can be configured using:

```text
-Dapi.base.url=<backend-url>
```

If no URL is provided, the application uses the configured default backend URL.

---

## Role-Based Dashboards

The frontend provides separate dashboards for different user types.

### Patient Dashboard

The patient interface provides navigation for:

* Dashboard
* Appointments
* Tickets
* Payments
* Notifications
* Profile

### Doctor Dashboard

The doctor interface provides navigation for:

* Dashboard
* Appointments
* Tickets
* Patients
* Notifications
* Profile

### Clinic Staff Dashboard

Clinic staff functionality is separated according to staff responsibilities, including administrative and nursing interfaces.

Administrative functionality includes areas such as:

* Dashboard
* Appointments
* Tickets
* Patients
* Notifications
* Staff management
* Profile

---

## Notifications

The frontend includes notification interfaces for the supported user roles.

Notification functionality includes:

* Notification history
* Notification status display
* Notification type display
* Notification details
* Recipient information
* Mark as Read functionality
* Notification filtering
* Follow-up notification functionality

The notification interface communicates with the backend Notification API.

---

## Patient Tickets

The frontend provides ticket management interfaces for patients, doctors and clinic staff.

Ticket functionality includes:

* Viewing patient tickets
* Viewing ticket status
* Viewing ticket details
* Ticket-related appointment information
* Role-specific ticket management

---

## Appointments

Appointment pages provide users with access to appointment information appropriate to their role.

Appointments are connected to patients, doctors and clinic staff through the backend API.

---

## Payments

The patient interface provides a dedicated Payments section for viewing and managing payment-related information provided by the backend.

---

## User Interface

The application uses shared UI components to maintain a consistent interface across dashboards.

### Shared Components

* Sidebar navigation
* Top header
* Dialog components
* Notification dialogs
* Reusable page components
* Shared buttons and form controls

### Theme

The application uses:

* `AppTheme` for shared colours, spacing and UI constants
* `FontManager` for consistent typography
* FlatLaf for the application's Swing look and feel
* MigLayout where appropriate for component layout

---

## Configuration

The frontend can be configured to connect to different backend environments.

The API base URL can be supplied as a Java system property:

```bash
-Dapi.base.url=https://mediticket2-production.up.railway.app
```

This allows the same desktop client to communicate with either a local development backend or the deployed backend.

---

## Testing

The project uses:

* JUnit 5
* Mockito

Tests are used to verify frontend/API-related functionality where applicable.

---

## Development Workflow

### 1. Clone the Repository

```bash
git clone <repository-url>
```

### 2. Open the Project

Open the project in **IntelliJ IDEA**.

### 3. Load Maven Dependencies

Allow IntelliJ IDEA to load the Maven project and download the required dependencies.

### 4. Configure the Backend URL

Use the appropriate API base URL through:

```text
-Dapi.base.url=<backend-url>
```

### 5. Run the Application

Run the main application class from IntelliJ IDEA.

### 6. Create a Branch

Each team member works on their own branch.

```bash
git checkout -b <branch-name>
```

### 7. Commit Changes

```bash
git add .
git commit -m "Completed frontend implementation"
```

### 8. Push Changes

```bash
git push origin <branch-name>
```

### 9. Create a Pull Request

Submit a Pull Request from the development branch to the appropriate main project branch.

### 10. Await Review

The Team Lead reviews and merges approved Pull Requests.

---

## Team Members

| Team Member            | Student Number |
| ---------------------- | -------------: |
| Joshua Reid Adams      |      230317693 |
| Abdullahi Raage Farah  |      230971091 |
| Aidan Barends          |      230155639 |
| Jaden Clayton Abrahams |      222206721 |
| Joshua Peter Bonzet    |      221312536 |
| Matthew Barron         |      230398863 |
| Raul Jaaim Everts      |      230270564 |

---

## Backend

The frontend connects to the MediTicket2 Spring Boot backend through its REST API.

The backend provides:

* Authentication
* User management
* Appointments
* Patient tickets
* Payments
* Notifications
* Database persistence
* Email services
* JWT security

---

## Deployment

The frontend is a Java Swing desktop application and is run locally through IntelliJ IDEA.

The application can connect to the deployed MediTicket2 backend hosted on **Railway** using the configured API base URL.

---

## Project Status

The frontend is under active development alongside the MediTicket2 backend.

Features are implemented progressively according to the team's assigned tasks and project requirements.
